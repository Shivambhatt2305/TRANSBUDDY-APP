package com.transbuddy.app.utils

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.transbuddy.app.models.Penalty
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * PenaltyDatabaseHelper — SQLite Database Helper
 * Handles persistence for all driver and student penalties issued in TransBuddy App.
 * Ensures assigned penalties persist in local DB table and are rendered across activities.
 */
class PenaltyDatabaseHelper private constructor(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "transbuddy_penalties.db"
        private const val DATABASE_VERSION = 3

        const val TABLE_PENALTIES = "penalties"
        const val COLUMN_ID = "id"
        const val COLUMN_TARGET_TYPE = "target_type"
        const val COLUMN_ICON_TYPE = "icon_type"
        const val COLUMN_TITLE = "title"
        const val COLUMN_DRIVER_INFO = "driver_info"
        const val COLUMN_AMOUNT = "amount"
        const val COLUMN_IS_ERROR = "is_error"
        const val COLUMN_TARGET_EMAIL = "target_email"
        const val COLUMN_NOTES = "notes"
        const val COLUMN_CREATED_AT = "created_at"
        const val COLUMN_STATUS = "status"
        const val COLUMN_PHOTO_URL = "photo_url"

        @Volatile
        private var instance: PenaltyDatabaseHelper? = null

        fun getInstance(context: Context): PenaltyDatabaseHelper {
            return instance ?: synchronized(this) {
                instance ?: PenaltyDatabaseHelper(context.applicationContext).also { instance = it }
            }
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTableQuery = """
            CREATE TABLE $TABLE_PENALTIES (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_TARGET_TYPE TEXT NOT NULL DEFAULT 'DRIVER',
                $COLUMN_ICON_TYPE TEXT NOT NULL DEFAULT 'speeding',
                $COLUMN_TITLE TEXT NOT NULL DEFAULT 'Violation',
                $COLUMN_DRIVER_INFO TEXT NOT NULL DEFAULT '',
                $COLUMN_AMOUNT TEXT NOT NULL DEFAULT '0',
                $COLUMN_IS_ERROR INTEGER DEFAULT 1,
                $COLUMN_TARGET_EMAIL TEXT,
                $COLUMN_NOTES TEXT,
                $COLUMN_CREATED_AT TEXT,
                $COLUMN_STATUS TEXT DEFAULT 'PENDING',
                $COLUMN_PHOTO_URL TEXT
            )
        """.trimIndent()
        db.execSQL(createTableQuery)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Preserve penalties during upgrades—especially records waiting for
        // cloud upload.
        addColumnIfMissing(db, COLUMN_TARGET_TYPE, "TEXT NOT NULL DEFAULT 'DRIVER'")
        addColumnIfMissing(db, COLUMN_ICON_TYPE, "TEXT NOT NULL DEFAULT 'speeding'")
        addColumnIfMissing(db, COLUMN_TITLE, "TEXT NOT NULL DEFAULT 'Violation'")
        addColumnIfMissing(db, COLUMN_DRIVER_INFO, "TEXT NOT NULL DEFAULT ''")
        addColumnIfMissing(db, COLUMN_AMOUNT, "TEXT NOT NULL DEFAULT '0'")
        addColumnIfMissing(db, COLUMN_IS_ERROR, "INTEGER DEFAULT 1")
        addColumnIfMissing(db, COLUMN_TARGET_EMAIL, "TEXT")
        addColumnIfMissing(db, COLUMN_NOTES, "TEXT")
        addColumnIfMissing(db, COLUMN_CREATED_AT, "TEXT")
        addColumnIfMissing(db, COLUMN_STATUS, "TEXT DEFAULT 'PENDING'")
        addColumnIfMissing(db, COLUMN_PHOTO_URL, "TEXT")
    }

    private fun addColumnIfMissing(db: SQLiteDatabase, column: String, definition: String) {
        val columns = mutableSetOf<String>()
        db.rawQuery("PRAGMA table_info($TABLE_PENALTIES)", null).use { cursor ->
            val nameIndex = cursor.getColumnIndex("name")
            while (cursor.moveToNext()) columns.add(cursor.getString(nameIndex))
        }
        if (column !in columns) db.execSQL("ALTER TABLE $TABLE_PENALTIES ADD COLUMN $column $definition")
    }

    /**
     * Insert a new Penalty into DB table
     */
    fun insertPenalty(penalty: Penalty): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_TARGET_TYPE, penalty.targetType)
            put(COLUMN_ICON_TYPE, penalty.iconType)
            put(COLUMN_TITLE, penalty.title)
            put(COLUMN_DRIVER_INFO, penalty.driverInfo)
            put(COLUMN_AMOUNT, penalty.amount)
            put(COLUMN_IS_ERROR, if (penalty.isError) 1 else 0)
            put(COLUMN_TARGET_EMAIL, penalty.targetEmail)
            put(COLUMN_NOTES, penalty.notes)
            put(COLUMN_PHOTO_URL, penalty.photoUrl)

            val dateStr = if (penalty.createdAt.isBlank()) {
                SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date())
            } else {
                penalty.createdAt
            }
            put(COLUMN_CREATED_AT, dateStr)
            put(COLUMN_STATUS, penalty.status)
        }
        return db.insert(TABLE_PENALTIES, null, values)
    }

    /**
     * Fetch all penalty records ordered by newest first safely
     */
    fun getAllPenalties(): List<Penalty> {
        val list = mutableListOf<Penalty>()
        try {
            val db = readableDatabase
            val cursor = db.rawQuery(
                "SELECT * FROM $TABLE_PENALTIES ORDER BY $COLUMN_ID DESC",
                null
            )

            cursor.use {
                val idxId = it.getColumnIndex(COLUMN_ID)
                val idxTargetType = it.getColumnIndex(COLUMN_TARGET_TYPE)
                val idxIconType = it.getColumnIndex(COLUMN_ICON_TYPE)
                val idxTitle = it.getColumnIndex(COLUMN_TITLE)
                val idxDriverInfo = it.getColumnIndex(COLUMN_DRIVER_INFO)
                val idxAmount = it.getColumnIndex(COLUMN_AMOUNT)
                val idxIsError = it.getColumnIndex(COLUMN_IS_ERROR)
                val idxTargetEmail = it.getColumnIndex(COLUMN_TARGET_EMAIL)
                val idxNotes = it.getColumnIndex(COLUMN_NOTES)
                val idxCreatedAt = it.getColumnIndex(COLUMN_CREATED_AT)
                val idxStatus = it.getColumnIndex(COLUMN_STATUS)
                val idxPhotoUrl = it.getColumnIndex(COLUMN_PHOTO_URL)

                while (it.moveToNext()) {
                    val p = Penalty(
                        id = if (idxId != -1) it.getLong(idxId) else 0L,
                        targetType = if (idxTargetType != -1) (it.getString(idxTargetType) ?: "DRIVER") else "DRIVER",
                        iconType = if (idxIconType != -1) (it.getString(idxIconType) ?: "speeding") else "speeding",
                        title = if (idxTitle != -1) (it.getString(idxTitle) ?: "Violation") else "Violation",
                        driverInfo = if (idxDriverInfo != -1) (it.getString(idxDriverInfo) ?: "") else "",
                        amount = if (idxAmount != -1) (it.getString(idxAmount) ?: "0") else "0",
                        isError = if (idxIsError != -1) (it.getInt(idxIsError) == 1) else true,
                        targetEmail = if (idxTargetEmail != -1) (it.getString(idxTargetEmail) ?: "") else "",
                        notes = if (idxNotes != -1) (it.getString(idxNotes) ?: "") else "",
                        createdAt = if (idxCreatedAt != -1) (it.getString(idxCreatedAt) ?: "") else "",
                        status = if (idxStatus != -1) (it.getString(idxStatus) ?: "PENDING") else "PENDING",
                        photoUrl = if (idxPhotoUrl != -1) (it.getString(idxPhotoUrl) ?: "") else ""
                    )
                    list.add(p)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("PenaltyDatabaseHelper", "Error reading penalties: ${e.message}", e)
        }
        return list
    }

    /**
     * Delete penalty record by ID
     */
    fun deletePenalty(id: Long): Boolean {
        val db = writableDatabase
        val rows = db.delete(TABLE_PENALTIES, "$COLUMN_ID = ?", arrayOf(id.toString()))
        return rows > 0
    }
}
