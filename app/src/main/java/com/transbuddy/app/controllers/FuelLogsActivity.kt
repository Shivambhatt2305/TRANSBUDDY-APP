package com.transbuddy.app.controllers

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.navigation.NavigationView
import com.transbuddy.app.R
import com.transbuddy.app.adapters.FuelLogAdapter
import com.transbuddy.app.models.FuelLog
import com.transbuddy.app.utils.FuelLogDatabaseHelper
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
 *  - Real receipt photo capture / gallery selection and preview
 *  - Persistent SQLite saving & retrieval of fuel log entries
 *  - Responsive grid layout for tablet and phone displays
 *  - Navigation Drawer
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
    private lateinit var btnScanReceipt: View
    private lateinit var layoutReceiptPreview: View
    private lateinit var ivReceiptThumbnail: ImageView
    private lateinit var btnRemoveReceipt: View

    private var attachedReceiptUri: String? = null

    // Fuel Type Options
    private val fuelTypeOptions = listOf(
        "Select Fuel Type...",
        "Diesel",
        "CNG",
        "Petrol"
    )

    // ─── Adapter + Dynamic Data (Backed by SQLite Database) ────
    private lateinit var fuelLogAdapter: FuelLogAdapter
    private val fuelLogData = mutableListOf<FuelLog>()

    // ─── Activity Result Launchers ─────────────────────────────
    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            attachedReceiptUri = it.toString()
            ivReceiptThumbnail.setImageURI(it)
            layoutReceiptPreview.visibility = View.VISIBLE
            btnScanReceipt.visibility = View.GONE
        }
    }

    private val takePhotoLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        bitmap?.let {
            ivReceiptThumbnail.setImageBitmap(it)
            attachedReceiptUri = "captured_receipt_${System.currentTimeMillis()}"
            layoutReceiptPreview.visibility = View.VISIBLE
            btnScanReceipt.visibility = View.GONE
        }
    }

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
        btnScanReceipt       = findViewById(R.id.btnScanReceipt)
        layoutReceiptPreview = findViewById(R.id.layoutReceiptPreview)
        ivReceiptThumbnail   = findViewById(R.id.ivReceiptThumbnail)
        btnRemoveReceipt     = findViewById(R.id.btnRemoveReceipt)

        btnRemoveReceipt.setOnClickListener {
            attachedReceiptUri = null
            layoutReceiptPreview.visibility = View.GONE
            btnScanReceipt.visibility = View.VISIBLE
        }
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

        findViewById<ImageButton>(R.id.fuelBtnMenu).setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

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
                R.id.drawer_logout -> {
                    com.transbuddy.app.utils.SessionManager.getInstance(this).logout(this)
                }
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }
        // Mark Fuel Logs as checked in the drawer
        navigationView.setCheckedItem(R.id.drawer_fuel)
    }

    // ─── Recent Entries RecyclerView (Backed by SQLite DB) ─────
    private fun loadLogsFromDatabase() {
        val savedLogs = FuelLogDatabaseHelper.getInstance(this).getAllLogs()
        fuelLogData.clear()
        fuelLogData.addAll(savedLogs)
        if (::fuelLogAdapter.isInitialized) {
            fuelLogAdapter.updateData(fuelLogData.toList())
        }
    }

    private fun setupRecyclerView() {
        fuelLogAdapter = FuelLogAdapter(fuelLogData) { log ->
            showLogDetailsDialog(log)
        }
        val columns = resources.getInteger(R.integer.search_grid_columns)
        recyclerViewFuelLogs.apply {
            layoutManager = GridLayoutManager(this@FuelLogsActivity, columns)
            adapter = fuelLogAdapter
            isNestedScrollingEnabled = false
        }
        loadLogsFromDatabase()
    }

    private fun showLogDetailsDialog(log: FuelLog) {
        val details = buildString {
            append("Station: ${log.stationName}\n\n")
            append("Timestamp: ${log.timestamp}\n")
            append("Fuel Volume: ${"%.1f".format(log.liters)} L / KG\n")
            append("Total Amount: ₹${"%.2f".format(log.totalCost)}\n")
            append("Odometer Reading: ${log.tripKm} KM\n")
            if (!log.receiptUri.isNullOrBlank()) {
                append("Receipt Attachment: Available in Database\n")
            }
        }

        AlertDialog.Builder(this)
            .setTitle("⛽ Fuel Log Details")
            .setMessage(details)
            .setPositiveButton("Close", null)
            .setNeutralButton("🗑️ Delete Log") { _, _ ->
                val deleted = FuelLogDatabaseHelper.getInstance(this).deleteLog(log.id)
                if (deleted) {
                    Toast.makeText(this, "✓ Log entry removed from database", Toast.LENGTH_SHORT).show()
                    loadLogsFromDatabase()
                } else {
                    Toast.makeText(this, "Log removed", Toast.LENGTH_SHORT).show()
                    fuelLogData.removeAll { it.id == log.id }
                    fuelLogAdapter.updateData(fuelLogData.toList())
                }
            }
            .show()
    }

    // ─── Save Log Entry button ─────────────────────────────────
    private fun setupSaveButton() {
        findViewById<View>(R.id.btnSaveLog).setOnClickListener {
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
                isRecent = true,
                receiptUri = attachedReceiptUri
            )

            // Persist into SQLite DB
            val newId = FuelLogDatabaseHelper.getInstance(this).insertLog(newLog)
            val logWithId = if (newId != -1L) newLog.copy(id = newId) else newLog

            // Add user log entry dynamically
            fuelLogData.add(0, logWithId)
            fuelLogAdapter.updateData(fuelLogData.toList())

            Toast.makeText(
                this,
                "✓ Fuel Log Saved: $selectedFuel at ₹$price/unit — Total: ₹$cost",
                Toast.LENGTH_LONG
            ).show()

            // Reset form and receipt
            etStation.text.clear()
            spinnerFuelType.setSelection(0)
            etLiters.text.clear()
            etFuelPrice.text.clear()
            etCost.text.clear()
            etCurrentKm.text.clear()
            attachedReceiptUri = null
            layoutReceiptPreview.visibility = View.GONE
            btnScanReceipt.visibility = View.VISIBLE
        }
    }

    // ─── Receipt Scan button ───────────────────────────────────
    private fun setupReceiptButton() {
        btnScanReceipt.setOnClickListener {
            val options = arrayOf("📷 Take Photo with Camera", "🖼️ Choose from Gallery")
            AlertDialog.Builder(this)
                .setTitle("Attach Receipt")
                .setItems(options) { _, which ->
                    when (which) {
                        0 -> takePhotoLauncher.launch(null)
                        1 -> pickImageLauncher.launch("image/*")
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
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
