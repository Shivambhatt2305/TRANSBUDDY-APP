package com.transbuddy.app.controllers

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowInsetsController
import android.view.animation.DecelerateInterpolator
import androidx.appcompat.app.AppCompatActivity
import com.transbuddy.app.R
import com.transbuddy.app.utils.EmergencyDatabaseHelper
import com.transbuddy.app.utils.FuelLogDatabaseHelper
import com.transbuddy.app.utils.SessionManager
import com.transbuddy.app.utils.UserDatabaseHelper

/**
 * SplashActivity — Official Marwadi University Entry Point
 * Displays the Marwadi University logo on a clean white canvas matching Splash.png,
 * pre-warms database services, and routes seamlessly to Dashboard or Login.
 */
class SplashActivity : AppCompatActivity() {

    private lateinit var layoutSplashBrand: View
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        setupSystemBars()

        layoutSplashBrand = findViewById(R.id.layoutSplashBrand)

        startBrandingAnimations()
        prewarmDatabasesAndNavigate()
    }

    private fun setupSystemBars() {
        try {
            window.statusBarColor = Color.WHITE
            window.navigationBarColor = Color.WHITE

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                window.insetsController?.setSystemBarsAppearance(
                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS,
                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
                )
            } else {
                @Suppress("DEPRECATION")
                var flags = window.decorView.systemUiVisibility
                @Suppress("DEPRECATION")
                flags = flags or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    @Suppress("DEPRECATION")
                    flags = flags or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
                }
                @Suppress("DEPRECATION")
                window.decorView.systemUiVisibility = flags
            }
        } catch (e: Exception) {
            android.util.Log.e("SplashActivity", "System bars styling error: ${e.message}")
        }
    }

    private fun startBrandingAnimations() {
        layoutSplashBrand.alpha = 0f
        layoutSplashBrand.scaleX = 0.92f
        layoutSplashBrand.scaleY = 0.92f

        val fadeIn = ObjectAnimator.ofFloat(layoutSplashBrand, View.ALPHA, 0f, 1f)
        val scaleX = ObjectAnimator.ofFloat(layoutSplashBrand, View.SCALE_X, 0.92f, 1f)
        val scaleY = ObjectAnimator.ofFloat(layoutSplashBrand, View.SCALE_Y, 0.92f, 1f)

        AnimatorSet().apply {
            duration = 850
            interpolator = DecelerateInterpolator()
            playTogether(fadeIn, scaleX, scaleY)
            start()
        }
    }

    private fun prewarmDatabasesAndNavigate() {
        Thread {
            try {
                UserDatabaseHelper.getInstance(this).seedDefaultUserIfMissing()
                FuelLogDatabaseHelper.getInstance(this).getAllLogs()
                EmergencyDatabaseHelper.getInstance(this).getAllEmergencies()
            } catch (e: Exception) {
                android.util.Log.e("SplashActivity", "Error during DB pre-warm: ${e.message}")
            }
        }.start()

        mainHandler.postDelayed({
            if (!isFinishing && !isDestroyed) {
                routeToDestination()
            }
        }, 1900)
    }

    private fun routeToDestination() {
        val sessionManager = SessionManager.getInstance(this)
        val targetClass = if (sessionManager.isLoggedIn()) {
            MainActivity::class.java
        } else {
            LoginActivity::class.java
        }

        val intent = Intent(this, targetClass).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        mainHandler.removeCallbacksAndMessages(null)
    }
}
