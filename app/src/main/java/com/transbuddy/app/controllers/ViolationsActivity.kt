package com.transbuddy.app.controllers

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.cardview.widget.CardView
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.navigation.NavigationView
import com.transbuddy.app.R
import com.transbuddy.app.adapters.ViolationAdapter
import com.transbuddy.app.models.ViolationAlert

/**
 * ViolationsActivity — CONTROLLER (MVC)
 *
 * Manages the Violations & Fee Alerts screen:
 *  - Search bar filters the alerts list in real-time
 *  - Interactive filter dialog (All, Active, Resolved, Unauthorized, Unpaid, Invalid Scan)
 *  - Dynamic KPI stat cards: Critical Alerts | Unpaid Fees | Invalid Scans
 *  - Recent Alerts RecyclerView with review & resolve actions
 *  - Responsive grid layout for tablets and phones
 *  - Navigation Drawer
 */
class ViolationsActivity : AppCompatActivity() {

    // ─── View references ───────────────────────────────────────
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var toolbar: Toolbar
    private lateinit var etSearch: EditText
    private lateinit var recyclerViewViolations: RecyclerView

    // ─── Adapter + Data ────────────────────────────────────────
    private lateinit var violationAdapter: ViolationAdapter

    private val alertData = mutableListOf(
        ViolationAlert(
            alertType  = "unauthorized",
            title      = "Unauthorized Rider",
            busInfo    = "Bus ID: TX-8492 • Route 15",
            time       = "10:42 AM",
            isResolved = false
        ),
        ViolationAlert(
            alertType  = "unpaid",
            title      = "Unpaid Fare Detected",
            busInfo    = "Bus ID: TX-1102 • Express Line",
            time       = "10:15 AM",
            isResolved = false
        ),
        ViolationAlert(
            alertType  = "invalid_scan",
            title      = "Invalid ID Scan",
            busInfo    = "Bus ID: TX-3321 • Route 42",
            time       = "09:30 AM",
            isResolved = true
        )
    )

    // ─── Lifecycle ─────────────────────────────────────────────
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_violations)

        bindViews()
        setupToolbarAndDrawer()
        setupRecyclerView()
        setupSearch()
        setupFilterButton()
        updateKpiCounts()
    }

    // ─── View binding ──────────────────────────────────────────
    private fun bindViews() {
        drawerLayout           = findViewById(R.id.violationsDrawerLayout)
        navigationView         = findViewById(R.id.violationsNavigationView)
        toolbar                = findViewById(R.id.violationsToolbar)
        etSearch               = findViewById(R.id.etViolationSearch)
        recyclerViewViolations = findViewById(R.id.recyclerViewViolations)
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

        findViewById<android.widget.ImageButton>(R.id.violationsBtnMenu).setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.drawer_home_search -> {
                    startActivity(Intent(this, HomeSearchActivity::class.java)); finish()
                }
                R.id.drawer_dashboard -> {
                    startActivity(Intent(this, MainActivity::class.java)); finish()
                }
                R.id.drawer_fuel -> {
                    startActivity(Intent(this, FuelLogsActivity::class.java)); finish()
                }
                R.id.drawer_penalties -> {
                    startActivity(Intent(this, PenaltiesActivity::class.java)); finish()
                }
                R.id.drawer_operations -> {
                    startActivity(Intent(this, OperationsActivity::class.java)); finish()
                }
                R.id.drawer_emergency -> {
                    startActivity(Intent(this, EmergencyNotificationsActivity::class.java)); finish()
                }
                R.id.drawer_logout -> {
                    com.transbuddy.app.utils.SessionManager.getInstance(this).logout(this)
                }
                R.id.drawer_violations -> { /* already here */ }
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }
        navigationView.setCheckedItem(R.id.drawer_violations)
    }

    // ─── Dynamic KPI calculations ──────────────────────────────
    private fun updateKpiCounts() {
        val critical = alertData.count { !it.isResolved }
        val invalid  = alertData.count { it.alertType == "invalid_scan" }
        findViewById<TextView>(R.id.tvCriticalCount)?.text = critical.toString()
        findViewById<TextView>(R.id.tvInvalidScanCount)?.text = invalid.toString()
    }

    // ─── Recent Alerts RecyclerView ────────────────────────────
    private fun setupRecyclerView() {
        violationAdapter = ViolationAdapter(alertData) { alert, position ->
            // Review button tapped → mark as resolved in-place
            val indexInMaster = alertData.indexOfFirst { it.title == alert.title && it.time == alert.time }
            if (indexInMaster != -1) {
                alertData[indexInMaster] = alertData[indexInMaster].copy(isResolved = true)
            }
            violationAdapter.markResolved(position)
            updateKpiCounts()
            Toast.makeText(
                this,
                "✓ \"${alert.title}\" marked as resolved.",
                Toast.LENGTH_SHORT
            ).show()
        }
        val columns = resources.getInteger(R.integer.search_grid_columns)
        recyclerViewViolations.apply {
            layoutManager = GridLayoutManager(this@ViolationsActivity, columns)
            adapter = violationAdapter
            isNestedScrollingEnabled = false
        }
    }

    // ─── Search bar filtering ──────────────────────────────────
    private fun setupSearch() {
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                violationAdapter.filter(s.toString(), alertData)
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    // ─── Filter dialog ─────────────────────────────────────────
    private fun setupFilterButton() {
        findViewById<CardView>(R.id.btnFilter).setOnClickListener {
            val options = arrayOf(
                "All Violations",
                "Active / Unresolved Only",
                "Resolved Only",
                "Unauthorized Riders",
                "Unpaid Fares",
                "Invalid ID Scans"
            )
            AlertDialog.Builder(this)
                .setTitle("Filter Alerts")
                .setItems(options) { _, which ->
                    val filtered = when (which) {
                        1 -> alertData.filter { !it.isResolved }
                        2 -> alertData.filter { it.isResolved }
                        3 -> alertData.filter { it.alertType == "unauthorized" }
                        4 -> alertData.filter { it.alertType == "unpaid" }
                        5 -> alertData.filter { it.alertType == "invalid_scan" }
                        else -> alertData
                    }
                    violationAdapter.updateData(filtered)
                    Toast.makeText(this, "Filter: ${options[which]} (${filtered.size} items)", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Close", null)
                .show()
        }
    }

    // ─── Back press ────────────────────────────────────────────
    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
}
