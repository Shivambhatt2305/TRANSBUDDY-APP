package com.transbuddy.app.controllers

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.cardview.widget.CardView
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
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
 *  - Filter button (stub — ready for bottom-sheet filter dialog)
 *  - KPI stat cards: Critical Alerts | Unpaid Fees | Invalid Scans
 *  - Recent Alerts RecyclerView with:
 *      · Review button → marks item resolved in-place (markResolved)
 *      · Resolved items show strikethrough + "Resolved" chip
 *  - Navigation Drawer (Violations checked) + Bottom Nav (Alerts active)
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

    private val alertData = listOf(
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
    }

    // ─── View binding ──────────────────────────────────────────
    private fun bindViews() {
        drawerLayout          = findViewById(R.id.violationsDrawerLayout)
        navigationView        = findViewById(R.id.violationsNavigationView)
        toolbar               = findViewById(R.id.violationsToolbar)
        etSearch              = findViewById(R.id.etViolationSearch)
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
                R.id.drawer_violations -> { /* already here */ }
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }
        navigationView.setCheckedItem(R.id.drawer_violations)
    }

    // ─── Recent Alerts RecyclerView ────────────────────────────
    private fun setupRecyclerView() {
        violationAdapter = ViolationAdapter(alertData) { alert, position ->
            // Review button tapped → mark as resolved in-place
            violationAdapter.markResolved(position)
            Toast.makeText(
                this,
                "✓ \"${alert.title}\" marked as resolved.",
                Toast.LENGTH_SHORT
            ).show()
        }
        recyclerViewViolations.apply {
            layoutManager = LinearLayoutManager(this@ViolationsActivity)
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

    // ─── Filter button (stub) ──────────────────────────────────
    private fun setupFilterButton() {
        findViewById<CardView>(R.id.btnFilter).setOnClickListener {
            // TODO: show BottomSheetDialog with filter options (type, status, time range)
            Toast.makeText(this, "Filter options — coming soon.", Toast.LENGTH_SHORT).show()
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
