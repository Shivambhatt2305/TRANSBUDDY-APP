package com.transbuddy.app.controllers

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.AdapterView
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
import com.transbuddy.app.adapters.PenaltyAdapter
import com.transbuddy.app.models.Penalty

/**
 * PenaltiesActivity — CONTROLLER (MVC)
 *
 * Manages the Penalties & Fines screen:
 *  - Search bar filtering the RecyclerView
 *  - Stats row (active count + total fines)
 *  - Assign Penalty form with spinner-driven dropdowns, amount input, notes
 *  - "Issue Penalty" action with validation + confirmation toast
 *  - Recent Disciplinary Actions RecyclerView
 *  - Navigation Drawer + Bottom Nav Bar (Admin tab active)
 */
class PenaltiesActivity : AppCompatActivity() {

    // ─── View references ───────────────────────────────────────
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var toolbar: Toolbar
    private lateinit var etSearch: EditText
    private lateinit var spinnerDriver: Spinner
    private lateinit var spinnerInfraction: Spinner
    private lateinit var etFineAmount: EditText
    private lateinit var etNotes: EditText
    private lateinit var recyclerViewPenalties: RecyclerView

    // ─── Adapter + Data ────────────────────────────────────────
    private lateinit var penaltyAdapter: PenaltyAdapter

    private val penaltyData = listOf(
        Penalty(
            iconType   = "speeding",
            title      = "Speeding Violation",
            driverInfo = "D-1042: Michael Chang • Oct 24, 2023",
            amount     = "$150.00",
            isError    = true
        ),
        Penalty(
            iconType   = "route",
            title      = "Route Deviation",
            driverInfo = "D-8831: Sarah Jenkins • Oct 22, 2023",
            amount     = "$50.00",
            isError    = false
        ),
        Penalty(
            iconType   = "idle",
            title      = "Idle Time Exceeded",
            driverInfo = "D-2219: Robert Cole • Oct 20, 2023",
            amount     = "Warning",
            isError    = false
        )
    )

    // Spinner option lists — matching HTML <option> values
    private val driverOptions = listOf(
        "Select an entity...",
        "D-1042: Michael Chang (Bus A)",
        "D-8831: Sarah Jenkins (Van 3)",
        "D-2219: Robert Cole (Bus C)"
    )

    private val infractionOptions = listOf(
        "Select category...",
        "Speeding Violation",
        "Route Deviation",
        "Idle Time Exceeded",
        "Safety Gear Missing"
    )

    // ─── Lifecycle ─────────────────────────────────────────────
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_penalties)

        bindViews()
        setupToolbarAndDrawer()
        setupSpinners()
        setupRecyclerView()
        setupSearch()
        setupIssuePenaltyButton()
    }

    // ─── View binding ──────────────────────────────────────────
    private fun bindViews() {
        drawerLayout         = findViewById(R.id.penaltiesDrawerLayout)
        navigationView       = findViewById(R.id.penaltiesNavigationView)
        toolbar              = findViewById(R.id.penaltiesToolbar)
        etSearch             = findViewById(R.id.etPenaltySearch)
        spinnerDriver        = findViewById(R.id.spinnerDriver)
        spinnerInfraction    = findViewById(R.id.spinnerInfraction)
        etFineAmount         = findViewById(R.id.etFineAmount)
        etNotes              = findViewById(R.id.etPenaltyNotes)
        recyclerViewPenalties = findViewById(R.id.recyclerViewPenalties)
    }

    // ─── Toolbar & Drawer ──────────────────────────────────────
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

        findViewById<android.widget.ImageButton>(R.id.penaltiesBtnMenu).setOnClickListener {
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
                R.id.drawer_fuel -> {
                    startActivity(Intent(this, FuelLogsActivity::class.java))
                    finish()
                }
                R.id.drawer_penalties -> { /* already here */ }
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
        navigationView.setCheckedItem(R.id.drawer_penalties)
    }

    // ─── Spinners ──────────────────────────────────────────────
    private fun setupSpinners() {
        // Driver spinner
        val driverAdapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_item, driverOptions
        ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
        spinnerDriver.adapter = driverAdapter

        // Infraction type spinner
        val infractionAdapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_item, infractionOptions
        ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
        spinnerInfraction.adapter = infractionAdapter
    }

    // ─── RecyclerView ──────────────────────────────────────────
    private fun setupRecyclerView() {
        penaltyAdapter = PenaltyAdapter(penaltyData) { penalty ->
            Toast.makeText(this, "Options for: ${penalty.title}", Toast.LENGTH_SHORT).show()
        }
        recyclerViewPenalties.apply {
            layoutManager = LinearLayoutManager(this@PenaltiesActivity)
            adapter = penaltyAdapter
            isNestedScrollingEnabled = false
        }
    }

    // ─── Search bar ────────────────────────────────────────────
    private fun setupSearch() {
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                penaltyAdapter.filter(s.toString(), penaltyData)
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    // ─── Issue Penalty button ──────────────────────────────────
    private fun setupIssuePenaltyButton() {
        findViewById<androidx.cardview.widget.CardView>(R.id.btnIssuePenalty).setOnClickListener {
            val driverIdx     = spinnerDriver.selectedItemPosition
            val infractionIdx = spinnerInfraction.selectedItemPosition
            val amount        = etFineAmount.text.toString().trim()

            if (driverIdx == 0) {
                Toast.makeText(this, "Please select a driver/vehicle.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (infractionIdx == 0) {
                Toast.makeText(this, "Please select an infraction type.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (amount.isBlank()) {
                Toast.makeText(this, "Please enter a fine amount.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val driver     = driverOptions[driverIdx]
            val infraction = infractionOptions[infractionIdx]
            // TODO: persist to database / API
            Toast.makeText(
                this,
                "✓ Penalty issued: $infraction for $driver — $$amount",
                Toast.LENGTH_LONG
            ).show()

            // Reset form
            spinnerDriver.setSelection(0)
            spinnerInfraction.setSelection(0)
            etFineAmount.text.clear()
            etNotes.text.clear()
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
