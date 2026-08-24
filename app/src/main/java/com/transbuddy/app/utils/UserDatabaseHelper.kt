package com.transbuddy.app.utils

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import com.transbuddy.app.models.User
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * UserDatabaseHelper — SQLite Database Helper for User Authentication
 * Manages the persistent 'users' table in local SQLite DB (transbuddy_users.db).
 * Automatically seeds the required user credentials (username: 'marwadi', password: 'marwadi@121').
 */
class UserDatabaseHelper private constructor(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val TAG = "UserDatabaseHelper"
        private const val DATABASE_NAME = "transbuddy_users.db"
        private const val DATABASE_VERSION = 1

        const val TABLE_USERS = "users"
        const val COLUMN_ID = "id"
        const val COLUMN_USERNAME = "username"
        const val COLUMN_PASSWORD = "password"
        const val COLUMN_FULL_NAME = "full_name"
        const val COLUMN_ROLE = "role"
        const val COLUMN_EMAIL = "email"
        const val COLUMN_PHONE = "phone"
        const val COLUMN_CREATED_AT = "created_at"

        @Volatile
        private var instance: UserDatabaseHelper? = null

        fun getInstance(context: Context): UserDatabaseHelper {
            return instance ?: synchronized(this) {
                instance ?: UserDatabaseHelper(context.applicationContext).also {
                    instance = it
                    it.seedDefaultUserIfMissing()
                }
            }
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTableQuery = """
            CREATE TABLE $TABLE_USERS (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_USERNAME TEXT UNIQUE NOT NULL,
                $COLUMN_PASSWORD TEXT NOT NULL,
                $COLUMN_FULL_NAME TEXT NOT NULL,
                $COLUMN_ROLE TEXT NOT NULL DEFAULT 'ADMIN',
                $COLUMN_EMAIL TEXT,
                $COLUMN_PHONE TEXT,
                $COLUMN_CREATED_AT TEXT
            )
        """.trimIndent()
        db.execSQL(createTableQuery)
        Log.d(TAG, "Table '$TABLE_USERS' created successfully.")

        seedDefaultUser(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_USERS")
        onCreate(db)
    }

    /**
     * Seeds the default Marwadi Transport Admin user into the database
     */
    private fun seedDefaultUser(db: SQLiteDatabase) {
        try {
            val dateStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date())
            val values = ContentValues().apply {
                put(COLUMN_USERNAME, "marwadi")
                put(COLUMN_PASSWORD, "marwadi@121")
                put(COLUMN_FULL_NAME, "Marwadi Transport Admin")
                put(COLUMN_ROLE, "TRANSPORT_ADMIN")
                put(COLUMN_EMAIL, "marwadi@marwadiuniversity.ac.in")
                put(COLUMN_PHONE, "+91 98765 43210")
                put(COLUMN_CREATED_AT, dateStr)
            }
            db.insertWithOnConflict(TABLE_USERS, null, values, SQLiteDatabase.CONFLICT_IGNORE)
            Log.d(TAG, "Default user 'marwadi' seeded into database.")
        } catch (e: Exception) {
            Log.e(TAG, "Error seeding default user: ${e.message}", e)
        }
    }

    /**
     * Ensures default credentials exist even after database updates or upgrades
     */
    fun seedDefaultUserIfMissing() {
        try {
            val db = writableDatabase
            val cursor = db.rawQuery(
                "SELECT $COLUMN_ID FROM $TABLE_USERS WHERE LOWER($COLUMN_USERNAME) = LOWER(?)",
                arrayOf("marwadi")
            )
            val exists = cursor.use { it.count > 0 }
            if (!exists) {
                seedDefaultUser(db)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking/seeding user: ${e.message}", e)
        }
    }

    /**
     * Authenticates user against the database.
     * Matches username (or email) and password.
     *
     * @param usernameOrEmail Input username or email (e.g. 'marwadi')
     * @param password Input password (e.g. 'marwadi@121')
     * @return User object if credentials are correct, null otherwise.
     */
    fun authenticate(usernameOrEmail: String, password: String): User? {
        val trimmedInput = usernameOrEmail.trim()
        val trimmedPassword = password.trim()

        if (trimmedInput.isEmpty() || trimmedPassword.isEmpty()) {
            return null
        }

        try {
            val db = readableDatabase
            val query = """
                SELECT * FROM $TABLE_USERS 
                WHERE (LOWER($COLUMN_USERNAME) = LOWER(?) OR LOWER($COLUMN_EMAIL) = LOWER(?)) 
                  AND $COLUMN_PASSWORD = ? 
                LIMIT 1
            """.trimIndent()

            val cursor = db.rawQuery(query, arrayOf(trimmedInput, trimmedInput, trimmedPassword))
            cursor.use {
                if (it.moveToFirst()) {
                    val idxId = it.getColumnIndex(COLUMN_ID)
                    val idxUsername = it.getColumnIndex(COLUMN_USERNAME)
                    val idxPassword = it.getColumnIndex(COLUMN_PASSWORD)
                    val idxFullName = it.getColumnIndex(COLUMN_FULL_NAME)
                    val idxRole = it.getColumnIndex(COLUMN_ROLE)
                    val idxEmail = it.getColumnIndex(COLUMN_EMAIL)
                    val idxPhone = it.getColumnIndex(COLUMN_PHONE)
                    val idxCreatedAt = it.getColumnIndex(COLUMN_CREATED_AT)

                    return User(
                        id = if (idxId != -1) it.getLong(idxId) else 0L,
                        username = if (idxUsername != -1) it.getString(idxUsername) ?: "" else "",
                        password = if (idxPassword != -1) it.getString(idxPassword) ?: "" else "",
                        fullName = if (idxFullName != -1) it.getString(idxFullName) ?: "" else "",
                        role = if (idxRole != -1) it.getString(idxRole) ?: "ADMIN" else "ADMIN",
                        email = if (idxEmail != -1) it.getString(idxEmail) ?: "" else "",
                        phone = if (idxPhone != -1) it.getString(idxPhone) ?: "" else "",
                        createdAt = if (idxCreatedAt != -1) it.getString(idxCreatedAt) ?: "" else ""
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Authentication query error: ${e.message}", e)
        }
        return null
    }

    /**
     * Inserts a new user into SQLite DB
     */
    fun insertUser(user: User): Long {
        val db = writableDatabase
        val dateStr = if (user.createdAt.isBlank()) {
            SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date())
        } else {
            user.createdAt
        }

        val values = ContentValues().apply {
            put(COLUMN_USERNAME, user.username)
            put(COLUMN_PASSWORD, user.password)
            put(COLUMN_FULL_NAME, user.fullName)
            put(COLUMN_ROLE, user.role)
            put(COLUMN_EMAIL, user.email)
            put(COLUMN_PHONE, user.phone)
            put(COLUMN_CREATED_AT, dateStr)
        }
        return db.insertWithOnConflict(TABLE_USERS, null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    /**
     * Fetches all registered users from DB
     */
    fun getAllUsers(): List<User> {
        val list = mutableListOf<User>()
        try {
            val db = readableDatabase
            val cursor = db.rawQuery("SELECT * FROM $TABLE_USERS ORDER BY $COLUMN_ID ASC", null)
            cursor.use {
                val idxId = it.getColumnIndex(COLUMN_ID)
                val idxUsername = it.getColumnIndex(COLUMN_USERNAME)
                val idxFullName = it.getColumnIndex(COLUMN_FULL_NAME)
                val idxRole = it.getColumnIndex(COLUMN_ROLE)
                val idxEmail = it.getColumnIndex(COLUMN_EMAIL)
                val idxPhone = it.getColumnIndex(COLUMN_PHONE)
                val idxCreatedAt = it.getColumnIndex(COLUMN_CREATED_AT)

                while (it.moveToNext()) {
                    list.add(
                        User(
                            id = if (idxId != -1) it.getLong(idxId) else 0L,
                            username = if (idxUsername != -1) it.getString(idxUsername) ?: "" else "",
                            fullName = if (idxFullName != -1) it.getString(idxFullName) ?: "" else "",
                            role = if (idxRole != -1) it.getString(idxRole) ?: "ADMIN" else "ADMIN",
                            email = if (idxEmail != -1) it.getString(idxEmail) ?: "" else "",
                            phone = if (idxPhone != -1) it.getString(idxPhone) ?: "" else "",
                            createdAt = if (idxCreatedAt != -1) it.getString(idxCreatedAt) ?: "" else ""
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading all users: ${e.message}", e)
        }
        return list
    }
}
