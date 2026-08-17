package com.transbuddy.app.controllers

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * FuelLogsActivity — CONTROLLER (MVC)
 *
 * Manages the Fuel & KM Logs screen:
 *  - Refuel Information (Station, Fuel Type Dropdown [Diesel, CNG, Petrol], Fuel Price, Volume, Cost)
 *  - Real-time automatic calculation of Total Cost = Volume * Price per Unit
 *  - Single Current Odometer Reading (KM)
 *  - Dynamic, clean list display of user-added fuel entries (NO mock data!)
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
    private lateinit var spinnerFuelType: Spinner
    private lateinit var etLiters: EditText
    private lateinit var etFuelPrice: EditText
    private lateinit var etCost: EditText
    private lateinit var etStation: EditText
    private lateinit var etCurrentKm: EditText

    // Fuel Type Options
    private val fuelTypeOptions = listOf(
        "Select Fuel Type...",
        "Diesel",
        "CNG",
        "Petrol"
    )

    // ─── Adapter + Dynamic Data (Empty start - NO mock data!) ─
    private lateinit var fuelLogAdapter: FuelLogAdapter
    private val fuelLogData = mutableListOf<FuelLog>()

    // ─── Lifecycle ─────────────────────────────────────────────
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fuel_logs)

        bindViews()
        setupToolbarAndDrawer()
        setupFuelTypeSpinner()
        setupAutoCostCalculation()
        setupRecyclerView()
        setupSaveButton()
        setupReceiptButton()
    }

    // ─── View binding ──────────────────────────────────────────
    private fun bindViews() {
        drawerLayout         = findViewById(R.id.fuelDrawerLayout)
        navigationView       = findViewById(R.id.fuelNavigationView)
        toolbar              = findViewById(R.id.fuelToolbar)
        recyclerViewFuelLogs = findViewById(R.id.recyclerViewFuelLogs)
        spinnerFuelType      = findViewById(R.id.spinnerFuelType)
        etStation            = findViewById(R.id.etStation)
        etLiters             = findViewById(R.id.etLiters)
        etFuelPrice          = findViewById(R.id.etFuelPrice)
        etCost               = findViewById(R.id.etCost)
        etCurrentKm          = findViewById(R.id.etCurrentKm)
    }

    // ─── Fuel Type Spinner ─────────────────────────────────────
    private fun setupFuelTypeSpinner() {
        val adapter = ArrayAdapter(
            this, R.layout.item_spinner_dropdown, fuelTypeOptions
        ).also { it.setDropDownViewResource(R.layout.item_spinner_dropdown) }
        spinnerFuelType.adapter = adapter
    }

    // ─── Automatic Cost Calculation ───────────────────────────
    private fun setupAutoCostCalculation() {
        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                calculateTotalCost()
            }
            override fun afterTextChanged(s: Editable?) {}
        }
        etLiters.addTextChangedListener(watcher)
        etFuelPrice.addTextChangedListener(watcher)
    }

    private fun calculateTotalCost() {
        val litersStr = etLiters.text.toString().trim()
        val priceStr  = etFuelPrice.text.toString().trim()

        if (litersStr.isNotEmpty() && priceStr.isNotEmpty()) {
            try {
                val liters = litersStr.toDouble()
                val price  = priceStr.toDouble()
                val total  = liters * price
                etCost.setText(String.format(Locale.US, "%.2f", total))
            } catch (e: NumberFormatException) {
                // Ignore parse errors while typing
            }
        }
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
                R.id.drawer_emergency -> {
                    startActivity(Intent(this, EmergencyNotificationsActivity::class.java))
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
        fuelLogAdapter = FuelLogAdapter(fuelLogData)
        recyclerViewFuelLogs.apply {
            layoutManager = LinearLayoutManager(this@FuelLogsActivity)
            adapter = fuelLogAdapter
            isNestedScrollingEnabled = false
        }
    }

    // ─── Save Log Entry button ─────────────────────────────────
    private fun setupSaveButton() {
        findViewById<android.view.View>(R.id.btnSaveLog).setOnClickListener {
            val station     = etStation.text.toString().trim()
            val fuelTypeIdx = spinnerFuelType.selectedItemPosition
            val liters      = etLiters.text.toString().trim()
            val price       = etFuelPrice.text.toString().trim()
            val cost        = etCost.text.toString().trim()
            val currentKm   = etCurrentKm.text.toString().trim()

            if (station.isBlank()) {
                Toast.makeText(this, "Please enter station name / location.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (fuelTypeIdx == 0) {
                Toast.makeText(this, "Please select a fuel type (Diesel, CNG, or Petrol).", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (liters.isBlank() || price.isBlank() || cost.isBlank() || currentKm.isBlank()) {
                Toast.makeText(this, "Please fill in all required fields.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val selectedFuel = fuelTypeOptions[fuelTypeIdx]
            val timestamp    = SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault()).format(Date())

            val newLog = FuelLog(
                stationName = "$station ($selectedFuel)",
                timestamp = timestamp,
                liters = liters.toDoubleOrNull() ?: 0.0,
                totalCost = cost.toDoubleOrNull() ?: 0.0,
                tripKm = currentKm.toIntOrNull() ?: 0,
                isRecent = true
            )

            // Add user log entry dynamically
            fuelLogData.add(0, newLog)
            fuelLogAdapter.updateData(fuelLogData.toList())

            Toast.makeText(
                this,
                "✓ Fuel Log Saved: $selectedFuel at ₹$price/unit — Total: ₹$cost",
                Toast.LENGTH_LONG
            ).show()

            // Reset form
            etStation.text.clear()
            spinnerFuelType.setSelection(0)
            etLiters.text.clear()
            etFuelPrice.text.clear()
            etCost.text.clear()
            etCurrentKm.text.clear()
        }
    }

    // ─── Receipt Scan button ───────────────────────────────────
    private fun setupReceiptButton() {
        findViewById<android.view.View>(R.id.btnScanReceipt).setOnClickListener {
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
