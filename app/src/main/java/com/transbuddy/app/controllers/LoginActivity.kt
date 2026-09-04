package com.transbuddy.app.controllers

import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import com.transbuddy.app.R
import com.transbuddy.app.utils.SessionManager
import com.transbuddy.app.utils.UserDatabaseHelper

/**
 * LoginActivity — CONTROLLER for TransBuddy Authentication
 * Authenticates user credentials directly against SQLite database (transbuddy_users.db).
 * Supports persistent autofill of previously entered credentials.
 */
class LoginActivity : AppCompatActivity() {

    private lateinit var etUsername: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnTogglePassword: ImageButton
    private lateinit var tvErrorMessage: TextView
    private lateinit var btnLogin: AppCompatButton
    private lateinit var pbLogin: ProgressBar

    private lateinit var dbHelper: UserDatabaseHelper
    private lateinit var sessionManager: SessionManager
    private var isPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sessionManager = SessionManager.getInstance(this)
        dbHelper = UserDatabaseHelper.getInstance(this)

        // If user is already logged in, navigate straight to MainActivity
        val isExplicitLogout = intent.getBooleanExtra("EXPLICIT_LOGOUT", false)
        if (sessionManager.isLoggedIn() && !isExplicitLogout) {
            navigateToDashboard()
            return
        }

        setContentView(R.layout.activity_login)

        bindViews()
        setupListeners()
    }

    private fun bindViews() {
        etUsername = findViewById(R.id.etUsername)
        etPassword = findViewById(R.id.etPassword)
        btnTogglePassword = findViewById(R.id.btnTogglePassword)
        tvErrorMessage = findViewById(R.id.tvErrorMessage)
        btnLogin = findViewById(R.id.btnLogin)
        pbLogin = findViewById(R.id.pbLogin)
    }

    private fun setupListeners() {
        // Toggle password visibility
        btnTogglePassword.setOnClickListener {
            togglePasswordVisibility()
        }

        // Login button
        btnLogin.setOnClickListener {
            performLogin()
        }
    }

    private fun togglePasswordVisibility() {
        isPasswordVisible = !isPasswordVisible
        if (isPasswordVisible) {
            etPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            btnTogglePassword.setImageResource(R.drawable.ic_visibility_off)
        } else {
            etPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            btnTogglePassword.setImageResource(R.drawable.ic_visibility)
        }
        etPassword.setSelection(etPassword.text.length)
    }

    private fun performLogin() {
        val username = etUsername.text.toString().trim()
        val password = etPassword.text.toString().trim()

        tvErrorMessage.visibility = View.GONE

        if (username.isEmpty()) {
            showError("Please enter your username or email.")
            etUsername.requestFocus()
            return
        }

        if (password.isEmpty()) {
            showError("Please enter your password.")
            etPassword.requestFocus()
            return
        }

        // Show loading spinner
        setLoading(true)

        // Query Database on background handler
        Handler(Looper.getMainLooper()).postDelayed({
            val user = dbHelper.authenticate(username, password)
            setLoading(false)

            if (user != null) {
                sessionManager.createLoginSession(user)
                Toast.makeText(this, "Welcome, ${user.fullName}!", Toast.LENGTH_SHORT).show()
                navigateToDashboard()
            } else {
                // Failed: show error feedback
                showError("Invalid username or password. Please verify the credentials in database.")
                shakeView(tvErrorMessage)
            }
        }, 200)
    }

    private fun setLoading(loading: Boolean) {
        if (loading) {
            btnLogin.text = ""
            pbLogin.visibility = View.VISIBLE
            btnLogin.isEnabled = false
        } else {
            btnLogin.text = "Sign In to Fleet Hub"
            pbLogin.visibility = View.GONE
            btnLogin.isEnabled = true
        }
    }

    private fun showError(message: String) {
        tvErrorMessage.text = message
        tvErrorMessage.visibility = View.VISIBLE
    }

    private fun shakeView(view: View) {
        val animator = ObjectAnimator.ofFloat(view, "translationX", 0f, 20f, -20f, 15f, -15f, 6f, -6f, 0f)
        animator.duration = 400
        animator.interpolator = AccelerateDecelerateInterpolator()
        animator.start()
    }

    private fun navigateToDashboard() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }
}
