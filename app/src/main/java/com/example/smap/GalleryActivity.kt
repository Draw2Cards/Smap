package com.example.smap

import ImageAdapter
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.CheckBox
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

class GalleryActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private var isLongPressDetected = false
    private lateinit var imageAdapter: ImageAdapter
    private val images = mutableListOf<String>()
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.MyTheme)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gallery)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = GridLayoutManager(this, 3)


        val databaseHelper = DatabaseHelper(this)

        val latitude = intent.getDoubleArrayExtra("latitudeArray") ?: doubleArrayOf()
        val longitude = intent.getDoubleArrayExtra("longitudeArray") ?: doubleArrayOf()

        val cursor: Cursor? = if (latitude.isNotEmpty()) {
            databaseHelper.findImagesByLocation(latitude, longitude)
        } else {
            val from = intent.getLongExtra("from", 0L)
            val to = intent.getLongExtra("to", 0L)
            databaseHelper.findImagesByDate(from, to)
        }

        cursor?.use {
            while (cursor.moveToNext()) {
                val _id = cursor.getInt(0)
                val path = cursor.getString(1)
                val timestamp = cursor.getLong(2)
                val loc_id = cursor.getInt(3)

                images.add(path)
                Log.d("DB", "($_id;$path;$timestamp;$loc_id)")
            }
        }

        imageAdapter = ImageAdapter(recyclerView.context, images, recyclerView, false) { isChecked ->
            // This lambda function is called when a checkbox is checked/unchecked
            isLongPressDetected = isChecked
            updateCheckboxVisibility()
        }

        imageAdapter.setOnCheckboxCheckedListener { isChecked ->
            // Update your UI based on the checkbox state
            // For example, enable/disable a "Delete" button
        }

        recyclerView.adapter = imageAdapter

        Log.d("Gallery", "($latitude, $longitude)")
    }

    private fun updateCheckboxVisibility() {
        val visibility = if (isLongPressDetected) View.VISIBLE else View.GONE
        for (i in 0 until recyclerView.childCount) {
            val view = recyclerView.getChildAt(i)
            val checkbox = view.findViewById<CheckBox>(R.id.checkBox)
            checkbox.visibility = visibility
        }
    }

    private fun shareImages() {
        val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_SUBJECT, "Shared Images")
            putExtra(Intent.EXTRA_TEXT, "Check out these images!")
            // Add the checked image paths as Uri to the Intent
            val imageUris: ArrayList<Uri> = ArrayList()
            for (i in 0 until recyclerView.childCount) {
                val view = recyclerView.getChildAt(i)
                val checkbox = view.findViewById<CheckBox>(R.id.checkBox)
                if (checkbox.isChecked) {
                    val image = images[i]
                    val uri = Uri.parse(image)
                    imageUris.add(uri)
                }
            }
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, imageUris)
        }

        // Verify if there are any apps available to handle the intent
        if (intent.resolveActivity(packageManager) != null) {
            // Start the chooser dialog
            startActivity(Intent.createChooser(intent, "Share images"))
        } else {
            // Handle case where no apps can handle the intent
            Toast.makeText(this, "No app available to share images", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_gallery, menu)
        return true
    }

    private fun checkAll() {
        for (i in 0 until recyclerView.childCount) {
            val view = recyclerView.getChildAt(i)
            val checkbox = view.findViewById<CheckBox>(R.id.checkBox)
            checkbox.isChecked = true
        }
    }

    private fun uncheckAll() {
        for (i in 0 until recyclerView.childCount) {
            val view = recyclerView.getChildAt(i)
            val checkbox = view.findViewById<CheckBox>(R.id.checkBox)
            checkbox.isChecked = false
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_share -> {
                shareImages()
                true
            }
            R.id.action_check_all -> {
                checkAll()
                true
            }
            R.id.action_uncheck_all -> {
                uncheckAll()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}