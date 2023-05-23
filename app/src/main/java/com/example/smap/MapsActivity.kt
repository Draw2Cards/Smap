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

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding
    private lateinit var databaseHelper: DatabaseHelper

    @SuppressLint("PotentialBehaviorOverride")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        databaseHelper = DatabaseHelper(this)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
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

        val map = (supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment).getMapAsync { googleMap ->
            googleMap.setOnMarkerClickListener { marker ->
                val markerLocation = marker.position

                val intent = Intent(this, Gallery::class.java)
                intent.putExtra("latitude", markerLocation.latitude)
                intent.putExtra("longitude", markerLocation.longitude)
                startActivity(intent)
                true
            }
        }

    }

    private fun readData() {
        val cursor = databaseHelper.readAllData()
        if (cursor != null)
        {
            while (cursor.moveToNext()) {
                val _id = cursor.getInt(0);
                val path = cursor.getString(1);
                val loc1 = cursor.getFloat(2);
                val loc2 = cursor.getFloat(3);
                val timestamp = cursor.getLong(4);

                Log.d("DB", _id.toString() + path + loc1 + loc2 +timestamp)
            }
        }
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
            addMarkers()

        }
    }

    private fun addMemoryToDb(imageURL: String?) {
        val databaseHelper = DatabaseHelper(this)
        val location = mMap?.myLocation
        if (imageURL != null && location != null) {
            val latitude = location.latitude
            val longitude = location.longitude
            val timestamp = System.currentTimeMillis()
            databaseHelper.addMemory(imageURL, latitude, longitude, timestamp)
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

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        getCurrentLocation(googleMap)
        addMarkers()

    }

    private fun addMarkers() {
        val cursor = databaseHelper.readAllData()
        if (cursor != null)
        {
            while (cursor.moveToNext()) {
                val _id = cursor.getInt(0);
                val path = cursor.getString(1);
                val loc1 = cursor.getDouble(2);
                val loc2 = cursor.getDouble(3);
                val timestamp = cursor.getLong(4);

                Log.d("DB", _id.toString() + path + loc1 + loc2 +timestamp)

                val latLng = LatLng(loc1, loc2) // Convert to a LatLng object
                val markerOptions = MarkerOptions().position(latLng) // Create marker options with the LatLng object
                mMap?.addMarker(markerOptions) // Add the marker to the map

            }
        }
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

