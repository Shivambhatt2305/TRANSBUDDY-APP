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
import android.widget.TextView
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
    private lateinit var tvActivePenaltiesCount: TextView
    private lateinit var tvTotalFines: TextView
    private lateinit var penaltyAdapter: PenaltyAdapter

    // Completely empty list - NO mock data!
    private val penaltyData = mutableListOf<Penalty>()

    // Clean Driver list from driver_detail database table
    private val driverOptions = listOf(
        "Select Driver...",
        "MR. SUDHIRBHAI BATUKBHAI BHUTA",
        "MR. MOSIN AJIJBHAI SANDHVANI",
        "MR. ASGAR OSMANBHAI RAUMA",
        "MR. NARESH KANTILAL CHAUA",
        "MR. HUSENBHAI SUMARBHAI RAUMA",
        "MR. KARAN MANUBHAI BASIYA",
        "MR. PARSHOTAMBHAI RAVJIBHAI TADHANI",
        "MR. KALPESHBHAI AMARASHIBHAI DADUKIYA",
        "MR. LALJI PARBAT KARENA",
        "MR. DHARMENDRABHAI CHHAGANBHAI RATHOD",
        "MR. ASHISH RAJNIKANT TRIVEDI",
        "MR. HASAMBHAI OSAMANBHAI KAJI",
        "MR. JITENDRABHAI LAKSHMANBHAI RATHOD"
    )

    private val infractionOptions = listOf(
        "Select category...",
        "Over-Speeding In Campus Zone",
        "Unauthorized Route Deviation",
        "Engine Idling Exceeded",
        "Seatbelt & Safety Gear Violation",
        "Late Station Arrival"
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
        updateStats()
    }

    // ─── View binding ──────────────────────────────────────────
    private fun bindViews() {
        drawerLayout            = findViewById(R.id.penaltiesDrawerLayout)
        navigationView          = findViewById(R.id.penaltiesNavigationView)
        toolbar                 = findViewById(R.id.penaltiesToolbar)
        etSearch                = findViewById(R.id.etPenaltySearch)
        spinnerDriver           = findViewById(R.id.spinnerDriver)
        spinnerInfraction       = findViewById(R.id.spinnerInfraction)
        etFineAmount            = findViewById(R.id.etFineAmount)
        etNotes                 = findViewById(R.id.etPenaltyNotes)
        recyclerViewPenalties   = findViewById(R.id.recyclerViewPenalties)
        tvActivePenaltiesCount  = findViewById(R.id.tvActivePenaltiesCount)
        tvTotalFines            = findViewById(R.id.tvTotalFines)
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
        // Driver spinner with custom dropdown item layout
        val driverAdapter = ArrayAdapter(
            this, R.layout.item_spinner_dropdown, driverOptions
        ).also { it.setDropDownViewResource(R.layout.item_spinner_dropdown) }
        spinnerDriver.adapter = driverAdapter

        // Infraction type spinner with custom dropdown item layout
        val infractionAdapter = ArrayAdapter(
            this, R.layout.item_spinner_dropdown, infractionOptions
        ).also { it.setDropDownViewResource(R.layout.item_spinner_dropdown) }
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
        findViewById<android.view.View>(R.id.btnIssuePenalty).setOnClickListener {
            val driverIdx     = spinnerDriver.selectedItemPosition
            val infractionIdx = spinnerInfraction.selectedItemPosition
            val amount        = etFineAmount.text.toString().trim()

            if (driverIdx == 0) {
                Toast.makeText(this, "Please select a driver from driver_detail list.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (infractionIdx == 0) {
                Toast.makeText(this, "Please select an infraction category.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (amount.isBlank()) {
                Toast.makeText(this, "Please enter fine amount in Rupees (₹).", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val driver     = driverOptions[driverIdx]
            val infraction = infractionOptions[infractionIdx]
            val formattedAmount = if (amount.startsWith("₹")) amount else "₹$amount"

            val newPenalty = Penalty(
                iconType = "speeding",
                title = infraction,
                driverInfo = driver,
                amount = formattedAmount,
                isError = true
            )
            penaltyData.add(0, newPenalty)
            penaltyAdapter.filter(etSearch.text.toString(), penaltyData)
            updateStats()

            Toast.makeText(
                this,
                "✓ Driver Penalty Issued: $infraction for $driver — $formattedAmount",
                Toast.LENGTH_LONG
            ).show()

            // Reset form
            spinnerDriver.setSelection(0)
            spinnerInfraction.setSelection(0)
            etFineAmount.text.clear()
            etNotes.text.clear()
        }
    }

    private fun updateStats() {
        tvActivePenaltiesCount.text = penaltyData.size.toString()
        var sum = 0
        for (p in penaltyData) {
            val digits = p.amount.replace("[^0-9]".toRegex(), "")
            if (digits.isNotEmpty()) {
                sum += digits.toInt()
            }
        }
        tvTotalFines.text = "₹$sum"
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
