package com.example.smap

import android.content.Intent
import android.icu.text.SimpleDateFormat
import android.icu.util.Calendar
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.datepicker.MaterialDatePicker
import java.util.*

class FilterActivity : AppCompatActivity() {

    private lateinit var selectedDateToTextView: TextView
    private lateinit var selectedDateFromTextView: TextView
    private var to: Long = 0
    private var from: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_filter)


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
            val intent = Intent(this, MapsActivity::class.java)
            intent.putExtra("to", to)
            intent.putExtra("from", from)
            startActivity(intent)
        }
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
}