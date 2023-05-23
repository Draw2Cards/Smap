package com.example.smap

import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.Manifest;
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.example.smap.databinding.ActivityMapsBinding
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.maps.android.clustering.Cluster
import com.google.maps.android.clustering.ClusterItem
import com.google.maps.android.clustering.ClusterManager

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding
    private lateinit var databaseHelper: DatabaseHelper
    private lateinit var markersList: ArrayList<ClusterMarkerItem>
    private lateinit var mapFragment: SupportMapFragment

    @SuppressLint("PotentialBehaviorOverride")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        databaseHelper = DatabaseHelper(this)


        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        val fabLocation = findViewById<FloatingActionButton>(R.id.fab_location)
        fabLocation.setOnClickListener {
            getCurrentLocation(mMap)
        }

        val fabCamera = findViewById<FloatingActionButton>(R.id.fab_camera)
        fabCamera.setOnClickListener {
            openCamera()
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        setUpClusterManager(mMap)
        getCurrentLocation(googleMap)
    }

    private fun setUpClusterManager(mMap: GoogleMap) {
        val clusterManager = ClusterManager<ClusterMarkerItem>(this, mMap)

        // Set up the ClusterManager as the renderer for the GoogleMap
        mMap.setOnCameraIdleListener(clusterManager)
        mMap.setOnMarkerClickListener(clusterManager)

        clusterManager.setOnClusterItemClickListener { clusterItem ->
            val markerLocation = clusterItem.position
            val intent = Intent(this, Gallery::class.java)
            intent.putExtra("latitude", markerLocation.latitude)
            intent.putExtra("longitude", markerLocation.longitude)
            startActivity(intent)
            true
        }

        // Retrieve and add the markers to the ClusterManager
        markersList = getAllItem()
        clusterManager.addItems(markersList)
        clusterManager.cluster()
    }


    private fun getAllItem(): ArrayList<ClusterMarkerItem> {
        var arrayList:ArrayList<ClusterMarkerItem> = ArrayList()
        val cursor = databaseHelper.readAllData()
        if (cursor != null)
        {
            while (cursor.moveToNext()) {
                val _id = cursor.getInt(0);
                val loc1 = cursor.getDouble(1);
                val loc2 = cursor.getDouble(2);
                Log.d("DB", _id.toString() + loc1 + loc2)
                arrayList.add(ClusterMarkerItem(LatLng(loc1, loc2)))
            }
        }
        return  arrayList
    }

    private fun openCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), REQUEST_CAMERA_PERMISSION)
        } else {
            val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            if (takePictureIntent.resolveActivity(packageManager) != null) {
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
            }
            else {
                Toast.makeText(this, "No app can handle this action", Toast.LENGTH_SHORT).show()
                return
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK) {
            val imageBitmap = data?.extras?.get("data") as Bitmap
            val savedImageURL = saveImageToGallery(imageBitmap)
            mMap?.clear()
            addMemoryToDb(savedImageURL)
            setUpClusterManager(mMap)
        }
    }

    private fun addMemoryToDb(imageURL: String?) {
        val databaseHelper = DatabaseHelper(this)
        val location = mMap?.myLocation
        if (imageURL != null && location != null) {
            val latitude = location.latitude
            val longitude =location.longitude
            val timestamp = System.currentTimeMillis()
            databaseHelper.addMemory(imageURL, latitude.toDouble(), longitude.toDouble(), timestamp)
        }
    }

    private fun addMarkerToCurrentLocation() {
        val location = mMap?.myLocation ?: return // Get the current location from the map
        val latLng = LatLng(location.latitude, location.longitude) // Convert to a LatLng object
        val markerOptions = MarkerOptions().position(latLng) // Create marker options with the LatLng object
        mMap?.addMarker(markerOptions) // Add the marker to the map
    }

    private fun saveImageToGallery(bitmap: Bitmap): String? {
        return MediaStore.Images.Media.insertImage(
            contentResolver,
            bitmap,
            "title",
            "description"
        )
    }

    private fun getCurrentLocation(googleMap: GoogleMap) {
        // Get the user's current location and add a marker to the map
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            googleMap.isMyLocationEnabled = true
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    val currentLatLng = LatLng(location.latitude, location.longitude)
                    googleMap.moveCamera(CameraUpdateFactory.newLatLng(currentLatLng))
                    googleMap.animateCamera(CameraUpdateFactory.zoomTo(15f))
                }
            }
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1
            )
        }
    }

    companion object {
        private const val REQUEST_IMAGE_CAPTURE = 1
        private val REQUEST_CAMERA_PERMISSION = 1
        }


}

