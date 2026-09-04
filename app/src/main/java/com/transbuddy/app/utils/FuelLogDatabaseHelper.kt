package com.transbuddy.app.utils

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import com.transbuddy.app.models.FuelLog

/**
 * FuelLogDatabaseHelper — SQLite Database Helper
 * Handles persistent storage, retrieval, and auto-seeding for Fuel & KM logs
 * in the TransBuddy App (transbuddy_fuel.db).
 */
class FuelLogDatabaseHelper private constructor(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val TAG = "FuelLogDatabaseHelper"
        private const val DATABASE_NAME = "transbuddy_fuel.db"
        private const val DATABASE_VERSION = 2

        const val TABLE_FUEL_LOGS = "fuel_logs"
        const val COLUMN_ID = "id"
        const val COLUMN_STATION = "station_name"
        const val COLUMN_TIMESTAMP = "timestamp"
        const val COLUMN_LITERS = "liters"
        const val COLUMN_TOTAL_COST = "total_cost"
        const val COLUMN_TRIP_KM = "trip_km"
        const val COLUMN_RECEIPT_URI = "receipt_uri"

        @Volatile
        private var instance: FuelLogDatabaseHelper? = null

        fun getInstance(context: Context): FuelLogDatabaseHelper {
            return instance ?: synchronized(this) {
                instance ?: FuelLogDatabaseHelper(context.applicationContext).also {
                    instance = it
                    it.seedDefaultLogsIfMissing()
                }
            }
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTableQuery = """
            CREATE TABLE IF NOT EXISTS $TABLE_FUEL_LOGS (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_STATION TEXT NOT NULL,
                $COLUMN_TIMESTAMP TEXT NOT NULL,
                $COLUMN_LITERS REAL NOT NULL,
                $COLUMN_TOTAL_COST REAL NOT NULL,
                $COLUMN_TRIP_KM INTEGER NOT NULL,
                $COLUMN_RECEIPT_URI TEXT
            )
        """.trimIndent()
        db.execSQL(createTableQuery)
        seedDefaultLogs(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Safe upgrade without dropping data if table already exists
        val createTableQuery = """
            CREATE TABLE IF NOT EXISTS $TABLE_FUEL_LOGS (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_STATION TEXT NOT NULL,
                $COLUMN_TIMESTAMP TEXT NOT NULL,
                $COLUMN_LITERS REAL NOT NULL,
                $COLUMN_TOTAL_COST REAL NOT NULL,
                $COLUMN_TRIP_KM INTEGER NOT NULL,
                $COLUMN_RECEIPT_URI TEXT
            )
        """.trimIndent()
        db.execSQL(createTableQuery)
    }

    private fun seedDefaultLogs(db: SQLiteDatabase) {
        try {
            val defaults = listOf(
                FuelLog(
                    stationName = "Marwadi University Campus Pump (Diesel)",
                    timestamp = "Today, 08:30 AM",
                    liters = 45.0,
                    totalCost = 4230.00,
                    tripKm = 142580,
                    isRecent = true
                ),
                FuelLog(
                    stationName = "Indian Oil Super Fuel Hub - Rajkot Highway (Diesel)",
                    timestamp = "Yesterday, 05:15 PM",
                    liters = 52.5,
                    totalCost = 4935.00,
                    tripKm = 142260,
                    isRecent = false
                ),
                FuelLog(
                    stationName = "Nayara Energy CNG Station - Gondal Bypass (CNG)",
                    timestamp = "Sep 02, 09:00 AM",
                    liters = 36.0,
                    totalCost = 2880.00,
                    tripKm = 141940,
                    isRecent = false
                )
            )

            for (log in defaults) {
                val values = ContentValues().apply {
                    put(COLUMN_STATION, log.stationName)
                    put(COLUMN_TIMESTAMP, log.timestamp)
                    put(COLUMN_LITERS, log.liters)
                    put(COLUMN_TOTAL_COST, log.totalCost)
                    put(COLUMN_TRIP_KM, log.tripKm)
                    put(COLUMN_RECEIPT_URI, log.receiptUri)
                }
                db.insert(TABLE_FUEL_LOGS, null, values)
            }
            Log.d(TAG, "Default Marwadi University fuel logs seeded successfully.")
        } catch (e: Exception) {
            Log.e(TAG, "Error seeding default fuel logs: ${e.message}", e)
        }
    }

    fun seedDefaultLogsIfMissing() {
        try {
            val db = writableDatabase
            val cursor = db.rawQuery("SELECT COUNT(*) FROM $TABLE_FUEL_LOGS", null)
            val count = cursor.use {
                if (it.moveToFirst()) it.getInt(0) else 0
            }
            if (count == 0) {
                seedDefaultLogs(db)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking/seeding fuel logs: ${e.message}", e)
        }
    }

    fun insertLog(log: FuelLog): Long {
        return try {
            val values = ContentValues().apply {
                put(COLUMN_STATION, log.stationName)
                put(COLUMN_TIMESTAMP, log.timestamp)
                put(COLUMN_LITERS, log.liters)
                put(COLUMN_TOTAL_COST, log.totalCost)
                put(COLUMN_TRIP_KM, log.tripKm)
                put(COLUMN_RECEIPT_URI, log.receiptUri)
            }
            writableDatabase.insert(TABLE_FUEL_LOGS, null, values)
        } catch (e: Exception) {
            Log.e(TAG, "Error inserting fuel log: ${e.message}", e)
            -1L
        }
    }

    fun deleteLog(id: Long): Boolean {
        return try {
            val rows = writableDatabase.delete(TABLE_FUEL_LOGS, "$COLUMN_ID = ?", arrayOf(id.toString()))
            rows > 0
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting fuel log $id: ${e.message}", e)
            false
        }
    }

    fun getAllLogs(): List<FuelLog> {
        val list = mutableListOf<FuelLog>()
        try {
            val cursor = readableDatabase.rawQuery(
                "SELECT * FROM $TABLE_FUEL_LOGS ORDER BY $COLUMN_ID DESC",
                null
            )
            cursor.use {
                val idxId = it.getColumnIndex(COLUMN_ID)
                val idxStation = it.getColumnIndex(COLUMN_STATION)
                val idxTimestamp = it.getColumnIndex(COLUMN_TIMESTAMP)
                val idxLiters = it.getColumnIndex(COLUMN_LITERS)
                val idxCost = it.getColumnIndex(COLUMN_TOTAL_COST)
                val idxKm = it.getColumnIndex(COLUMN_TRIP_KM)
                val idxReceipt = it.getColumnIndex(COLUMN_RECEIPT_URI)

                var isFirst = true
                while (it.moveToNext()) {
                    list.add(
                        FuelLog(
                            id = if (idxId != -1) it.getLong(idxId) else 0L,
                            stationName = if (idxStation != -1) it.getString(idxStation) else "",
                            timestamp = if (idxTimestamp != -1) it.getString(idxTimestamp) else "",
                            liters = if (idxLiters != -1) it.getDouble(idxLiters) else 0.0,
                            totalCost = if (idxCost != -1) it.getDouble(idxCost) else 0.0,
                            tripKm = if (idxKm != -1) it.getInt(idxKm) else 0,
                            isRecent = isFirst,
                            receiptUri = if (idxReceipt != -1) it.getString(idxReceipt) else null
                        )
                    )
                    isFirst = false
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading all fuel logs: ${e.message}", e)
        }
        return list
    }
}
