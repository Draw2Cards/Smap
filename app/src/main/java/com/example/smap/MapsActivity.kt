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
import android.database.Cursor
import android.graphics.Bitmap
import android.icu.text.SimpleDateFormat
import android.icu.util.Calendar
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.example.smap.databinding.ActivityMapsBinding
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.maps.android.clustering.ClusterManager
import java.util.*
import kotlin.collections.ArrayList

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding
    private lateinit var databaseHelper: DatabaseHelper
    private lateinit var markersList: ArrayList<ClusterMarkerItem>
    private lateinit var mapFragment: SupportMapFragment
    private lateinit var selectedDateToTextView: TextView
    private lateinit var selectedDateFromTextView: TextView
    private var to: Long = 0
    private var from: Long = 0
    private lateinit var drawerLayout: DrawerLayout

    @SuppressLint("PotentialBehaviorOverride")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        databaseHelper = DatabaseHelper(this)
        supportActionBar?.hide()
        drawerLayout = findViewById<DrawerLayout>(R.id.drawerLayout)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        val fabCamera = findViewById<FloatingActionButton>(R.id.fab_camera)
        fabCamera.setOnClickListener {
            openCamera()
        }

        val fabFilter = findViewById<FloatingActionButton>(R.id.fab_filter)
        fabFilter.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        selectedDateToTextView = findViewById(R.id.textTo)
        selectedDateToTextView.setOnClickListener {
            showDatePicker(selectedDateToTextView, R.string.to)
        }

        selectedDateFromTextView = findViewById(R.id.textFrom)
        selectedDateFromTextView.setOnClickListener {
            showDatePicker(selectedDateFromTextView, R.string.from)
        }

        val galleryButton: Button = findViewById(R.id.galleryButton)
        galleryButton.setOnClickListener {
            Log.d("FilterActivity", "Selected 'to' value: $to")
            Log.d("FilterActivity", "Selected 'from' value: $from")
            val intent = Intent(this, GalleryActivity::class.java)
            intent.putExtra("to", to)
            intent.putExtra("from", from)
            startActivity(intent)
        }


        val mapButton: Button = findViewById(R.id.mapButton)
        mapButton.setOnClickListener {
            // Print the selected "to" and "from" values to the log
            Log.d("FilterActivity", "Selected 'to' value: $to")
            Log.d("FilterActivity", "Selected 'from' value: $from")
            filterMap();
        }
    }

    private fun filterMap() {
        mMap?.clear()
        setUpClusterManager(mMap)
        drawerLayout.closeDrawer(GravityCompat.START)
    }

    private fun showDatePicker(textView: TextView, text: Int) {
        val calendar = Calendar.getInstance()
        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Select Date")
            .setSelection(calendar.timeInMillis)
            .build()

        datePicker.addOnPositiveButtonClickListener { selectedDate ->
            val selectedTimestamp = selectedDate as Long
            when (textView) {
                selectedDateToTextView -> {
                    val calendar = Calendar.getInstance()
                    calendar.timeInMillis = selectedTimestamp
                    // Set the time to 23:59
                    calendar.set(Calendar.HOUR_OF_DAY, 23)
                    calendar.set(Calendar.MINUTE, 59)
                    calendar.set(Calendar.SECOND, 0)
                    calendar.set(Calendar.MILLISECOND, 0)
                    to = calendar.timeInMillis
                    val selectedDateText = formatDate(to, true)
                    textView.text = getString(text) + " " + selectedDateText
                }
                selectedDateFromTextView -> {
                    from = selectedTimestamp
                    val selectedDateText = formatDate(selectedTimestamp)
                    textView.text = getString(text) + " " + selectedDateText
                }
            }
        }

        datePicker.show(supportFragmentManager, "DatePicker")
    }

    private fun formatDate(timestamp: Long, isEndOfDay: Boolean = false): String {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        return dateFormat.format(calendar.time)
    }

    private fun openFilterActivity() {
        val intent = Intent(this, FilterActivity::class.java)
        startActivityForResult(intent, FILTER_REQUEST_CODE)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        drawerLayout = findViewById<DrawerLayout>(R.id.drawerLayout)
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
            val latitudeArray = DoubleArray(1)
            val longitudeArray = DoubleArray(1)

            val markerLocation = clusterItem.position
            latitudeArray[0] = markerLocation.latitude
            longitudeArray[0] = markerLocation.longitude

            val intent = Intent(this, GalleryActivity::class.java)
            intent.putExtra("latitudeArray", latitudeArray)
            intent.putExtra("longitudeArray", longitudeArray)

            startActivity(intent)
            true
        }

        clusterManager.setOnClusterClickListener { cluster ->
            val latitudeArray = DoubleArray(cluster.size)
            val longitudeArray = DoubleArray(cluster.size)

            for ((index, clusterItem) in cluster.items.withIndex()) {
                val markerLocation = clusterItem.position
                latitudeArray[index] = markerLocation.latitude
                longitudeArray[index] = markerLocation.longitude
            }

            val intent = Intent(this, GalleryActivity::class.java)
            intent.putExtra("latitudeArray", latitudeArray)
            intent.putExtra("longitudeArray", longitudeArray)

            startActivity(intent)
            true
        }

        markersList = getAllItem()
        clusterManager.addItems(markersList)
        clusterManager.cluster()
    }

    private fun getAllItem(): ArrayList<ClusterMarkerItem> {
        val arrayList: ArrayList<ClusterMarkerItem> = ArrayList()

        val cursor: Cursor? = if (from == 0L) {
            databaseHelper.readAllData()
        } else {
            databaseHelper.getFilteredLocations(from, to)
        }

        if (cursor != null) {
            while (cursor.moveToNext()) {
                val loc1 = cursor.getDouble(cursor.getColumnIndex("latitude"))
                val loc2 = cursor.getDouble(cursor.getColumnIndex("longitude"))
                Log.d("DB", "$loc1 $loc2")
                arrayList.add(ClusterMarkerItem(LatLng(loc1, loc2)))
            }
            cursor.close()
        }

        return arrayList
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
        private const val REQUEST_IMAGE_CAPTURE = 1001
        private const val FILTER_REQUEST_CODE = 2001
        private const val REQUEST_CAMERA_PERMISSION = 3001
        }


}

