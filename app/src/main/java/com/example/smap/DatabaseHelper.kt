package com.example.smap

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_VERSION = 1
        private const val DATABASE_NAME = "MemoryDatabase.db"

        // Images table
        private const val TABLE_IMAGES = "images"
        private const val COLUMN_IMAGE_ID = "_id"
        private const val COLUMN_IMAGE_PATH = "path"
        private const val COLUMN_IMAGE_TIMESTAMP = "timestamp"
        private const val COLUMN_IMAGE_LOCATION_ID = "location_id"

        // Location table
        private const val TABLE_LOCATION = "location"
        private const val COLUMN_LOCATION_ID = "_id"
        private const val COLUMN_LOCATION_LATITUDE = "latitude"
        private const val COLUMN_LOCATION_LONGITUDE = "longitude"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createLocationTable = "CREATE TABLE IF NOT EXISTS $TABLE_LOCATION (" +
                "$COLUMN_LOCATION_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "$COLUMN_LOCATION_LATITUDE REAL, " +
                "$COLUMN_LOCATION_LONGITUDE REAL" +
                ")"

        val createImagesTable = "CREATE TABLE IF NOT EXISTS $TABLE_IMAGES (" +
                "$COLUMN_IMAGE_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "$COLUMN_IMAGE_PATH TEXT, " +
                "$COLUMN_IMAGE_TIMESTAMP INTEGER, " +
                "$COLUMN_IMAGE_LOCATION_ID INTEGER, " +
                "FOREIGN KEY ($COLUMN_IMAGE_LOCATION_ID) REFERENCES $TABLE_LOCATION($COLUMN_LOCATION_ID)" +
                ")"

        db.execSQL(createLocationTable)
        db.execSQL(createImagesTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_IMAGES")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_LOCATION")
        onCreate(db)
    }

    fun addMemory(imagePath: String, latitude: Double, longitude: Double, timestamp: Long): Long {
        val db = this.writableDatabase

        val locationId = getLocationId(db, latitude, longitude)

        if (locationId == null) {
            val locationValues = ContentValues()
            locationValues.put(COLUMN_LOCATION_LATITUDE, latitude)
            locationValues.put(COLUMN_LOCATION_LONGITUDE, longitude)
            val newLocationId = db.insert(TABLE_LOCATION, null, locationValues)

            insertImage(db, imagePath, timestamp, newLocationId)
            db.close()
            return newLocationId
        } else {
            insertImage(db, imagePath, timestamp, locationId)
            db.close()
            return locationId
        }
    }

    @SuppressLint("Range")
    private fun getLocationId(db: SQLiteDatabase, latitude: Double, longitude: Double): Long? {
        val locationQuery = "SELECT $COLUMN_LOCATION_ID FROM $TABLE_LOCATION WHERE " +
                "$COLUMN_LOCATION_LATITUDE = $latitude AND $COLUMN_LOCATION_LONGITUDE = $longitude"
        val cursor = db.rawQuery(locationQuery, null)
        val locationId: Long? = if (cursor != null && cursor.moveToFirst()) {
            val id = cursor.getLong(cursor.getColumnIndex(COLUMN_LOCATION_ID))
            cursor.close()
            id
        } else {
            null
        }
        return locationId
    }

    private fun insertImage(db: SQLiteDatabase, imagePath: String, timestamp: Long, locationId: Long) {
        val imageValues = ContentValues()
        imageValues.put(COLUMN_IMAGE_PATH, imagePath)
        imageValues.put(COLUMN_IMAGE_TIMESTAMP, timestamp)
        imageValues.put(COLUMN_IMAGE_LOCATION_ID, locationId)
        db.insert(TABLE_IMAGES, null, imageValues)
    }


    fun readAllData(): Cursor? {
        val db = this.readableDatabase
        return db.rawQuery("SELECT * FROM $TABLE_LOCATION", null)
    }

    fun readAllLocations(): Cursor? {
        val db = this.readableDatabase
        return db.rawQuery("SELECT * FROM $TABLE_LOCATION", null)
    }

    @SuppressLint("Range")
    fun findImagesByLocation(latitude: DoubleArray, longitude: DoubleArray): Cursor? {
        val db = this.readableDatabase

        val locationIds = mutableListOf<Long>()
        for (i in latitude.indices) {
            val latitudeValue = latitude[i]
            val longitudeValue = longitude[i]

            val locationIdQuery = "SELECT $COLUMN_LOCATION_ID FROM $TABLE_LOCATION WHERE " +
                    "$COLUMN_LOCATION_LATITUDE = $latitudeValue AND $COLUMN_LOCATION_LONGITUDE = $longitudeValue"
            val locationIdCursor = db.rawQuery(locationIdQuery, null)

            if (locationIdCursor != null && locationIdCursor.moveToFirst()) {
                val locationId = locationIdCursor.getLong(locationIdCursor.getColumnIndex(COLUMN_LOCATION_ID))
                locationIds.add(locationId)
            }

            locationIdCursor?.close()
        }

        if (locationIds.isNotEmpty()) {
            val locationIdsString = locationIds.joinToString()
            val imagesQuery = "SELECT * FROM $TABLE_IMAGES WHERE $COLUMN_IMAGE_LOCATION_ID IN ($locationIdsString)"
            return db.rawQuery(imagesQuery, null)
        }

        return null
    }
    fun findImagesByDate(from: Long, to: Long): Cursor? {
        val db = this.readableDatabase

        val countQuery = "SELECT COUNT(*) FROM $TABLE_IMAGES WHERE $COLUMN_IMAGE_TIMESTAMP >= $from AND $COLUMN_IMAGE_TIMESTAMP <= $to"
        val countCursor = db.rawQuery(countQuery, null)
        countCursor.moveToFirst()
        val count = countCursor.getInt(0)
        countCursor.close()

        val imagesQuery = if (count > 0) {
            "SELECT * FROM $TABLE_IMAGES WHERE $COLUMN_IMAGE_TIMESTAMP >= $from AND $COLUMN_IMAGE_TIMESTAMP <= $to"
        } else {
            "SELECT * FROM $TABLE_IMAGES"
        }
        return db.rawQuery(imagesQuery, null)
    }

    fun getFilteredLocations(from: Long, to: Long): Cursor? {
        val db = this.readableDatabase

        val imagesQuery = "SELECT $COLUMN_IMAGE_LOCATION_ID FROM $TABLE_IMAGES WHERE $COLUMN_IMAGE_TIMESTAMP BETWEEN $from AND $to"
        val imageCursor = db.rawQuery(imagesQuery, null)

        val locationIds = mutableListOf<Long>()
        if (imageCursor != null && imageCursor.moveToFirst()) {
            do {
                val locationId = imageCursor.getLong(0)
                locationIds.add(locationId)
            } while (imageCursor.moveToNext())

            imageCursor.close()
        }

        if (locationIds.isNotEmpty()) {
            val locationIdsStr = locationIds.joinToString(",")
            val locationsQuery = "SELECT * FROM $TABLE_LOCATION WHERE $COLUMN_LOCATION_ID IN ($locationIdsStr)"
            return db.rawQuery(locationsQuery, null)
        }

        return db.rawQuery("SELECT * FROM $TABLE_LOCATION", null)
    }

}
