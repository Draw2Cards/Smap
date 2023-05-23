package com.example.smap

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "gallery.db"
        private const val DATABASE_VERSION = 1

        private const val TABLE_IMAGES = "images"
        private const val COLUMN_ID = "_id"
        private const val COLUMN_PATH = "path"
        private const val COLUMN_LOCATION_1 = "location1"
        private const val COLUMN_LOCATION_2 = "location2"
        private const val COLUMN_TIMESTAMP = "timestamp"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createImagesTable = "CREATE TABLE $TABLE_IMAGES (" +
                "$COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT," +
                "$COLUMN_PATH TEXT," +
                "$COLUMN_LOCATION_1 REAL," +
                "$COLUMN_LOCATION_2 REAL," +
                "$COLUMN_TIMESTAMP INTEGER" +
                ")"
        db.execSQL(createImagesTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_IMAGES")
        onCreate(db)
    }

    fun insertImage(path: String, location1: Float, location2: Float, timestamp: Long): Long {
        val values = ContentValues()
        values.put(COLUMN_PATH, path)
        values.put(COLUMN_LOCATION_1, location1)
        values.put(COLUMN_LOCATION_2, location2)
        values.put(COLUMN_TIMESTAMP, timestamp)

        val db = writableDatabase
        val id = db.insert(TABLE_IMAGES, null, values)
        db.close()
        return id
    }

    fun findImagesByLocation(location1: Double, location2: Double): Cursor? {
        val db = readableDatabase
        val query = "SELECT * FROM $TABLE_IMAGES WHERE $COLUMN_LOCATION_1 = ? AND $COLUMN_LOCATION_2 = ?"
        val args = arrayOf(location1.toString(), location2.toString())
        Log.d("Query", "SQL Query: $query, Args: ${args.contentToString()}")
        return db.rawQuery(query, args)
    }

    fun addMemory(path: String, location1: Double, location2: Double, timestamp: Long): Long {
        val roundedLocation1 = String.format("%.5f", location1).toDouble()
        val roundedLocation2 = String.format("%.5f", location2).toDouble()

        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_PATH, path)
            put(COLUMN_LOCATION_1, roundedLocation1)
            put(COLUMN_LOCATION_2, roundedLocation2)
            put(COLUMN_TIMESTAMP, timestamp)
        }
        val id = db.insert(TABLE_IMAGES, null, values)
        db.close()
        return id
    }

    fun readAllData(): Cursor? {
        val db = readableDatabase
        val query = "SELECT * FROM $TABLE_IMAGES"
        return db.rawQuery(query, null)
    }
}
