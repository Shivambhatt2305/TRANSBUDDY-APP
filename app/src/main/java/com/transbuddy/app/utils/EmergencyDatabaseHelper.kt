package com.transbuddy.app.utils

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import com.transbuddy.app.models.Emergency

class EmergencyDatabaseHelper private constructor(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val TAG = "EmergencyDatabaseHelper"
        private const val DATABASE_NAME = "transbuddy_emergencies.db"
        private const val DATABASE_VERSION = 1

        const val TABLE_EMERGENCIES = "emergencies"
        const val COLUMN_ID = "id"
        const val COLUMN_BUS_ID = "bus_id"
        const val COLUMN_DRIVER_ID = "driver_id"
        const val COLUMN_LOCATION = "location"
        const val COLUMN_STATUS = "status"
        const val COLUMN_SEVERITY = "severity"
        const val COLUMN_DESCRIPTION = "description"
        const val COLUMN_CREATED_AT = "created_at"
        const val COLUMN_UPDATED_AT = "updated_at"
        const val COLUMN_DRIVER_NAME = "driver_name"
        const val COLUMN_USERNAME = "username"
        const val COLUMN_BUS_NO = "bus_no"

        @Volatile
        private var instance: EmergencyDatabaseHelper? = null

        fun getInstance(context: Context): EmergencyDatabaseHelper {
            return instance ?: synchronized(this) {
                instance ?: EmergencyDatabaseHelper(context.applicationContext).also {
                    instance = it
                    it.seedDefaultEmergenciesIfMissing()
                }
            }
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createQuery = """
            CREATE TABLE IF NOT EXISTS $TABLE_EMERGENCIES (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_BUS_ID TEXT,
                $COLUMN_DRIVER_ID TEXT,
                $COLUMN_LOCATION TEXT,
                $COLUMN_STATUS TEXT,
                $COLUMN_SEVERITY TEXT,
                $COLUMN_DESCRIPTION TEXT,
                $COLUMN_CREATED_AT TEXT,
                $COLUMN_UPDATED_AT TEXT,
                $COLUMN_DRIVER_NAME TEXT,
                $COLUMN_USERNAME TEXT,
                $COLUMN_BUS_NO TEXT
            )
        """.trimIndent()
        db.execSQL(createQuery)
        seedDefaults(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        val createQuery = """
            CREATE TABLE IF NOT EXISTS $TABLE_EMERGENCIES (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_BUS_ID TEXT,
                $COLUMN_DRIVER_ID TEXT,
                $COLUMN_LOCATION TEXT,
                $COLUMN_STATUS TEXT,
                $COLUMN_SEVERITY TEXT,
                $COLUMN_DESCRIPTION TEXT,
                $COLUMN_CREATED_AT TEXT,
                $COLUMN_UPDATED_AT TEXT,
                $COLUMN_DRIVER_NAME TEXT,
                $COLUMN_USERNAME TEXT,
                $COLUMN_BUS_NO TEXT
            )
        """.trimIndent()
        db.execSQL(createQuery)
    }

    private fun seedDefaults(db: SQLiteDatabase) {
        try {
            val alerts = listOf(
                Emergency(
                    id = 101L,
                    busId = "102",
                    busNo = "TB-102",
                    location = "Campus Gate 2 - Highway Junction",
                    status = "Active",
                    severity = "Critical",
                    description = "Engine overheating warning triggered on Route 9. Vehicle halted safely at designated roadside bay.",
                    driverName = "Michael Scott",
                    driverPhone = "+91 98250 12345",
                    createdAt = "10 mins ago"
                ),
                Emergency(
                    id = 102L,
                    busId = "442",
                    busNo = "TB-442",
                    location = "Ring Road Circle, Near Bypass",
                    status = "Reported",
                    severity = "High",
                    description = "Rear axle tire pressure anomaly alert. Driver requested precautionary mechanical assistance.",
                    driverName = "MR. SUDHIRBHAI BATUKBHAI BHUTA",
                    driverPhone = "+91 98251 10001",
                    createdAt = "25 mins ago"
                ),
                Emergency(
                    id = 103L,
                    busId = "089",
                    busNo = "TB-089",
                    location = "City Center Bus Stop 3",
                    status = "Active",
                    severity = "Medium",
                    description = "Severe congestion on Jamnagar Highway. Delay of ~15 minutes expected for morning pickup points.",
                    driverName = "MR. MOSIN AJIJBHAI SANDHVANI",
                    driverPhone = "+91 98251 10002",
                    createdAt = "45 mins ago"
                )
            )

            for (e in alerts) {
                val cv = ContentValues().apply {
                    put(COLUMN_BUS_ID, e.busId)
                    put(COLUMN_DRIVER_ID, e.driverId)
                    put(COLUMN_LOCATION, e.location)
                    put(COLUMN_STATUS, e.status)
                    put(COLUMN_SEVERITY, e.severity)
                    put(COLUMN_DESCRIPTION, e.description)
                    put(COLUMN_CREATED_AT, e.createdAt)
                    put(COLUMN_UPDATED_AT, e.updatedAt)
                    put(COLUMN_DRIVER_NAME, e.driverName)
                    put(COLUMN_USERNAME, e.username)
                    put(COLUMN_BUS_NO, e.busNo)
                }
                db.insert(TABLE_EMERGENCIES, null, cv)
            }
            Log.d(TAG, "Default emergencies seeded into SQLite.")
        } catch (e: Exception) {
            Log.e(TAG, "Error seeding defaults: ${e.message}", e)
        }
    }

    fun seedDefaultEmergenciesIfMissing() {
        try {
            val db = writableDatabase
            // Ensure table exists
            val createQuery = """
                CREATE TABLE IF NOT EXISTS $TABLE_EMERGENCIES (
                    $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                    $COLUMN_BUS_ID TEXT,
                    $COLUMN_DRIVER_ID TEXT,
                    $COLUMN_LOCATION TEXT,
                    $COLUMN_STATUS TEXT,
                    $COLUMN_SEVERITY TEXT,
                    $COLUMN_DESCRIPTION TEXT,
                    $COLUMN_CREATED_AT TEXT,
                    $COLUMN_UPDATED_AT TEXT,
                    $COLUMN_DRIVER_NAME TEXT,
                    $COLUMN_USERNAME TEXT,
                    $COLUMN_BUS_NO TEXT
                )
            """.trimIndent()
            db.execSQL(createQuery)

            val cursor = db.rawQuery("SELECT COUNT(*) FROM $TABLE_EMERGENCIES", null)
            val count = cursor.use { if (it.moveToFirst()) it.getInt(0) else 0 }
            if (count == 0) {
                seedDefaults(db)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error ensuring emergencies table: ${e.message}", e)
        }
    }

    fun getAllEmergencies(): List<Emergency> {
        val list = mutableListOf<Emergency>()
        try {
            val db = readableDatabase
            val cursor = db.rawQuery(
                "SELECT * FROM $TABLE_EMERGENCIES ORDER BY $COLUMN_ID DESC",
                null
            )

            cursor.use {
                val idxId = it.getColumnIndex(COLUMN_ID)
                val idxBusId = it.getColumnIndex(COLUMN_BUS_ID)
                val idxDriverId = it.getColumnIndex(COLUMN_DRIVER_ID)
                val idxLocation = it.getColumnIndex(COLUMN_LOCATION)
                val idxStatus = it.getColumnIndex(COLUMN_STATUS)
                val idxSeverity = it.getColumnIndex(COLUMN_SEVERITY)
                val idxDescription = it.getColumnIndex(COLUMN_DESCRIPTION)
                val idxCreatedAt = it.getColumnIndex(COLUMN_CREATED_AT)
                val idxUpdatedAt = it.getColumnIndex(COLUMN_UPDATED_AT)
                val idxDriverName = it.getColumnIndex(COLUMN_DRIVER_NAME)
                val idxUsername = it.getColumnIndex(COLUMN_USERNAME)
                val idxBusNo = it.getColumnIndex(COLUMN_BUS_NO)

                while (it.moveToNext()) {
                    val e = Emergency(
                        id = if (idxId != -1) it.getLong(idxId) else 0L,
                        busId = if (idxBusId != -1) (it.getString(idxBusId) ?: "") else "",
                        driverId = if (idxDriverId != -1) (it.getString(idxDriverId) ?: "") else "",
                        location = if (idxLocation != -1) (it.getString(idxLocation) ?: "") else "",
                        status = if (idxStatus != -1) (it.getString(idxStatus) ?: "") else "",
                        severity = if (idxSeverity != -1) (it.getString(idxSeverity) ?: "") else "",
                        description = if (idxDescription != -1) (it.getString(idxDescription) ?: "") else "",
                        createdAt = if (idxCreatedAt != -1) (it.getString(idxCreatedAt) ?: "") else "",
                        updatedAt = if (idxUpdatedAt != -1) (it.getString(idxUpdatedAt) ?: "") else "",
                        driverName = if (idxDriverName != -1) (it.getString(idxDriverName) ?: "") else "",
                        username = if (idxUsername != -1) (it.getString(idxUsername) ?: "") else "",
                        busNo = if (idxBusNo != -1) (it.getString(idxBusNo) ?: "") else ""
                    )
                    list.add(e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading emergencies: ${e.message}", e)
        }
        return list
    }
}
