package com.transbuddy.app.utils

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import com.transbuddy.app.controllers.LoginActivity
import com.transbuddy.app.models.User

/**
 * SessionManager — Manages persistent login session via SharedPreferences.
 */
class SessionManager private constructor(context: Context) {

    companion object {
        private const val PREF_NAME = "transbuddy_user_session"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USERNAME = "username"
        private const val KEY_FULL_NAME = "full_name"
        private const val KEY_ROLE = "role"
        private const val KEY_EMAIL = "email"
        private const val KEY_PHONE = "phone"

        @Volatile
        private var instance: SessionManager? = null

        fun getInstance(context: Context): SessionManager {
            return instance ?: synchronized(this) {
                instance ?: SessionManager(context.applicationContext).also { instance = it }
            }
        }
    }

    private val pref: SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    private val editor: SharedPreferences.Editor = pref.edit()

    /**
     * Saves user login session
     */
    fun createLoginSession(user: User) {
        editor.putBoolean(KEY_IS_LOGGED_IN, true)
        editor.putLong(KEY_USER_ID, user.id)
        editor.putString(KEY_USERNAME, user.username)
        editor.putString(KEY_FULL_NAME, user.fullName)
        editor.putString(KEY_ROLE, user.role)
        editor.putString(KEY_EMAIL, user.email)
        editor.putString(KEY_PHONE, user.phone)
        editor.apply()
    }

    /**
     * Checks if a user is currently logged in
     */
    fun isLoggedIn(): Boolean {
        return pref.getBoolean(KEY_IS_LOGGED_IN, false)
    }

    /**
     * Retrieves currently logged in user info
     */
    fun getUser(): User? {
        if (!isLoggedIn()) return null
        return User(
            id = pref.getLong(KEY_USER_ID, 0L),
            username = pref.getString(KEY_USERNAME, "") ?: "",
            fullName = pref.getString(KEY_FULL_NAME, "Marwadi Admin") ?: "Marwadi Admin",
            role = pref.getString(KEY_ROLE, "TRANSPORT_ADMIN") ?: "TRANSPORT_ADMIN",
            email = pref.getString(KEY_EMAIL, "") ?: "",
            phone = pref.getString(KEY_PHONE, "") ?: ""
        )
    }

    /**
     * Clears user session and redirects to LoginActivity
     */
    fun logout(context: Context) {
        editor.clear()
        editor.apply()

        val intent = Intent(context, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        context.startActivity(intent)
    }
}
