package com.transbuddy.app.controllers

import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.view.View
import android.view.animation.CycleInterpolator
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.transbuddy.app.R
import com.transbuddy.app.utils.SessionManager
import com.transbuddy.app.utils.UserDatabaseHelper

/**
 * LoginActivity — CONTROLLER
 * Handles user authentication against local SQLite database (UserDatabaseHelper).
 * Supports pre-configured user 'marwadi' with password 'marwadi@121'.
 */
class LoginActivity : AppCompatActivity() {

    private lateinit var etUsername: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var progressBarLogin: ProgressBar
    private lateinit var btnTogglePassword: ImageButton
    private lateinit var cbRememberMe: CheckBox
    private lateinit var layoutError: LinearLayout
    private lateinit var tvErrorMessage: TextView
    private lateinit var layoutDemoCredentials: LinearLayout
    private lateinit var boxUsername: LinearLayout
    private lateinit var boxPassword: LinearLayout

    private var isPasswordVisible = false
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Check if user is already logged in
        if (SessionManager.isLoggedIn(this)) {
            navigateToDashboard()
            return
        }

        setContentView(R.layout.activity_login)

        // 2. Initialize Database & Seed default user
        UserDatabaseHelper.getInstance(this).ensureTableAndDefaultUser()

        // 3. Bind view references
        bindViews()

        // 4. Setup listeners
        setupPasswordToggle()
        setupDemoAutofill()
        setupLoginButton()
    }

    private fun bindViews() {
        etUsername = findViewById(R.id.etUsername)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        progressBarLogin = findViewById(R.id.progressBarLogin)
        btnTogglePassword = findViewById(R.id.btnTogglePassword)
        cbRememberMe = findViewById(R.id.cbRememberMe)
        layoutError = findViewById(R.id.layoutError)
        tvErrorMessage = findViewById(R.id.tvErrorMessage)
        layoutDemoCredentials = findViewById(R.id.layoutDemoCredentials)
        boxUsername = findViewById(R.id.boxUsername)
        boxPassword = findViewById(R.id.boxPassword)
    }

    private fun setupPasswordToggle() {
        btnTogglePassword.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            if (isPasswordVisible) {
                etPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                btnTogglePassword.setImageResource(R.drawable.ic_visibility_off)
            } else {
                etPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                btnTogglePassword.setImageResource(R.drawable.ic_visibility)
            }
            // Preserve cursor position at end of text
            etPassword.setSelection(etPassword.text.length)
        }
    }

    private fun setupDemoAutofill() {
        layoutDemoCredentials.setOnClickListener {
            etUsername.setText(UserDatabaseHelper.DEFAULT_USERNAME)
            etPassword.setText(UserDatabaseHelper.DEFAULT_PASSWORD)
            hideError()
            Toast.makeText(this, "Credentials filled from database default!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupLoginButton() {
        btnLogin.setOnClickListener {
            performLogin()
        }
    }

    private fun performLogin() {
        val username = etUsername.text.toString().trim()
        val password = etPassword.text.toString().trim()

        hideError()

        // Input verification
        if (username.isEmpty()) {
            showError("Please enter your username.", boxUsername)
            etUsername.requestFocus()
            return
        }

        if (password.isEmpty()) {
            showError("Please enter your password.", boxPassword)
            etPassword.requestFocus()
            return
        }

        // Show loading state
        setLoading(true)

        // Verify credentials in background thread
        Thread {
            val dbHelper = UserDatabaseHelper.getInstance(this)
            val user = dbHelper.verifyCredentials(username, password)

            // Simulate slight natural verification delay for smooth UX
            Thread.sleep(300)

            mainHandler.post {
                setLoading(false)
                if (user != null) {
                    // Success: Save session and proceed
                    val rememberMe = cbRememberMe.isChecked
                    SessionManager.createLoginSession(this, user, rememberMe)

                    Toast.makeText(
                        this,
                        "Welcome, ${user.fullName.ifBlank { user.username }}!",
                        Toast.LENGTH_SHORT
                    ).show()

                    navigateToDashboard()
                } else {
                    // Failure: Display error feedback
                    showError("Invalid username or password. Please verify your credentials.", findViewById(R.id.cardLogin))
                }
            }
        }.start()
    }

    private fun setLoading(isLoading: Boolean) {
        if (isLoading) {
            btnLogin.text = ""
            btnLogin.isEnabled = false
            progressBarLogin.visibility = View.VISIBLE
        } else {
            btnLogin.text = "Sign In to TransBuddy"
            btnLogin.isEnabled = true
            progressBarLogin.visibility = View.GONE
        }
    }

    private fun showError(message: String, targetToShake: View? = null) {
        tvErrorMessage.text = message
        layoutError.visibility = View.VISIBLE

        targetToShake?.let {
            val shake = ObjectAnimator.ofFloat(it, "translationX", 0f, 15f, -15f, 10f, -10f, 5f, -5f, 0f)
            shake.duration = 450
            shake.interpolator = CycleInterpolator(1f)
            shake.start()
        }
    }

    private fun hideError() {
        layoutError.visibility = View.GONE
    }

    private fun navigateToDashboard() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }
}
