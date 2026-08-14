package com.transbuddy.app.controllers

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.navigation.NavigationView
import com.transbuddy.app.R
import com.transbuddy.app.adapters.FuelLogAdapter
import com.transbuddy.app.models.FuelLog

/**
 * FuelLogsActivity — CONTROLLER (MVC)
 *
 * Manages the Fuel & KM Logs screen:
 *  - Fuel Details form (station, liters, cost, receipt scan)
 *  - Odometer Reading form (start KM, end KM)
 *  - Save Log Entry button
 *  - Recent Entries RecyclerView
 *  - Navigation Drawer + Bottom Nav Bar (Logs tab active)
 */
class FuelLogsActivity : AppCompatActivity() {

    // ─── View references ───────────────────────────────────────
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var toolbar: Toolbar
    private lateinit var recyclerViewFuelLogs: RecyclerView

    // ─── Adapter + Data ────────────────────────────────────────
    private lateinit var fuelLogAdapter: FuelLogAdapter

    private val recentLogs = listOf(
        FuelLog("BP Station N4",        "Today, 08:30 AM",      45.2,  82.40,  320, isRecent = true),
        FuelLog("Chevron City Center",  "Yesterday, 17:15 PM",  38.0,  65.00,  215),
        FuelLog("Shell Highway 1",      "Oct 12, 09:00 AM",     50.5,  91.20,  410)
    )

    // ─── Lifecycle ─────────────────────────────────────────────
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fuel_logs)

        bindViews()
        setupToolbarAndDrawer()
        setupRecyclerView()
        setupSaveButton()
        setupReceiptButton()
    }

    // ─── View binding ──────────────────────────────────────────
    private fun bindViews() {
        drawerLayout        = findViewById(R.id.fuelDrawerLayout)
        navigationView      = findViewById(R.id.fuelNavigationView)
        toolbar             = findViewById(R.id.fuelToolbar)
        recyclerViewFuelLogs = findViewById(R.id.recyclerViewFuelLogs)
    }

    // ─── Toolbar & Navigation Drawer ───────────────────────────
    private fun setupToolbarAndDrawer() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        val toggle = ActionBarDrawerToggle(
            this, drawerLayout,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        // Hamburger button in layout
        findViewById<android.widget.ImageButton>(R.id.fuelBtnMenu).setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        // Drawer item navigation
        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.drawer_home_search -> {
                    startActivity(Intent(this, HomeSearchActivity::class.java))
                    finish()
                }
                R.id.drawer_dashboard -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                }
                R.id.drawer_fuel -> { /* already here */ }
                R.id.drawer_penalties -> {
                    startActivity(Intent(this, PenaltiesActivity::class.java))
                    finish()
                }
                R.id.drawer_operations -> {
                    startActivity(Intent(this, OperationsActivity::class.java))
                    finish()
                }
                R.id.drawer_violations -> {
                    startActivity(Intent(this, ViolationsActivity::class.java))
                    finish()
                }
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }
        // Mark Fuel Logs as checked in the drawer
        navigationView.setCheckedItem(R.id.drawer_fuel)
    }

    // ─── Recent Entries RecyclerView ───────────────────────────
    private fun setupRecyclerView() {
        fuelLogAdapter = FuelLogAdapter(recentLogs)
        recyclerViewFuelLogs.apply {
            layoutManager = LinearLayoutManager(this@FuelLogsActivity)
            adapter = fuelLogAdapter
            isNestedScrollingEnabled = false
        }
    }

    // ─── Save Log Entry button ─────────────────────────────────
    private fun setupSaveButton() {
        findViewById<androidx.cardview.widget.CardView>(R.id.btnSaveLog).setOnClickListener {
            val station  = findViewById<android.widget.EditText>(R.id.etStation).text.toString()
            val liters   = findViewById<android.widget.EditText>(R.id.etLiters).text.toString()
            val cost     = findViewById<android.widget.EditText>(R.id.etCost).text.toString()
            val startKm  = findViewById<android.widget.EditText>(R.id.etStartKm).text.toString()
            val endKm    = findViewById<android.widget.EditText>(R.id.etEndKm).text.toString()

            if (station.isBlank() || liters.isBlank() || cost.isBlank() || endKm.isBlank()) {
                Toast.makeText(this, "Please fill in all required fields.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // TODO: Persist to database / send to API
            Toast.makeText(this, "✓ Log entry saved!", Toast.LENGTH_SHORT).show()
        }
    }

    // ─── Receipt Scan button ───────────────────────────────────
    private fun setupReceiptButton() {
        findViewById<androidx.cardview.widget.CardView>(R.id.btnScanReceipt).setOnClickListener {
            // TODO: Launch camera / image picker intent
            Toast.makeText(this, "Camera feature coming soon.", Toast.LENGTH_SHORT).show()
        }
    }

    // ─── Back press: close drawer first if open ────────────────
    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
}
