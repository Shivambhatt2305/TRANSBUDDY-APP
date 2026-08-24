package com.transbuddy.app.utils

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import com.transbuddy.app.controllers.LoginActivity
import com.transbuddy.app.models.User

/**
 * SessionManager — Manages user session state and authentication persistence.
 */
object SessionManager {

    private const val PREF_NAME = "transbuddy_user_session"
    private const val KEY_IS_LOGGED_IN = "is_logged_in"
    private const val KEY_USERNAME = "username"
    private const val KEY_FULL_NAME = "full_name"
    private const val KEY_ROLE = "role"
    private const val KEY_EMAIL = "email"
    private const val KEY_REMEMBER_ME = "remember_me"

    private fun getPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Save user session after successful authentication
     */
    fun createLoginSession(context: Context, user: User, rememberMe: Boolean = true) {
        val editor = getPreferences(context).edit()
        editor.putBoolean(KEY_IS_LOGGED_IN, true)
        editor.putString(KEY_USERNAME, user.username)
        editor.putString(KEY_FULL_NAME, user.fullName.ifBlank { "Marwadi Transport Admin" })
        editor.putString(KEY_ROLE, user.role.ifBlank { "Fleet Manager Admin" })
        editor.putString(KEY_EMAIL, user.email)
        editor.putBoolean(KEY_REMEMBER_ME, rememberMe)
        editor.apply()
    }

    /**
     * Check if user is currently logged in
     */
    fun isLoggedIn(context: Context): Boolean {
        val prefs = getPreferences(context)
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false)
    }

    /**
     * Get logged-in username
     */
    fun getUsername(context: Context): String {
        return getPreferences(context).getString(KEY_USERNAME, "marwadi") ?: "marwadi"
    }

    /**
     * Get logged-in user full name
     */
    fun getFullName(context: Context): String {
        return getPreferences(context).getString(KEY_FULL_NAME, "Marwadi Transport Admin") ?: "Marwadi Transport Admin"
    }

    /**
     * Get user role
     */
    fun getRole(context: Context): String {
        return getPreferences(context).getString(KEY_ROLE, "Fleet Manager Admin") ?: "Fleet Manager Admin"
    }

    /**
     * Get user email
     */
    fun getEmail(context: Context): String {
        return getPreferences(context).getString(KEY_EMAIL, "marwadi@transbuddy.com") ?: "marwadi@transbuddy.com"
    }

    /**
     * Clear session and navigate to LoginActivity
     */
    fun logout(context: Context) {
        val editor = getPreferences(context).edit()
        editor.clear()
        editor.apply()

        val intent = Intent(context, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        context.startActivity(intent)
    }
}
