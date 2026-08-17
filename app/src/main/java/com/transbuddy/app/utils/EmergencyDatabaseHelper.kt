package com.transbuddy.app.utils

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.transbuddy.app.models.Emergency

class EmergencyDatabaseHelper private constructor(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "transbuddy_penalties.db"
        private const val DATABASE_VERSION = 9999

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
                instance ?: EmergencyDatabaseHelper(context.applicationContext).also { instance = it }
            }
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
    }

    fun getAllEmergencies(): List<Emergency> {
        val list = mutableListOf<Emergency>()
        try {
            val db = readableDatabase
            android.util.Log.d("EmergencyDatabaseHelper", "Reading from DB: $databaseName, path: ${db.path}")

            val cursor = db.rawQuery(
                "SELECT * FROM $TABLE_EMERGENCIES ORDER BY $COLUMN_ID DESC",
                null
            )
            android.util.Log.d("EmergencyDatabaseHelper", "Cursor count: ${cursor.count}")

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
            android.util.Log.e("EmergencyDatabaseHelper", "Error reading emergencies: ${e.message}", e)
        }
        return list
    }
}
