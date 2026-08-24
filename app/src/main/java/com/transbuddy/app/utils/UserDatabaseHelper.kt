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
 * UserDatabaseHelper — SQLite Database Helper for TransBuddy Authentication
 * Manages user accounts, authentication verification, and ensures default
 * user 'marwadi' with password 'marwadi@121' is pre-seeded in the database.
 */
class UserDatabaseHelper private constructor(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val TAG = "UserDatabaseHelper"
        private const val DATABASE_NAME = "transbuddy_penalties.db"
        private const val DATABASE_VERSION = 3

        const val TABLE_USERS = "users"
        const val COLUMN_ID = "id"
        const val COLUMN_USERNAME = "username"
        const val COLUMN_PASSWORD = "password"
        const val COLUMN_EMAIL = "email"
        const val COLUMN_FULL_NAME = "full_name"
        const val COLUMN_ROLE = "role"
        const val COLUMN_CREATED_AT = "created_at"

        // Default Seed Credentials
        const val DEFAULT_USERNAME = "marwadi"
        const val DEFAULT_PASSWORD = "marwadi@121"
        const val DEFAULT_EMAIL = "marwadi@transbuddy.com"
        const val DEFAULT_FULL_NAME = "Marwadi University Fleet Admin"
        const val DEFAULT_ROLE = "Fleet Manager Admin"

        @Volatile
        private var instance: UserDatabaseHelper? = null

        fun getInstance(context: Context): UserDatabaseHelper {
            return instance ?: synchronized(this) {
                instance ?: UserDatabaseHelper(context.applicationContext).also {
                    instance = it
                    it.ensureTableAndDefaultUser()
                }
            }
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        createUsersTable(db)
        seedDefaultUser(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        createUsersTable(db)
        seedDefaultUser(db)
    }

    override fun onOpen(db: SQLiteDatabase) {
        super.onOpen(db)
        createUsersTable(db)
        seedDefaultUser(db)
    }

    /**
     * Ensures the users table exists and the default user is seeded.
     */
    fun ensureTableAndDefaultUser() {
        try {
            val db = writableDatabase
            createUsersTable(db)
            seedDefaultUser(db)
        } catch (e: Exception) {
            Log.e(TAG, "Error in ensureTableAndDefaultUser: ${e.message}", e)
        }
    }

    private fun createUsersTable(db: SQLiteDatabase) {
        val createTableQuery = """
            CREATE TABLE IF NOT EXISTS $TABLE_USERS (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_USERNAME TEXT UNIQUE NOT NULL,
                $COLUMN_PASSWORD TEXT NOT NULL,
                $COLUMN_EMAIL TEXT,
                $COLUMN_FULL_NAME TEXT,
                $COLUMN_ROLE TEXT DEFAULT 'Fleet Manager Admin',
                $COLUMN_CREATED_AT TEXT
            )
        """.trimIndent()
        db.execSQL(createTableQuery)
    }

    private fun seedDefaultUser(db: SQLiteDatabase) {
        try {
            val cursor = db.rawQuery(
                "SELECT $COLUMN_ID FROM $TABLE_USERS WHERE LOWER($COLUMN_USERNAME) = LOWER(?)",
                arrayOf(DEFAULT_USERNAME)
            )
            val exists = cursor.use { it.moveToFirst() }
            if (!exists) {
                val dateStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date())
                val values = ContentValues().apply {
                    put(COLUMN_USERNAME, DEFAULT_USERNAME)
                    put(COLUMN_PASSWORD, DEFAULT_PASSWORD)
                    put(COLUMN_EMAIL, DEFAULT_EMAIL)
                    put(COLUMN_FULL_NAME, DEFAULT_FULL_NAME)
                    put(COLUMN_ROLE, DEFAULT_ROLE)
                    put(COLUMN_CREATED_AT, dateStr)
                }
                val rowId = db.insert(TABLE_USERS, null, values)
                Log.d(TAG, "Default user '$DEFAULT_USERNAME' seeded successfully with id: $rowId")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error seeding default user: ${e.message}", e)
        }
    }

    /**
     * Verifies username and password against the SQLite database.
     * Uses parameterized query to safely prevent SQL injection.
     * @return User object if authenticated, null otherwise.
     */
    fun verifyCredentials(username: String, password: String): User? {
        val cleanUsername = username.trim()
        val cleanPassword = password.trim()

        if (cleanUsername.isEmpty() || cleanPassword.isEmpty()) {
            return null
        }

        try {
            val db = readableDatabase
            val query = """
                SELECT $COLUMN_ID, $COLUMN_USERNAME, $COLUMN_PASSWORD, $COLUMN_EMAIL, 
                       $COLUMN_FULL_NAME, $COLUMN_ROLE, $COLUMN_CREATED_AT 
                FROM $TABLE_USERS 
                WHERE LOWER($COLUMN_USERNAME) = LOWER(?) AND $COLUMN_PASSWORD = ?
                LIMIT 1
            """.trimIndent()

            val cursor = db.rawQuery(query, arrayOf(cleanUsername, cleanPassword))
            cursor.use {
                if (it.moveToFirst()) {
                    val idxId = it.getColumnIndex(COLUMN_ID)
                    val idxUsername = it.getColumnIndex(COLUMN_USERNAME)
                    val idxPassword = it.getColumnIndex(COLUMN_PASSWORD)
                    val idxEmail = it.getColumnIndex(COLUMN_EMAIL)
                    val idxFullName = it.getColumnIndex(COLUMN_FULL_NAME)
                    val idxRole = it.getColumnIndex(COLUMN_ROLE)
                    val idxCreatedAt = it.getColumnIndex(COLUMN_CREATED_AT)

                    return User(
                        id = if (idxId != -1) it.getLong(idxId) else 0L,
                        username = if (idxUsername != -1) it.getString(idxUsername) else cleanUsername,
                        password = if (idxPassword != -1) it.getString(idxPassword) else "",
                        email = if (idxEmail != -1) (it.getString(idxEmail) ?: "") else "",
                        fullName = if (idxFullName != -1) (it.getString(idxFullName) ?: "") else "",
                        role = if (idxRole != -1) (it.getString(idxRole) ?: DEFAULT_ROLE) else DEFAULT_ROLE,
                        createdAt = if (idxCreatedAt != -1) (it.getString(idxCreatedAt) ?: "") else ""
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error verifying credentials: ${e.message}", e)
        }
        return null
    }

    /**
     * Fetch a user by username.
     */
    fun getUserByUsername(username: String): User? {
        val cleanUsername = username.trim()
        if (cleanUsername.isEmpty()) return null

        try {
            val db = readableDatabase
            val cursor = db.rawQuery(
                "SELECT * FROM $TABLE_USERS WHERE LOWER($COLUMN_USERNAME) = LOWER(?) LIMIT 1",
                arrayOf(cleanUsername)
            )
            cursor.use {
                if (it.moveToFirst()) {
                    val idxId = it.getColumnIndex(COLUMN_ID)
                    val idxUsername = it.getColumnIndex(COLUMN_USERNAME)
                    val idxPassword = it.getColumnIndex(COLUMN_PASSWORD)
                    val idxEmail = it.getColumnIndex(COLUMN_EMAIL)
                    val idxFullName = it.getColumnIndex(COLUMN_FULL_NAME)
                    val idxRole = it.getColumnIndex(COLUMN_ROLE)
                    val idxCreatedAt = it.getColumnIndex(COLUMN_CREATED_AT)

                    return User(
                        id = if (idxId != -1) it.getLong(idxId) else 0L,
                        username = if (idxUsername != -1) it.getString(idxUsername) else cleanUsername,
                        password = if (idxPassword != -1) it.getString(idxPassword) else "",
                        email = if (idxEmail != -1) (it.getString(idxEmail) ?: "") else "",
                        fullName = if (idxFullName != -1) (it.getString(idxFullName) ?: "") else "",
                        role = if (idxRole != -1) (it.getString(idxRole) ?: DEFAULT_ROLE) else DEFAULT_ROLE,
                        createdAt = if (idxCreatedAt != -1) (it.getString(idxCreatedAt) ?: "") else ""
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting user: ${e.message}", e)
        }
        return null
    }

    /**
     * Insert a new user into the database
     */
    fun insertUser(user: User): Long {
        return try {
            val db = writableDatabase
            val dateStr = if (user.createdAt.isBlank()) {
                SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date())
            } else {
                user.createdAt
            }
            val values = ContentValues().apply {
                put(COLUMN_USERNAME, user.username.trim())
                put(COLUMN_PASSWORD, user.password)
                put(COLUMN_EMAIL, user.email.trim())
                put(COLUMN_FULL_NAME, user.fullName.trim())
                put(COLUMN_ROLE, user.role.trim().ifEmpty { DEFAULT_ROLE })
                put(COLUMN_CREATED_AT, dateStr)
            }
            db.insertWithOnConflict(TABLE_USERS, null, values, SQLiteDatabase.CONFLICT_REPLACE)
        } catch (e: Exception) {
            Log.e(TAG, "Error inserting user: ${e.message}", e)
            -1L
        }
    }

    /**
     * Check if a username exists
     */
    fun userExists(username: String): Boolean {
        return try {
            val db = readableDatabase
            val cursor = db.rawQuery(
                "SELECT $COLUMN_ID FROM $TABLE_USERS WHERE LOWER($COLUMN_USERNAME) = LOWER(?)",
                arrayOf(username.trim())
            )
            cursor.use { it.moveToFirst() }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking userExists: ${e.message}", e)
            false
        }
    }

    /**
     * Get all users in the system
     */
    fun getAllUsers(): List<User> {
        val list = mutableListOf<User>()
        try {
            val db = readableDatabase
            val cursor = db.rawQuery("SELECT * FROM $TABLE_USERS ORDER BY $COLUMN_ID ASC", null)
            cursor.use {
                val idxId = it.getColumnIndex(COLUMN_ID)
                val idxUsername = it.getColumnIndex(COLUMN_USERNAME)
                val idxEmail = it.getColumnIndex(COLUMN_EMAIL)
                val idxFullName = it.getColumnIndex(COLUMN_FULL_NAME)
                val idxRole = it.getColumnIndex(COLUMN_ROLE)
                val idxCreatedAt = it.getColumnIndex(COLUMN_CREATED_AT)

                while (it.moveToNext()) {
                    list.add(
                        User(
                            id = if (idxId != -1) it.getLong(idxId) else 0L,
                            username = if (idxUsername != -1) it.getString(idxUsername) else "",
                            password = "••••••••", // mask password in lists
                            email = if (idxEmail != -1) (it.getString(idxEmail) ?: "") else "",
                            fullName = if (idxFullName != -1) (it.getString(idxFullName) ?: "") else "",
                            role = if (idxRole != -1) (it.getString(idxRole) ?: DEFAULT_ROLE) else DEFAULT_ROLE,
                            createdAt = if (idxCreatedAt != -1) (it.getString(idxCreatedAt) ?: "") else ""
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching all users: ${e.message}", e)
        }
        return list
    }
}
