package com.example.smap

import ImageAdapter
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.CheckBox
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

class Gallery : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private var isLongPressDetected = false
    private lateinit var imageAdapter: ImageAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gallery)

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = GridLayoutManager(this, 3)

        val latitude = intent.getDoubleExtra("latitude", 0.0)
        val longitude = intent.getDoubleExtra("longitude", 0.0)

        val images = mutableListOf<String>()
        var databaseHelper = DatabaseHelper(this)
        val cursor = databaseHelper.findImagesByLocation(latitude, longitude)
        if (cursor != null)
        {
            while (cursor.moveToNext()) {
                val _id = cursor.getInt(0);
                val path = cursor.getString(1);
                val timestamp = cursor.getLong(2);
                val loc_id = cursor.getInt(3);

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

}