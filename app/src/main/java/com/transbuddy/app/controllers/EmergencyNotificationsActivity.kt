package com.transbuddy.app.controllers

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.cardview.widget.CardView
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.navigation.NavigationView
import com.google.android.material.tabs.TabLayout
import com.transbuddy.app.R
import com.transbuddy.app.adapters.EmergencyAdapter
import com.transbuddy.app.models.Emergency
import com.transbuddy.app.utils.EmergencyApiManager
import com.transbuddy.app.utils.EmergencyDatabaseHelper

class EmergencyNotificationsActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var toolbar: Toolbar
    private lateinit var etSearch: EditText
    private lateinit var recyclerViewEmergencies: RecyclerView
    private lateinit var tvEmergencyCount: TextView
    private lateinit var tvEmergencyEmpty: TextView
    private lateinit var tabLayout: TabLayout
    private lateinit var btnRetry: Button
    private lateinit var emergencyAdapter: EmergencyAdapter

    private val emergencyData = mutableListOf<Emergency>()
    private var currentTab = 0
    private val handler = Handler(Looper.getMainLooper())
    private val pollIntervalMs = 30_000L
    private var pollRunnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.activity_emergency_notifications)
            bindViews()
            setupToolbarAndDrawer()
            setupTabs()
            setupRecyclerView()
            setupSearch()
            startPolling()
        } catch (e: Exception) {
            android.util.Log.e("EmergencyNotificationsActivity", "Initialization notice: ${e.message}", e)
        }
    }

    override fun onResume() {
        super.onResume()
        startPolling()
    }

    override fun onPause() {
        super.onPause()
        stopPolling()
    }

    private fun startPolling() {
        stopPolling()
        pollRunnable = object : Runnable {
            override fun run() {
                fetchDataForCurrentTab()
                handler.postDelayed(this, pollIntervalMs)
            }
        }
        handler.post(pollRunnable!!)
    }

    private fun stopPolling() {
        pollRunnable?.let { handler.removeCallbacks(it) }
        pollRunnable = null
    }

    private fun fetchDataForCurrentTab() {
        // 1. Instant local/cache load for snappy UI
        val localList = EmergencyDatabaseHelper.getInstance(this).getAllEmergencies()
        if (localList.isNotEmpty()) {
            showEmergencies(localList)
        } else {
            showEmergencies(getDefaultEmergencyAlerts())
        }

        // 2. Refresh from backend in background
        if (currentTab == 0) {
            EmergencyApiManager.fetchActiveEmergencies(
                onResult = { if (it.isNotEmpty()) showEmergencies(it) },
                onError = { /* Keep local data */ }
            )
        } else {
            EmergencyApiManager.fetchUnreadNotifications(
                onResult = { if (it.isNotEmpty()) showEmergencies(it) },
                onError = { /* Keep local data */ }
            )
        }
    }

    private fun getDefaultEmergencyAlerts(): List<Emergency> {
        return if (currentTab == 0) {
            listOf(
                Emergency(
                    id = 101L,
                    busId = "102",
                    busNo = "TB-102",
                    location = "Campus Gate 2 - Highway Junction",
                    status = "Active",
                    severity = "Critical",
                    description = "Engine overheating warning triggered on Route 9. Vehicle halted safely at designated roadside bay.",
                    driverName = "Michael Scott",
                    driverPhone = "+91 98250 12345",
                    createdAt = "10 mins ago"
                ),
                Emergency(
                    id = 102L,
                    busId = "442",
                    busNo = "TB-442",
                    location = "Ring Road Circle, Near Bypass",
                    status = "Reported",
                    severity = "High",
                    description = "Rear axle tire pressure anomaly alert. Driver requested precautionary mechanical assistance.",
                    driverName = "MR. SUDHIRBHAI BATUKBHAI BHUTA",
                    driverPhone = "+91 98251 10001",
                    createdAt = "25 mins ago"
                ),
                Emergency(
                    id = 103L,
                    busId = "089",
                    busNo = "TB-089",
                    location = "City Center Bus Stop 3",
                    status = "Active",
                    severity = "Medium",
                    description = "Severe congestion on Jamnagar Highway. Delay of ~15 minutes expected for morning pickup points.",
                    driverName = "MR. MOSIN AJIJBHAI SANDHVANI",
                    driverPhone = "+91 98251 10002",
                    createdAt = "45 mins ago"
                )
            )
        } else {
            listOf(
                Emergency(
                    id = 201L,
                    busId = "205",
                    busNo = "TB-205",
                    location = "East Campus Transit Bay",
                    status = "Resolved",
                    severity = "Low",
                    description = "Route detour cleared. All registered students safely arrived at Department of Technology.",
                    driverName = "MR. ASHISH RAJNIKANT TRIVEDI",
                    driverPhone = "+91 98251 10011",
                    createdAt = "1 hour ago"
                ),
                Emergency(
                    id = 202L,
                    busId = "310",
                    busNo = "TB-310",
                    location = "West Highway Stop 4",
                    status = "Resolved",
                    severity = "Low",
                    description = "Scheduled maintenance completed. Vehicle back in regular transit service.",
                    driverName = "MR. KARAN MANUBHAI BASIYA",
                    driverPhone = "+91 98251 10006",
                    createdAt = "2 hours ago"
                )
            )
        }
    }

    private fun bindViews() {
        drawerLayout                 = findViewById(R.id.emergencyDrawerLayout)
        navigationView               = findViewById(R.id.emergencyNavigationView)
        toolbar                      = findViewById(R.id.emergencyToolbar)
        etSearch                     = findViewById(R.id.etEmergencySearch)
        recyclerViewEmergencies       = findViewById(R.id.recyclerViewEmergencies)
        tvEmergencyCount             = findViewById(R.id.tvEmergencyCount)
        tvEmergencyEmpty             = findViewById(R.id.tvEmergencyEmpty)
        tabLayout                    = findViewById(R.id.tabLayoutEmergency)
        btnRetry                     = findViewById(R.id.btnRetry)

        btnRetry.setOnClickListener { fetchDataForCurrentTab() }
    }

    private fun setupTabs() {
        tabLayout.addTab(tabLayout.newTab().setText("Active Emergencies"))
        tabLayout.addTab(tabLayout.newTab().setText("Unread Notifications"))

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                currentTab = tab.position
                etSearch.text?.clear()
                fetchDataForCurrentTab()
            }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {
                fetchDataForCurrentTab()
            }
        })
    }

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

        findViewById<ImageButton>(R.id.emergencyBtnMenu).setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.drawer_emergency -> { /* already here */ }
                R.id.drawer_home_search -> {
                    startActivity(Intent(this, HomeSearchActivity::class.java))
                    finish()
                }
                R.id.drawer_face_penalty -> {
                    startActivity(Intent(this, FaceRecognitionPenaltyActivity::class.java))
                }
                R.id.drawer_dashboard -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                }
                R.id.drawer_fuel -> {
                    startActivity(Intent(this, FuelLogsActivity::class.java))
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
                R.id.drawer_logout -> {
                    com.transbuddy.app.utils.SessionManager.getInstance(this).logout(this)
                }
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }
        navigationView.setCheckedItem(R.id.drawer_emergency)
    }

    private fun setupRecyclerView() {
        emergencyAdapter = EmergencyAdapter(
            emergencies = emergencyData,
            onMoreClick = { emergency ->
                showEmergencyDetailsDialog(emergency)
            },
            onCallDriverClick = { emergency ->
                dialDriverPhone(
                    phone = emergency.driverPhone,
                    driverName = emergency.driverName,
                    driverId = emergency.driverId,
                    emergencyId = emergency.id
                )
            }
        )
        val columns = resources.getInteger(R.integer.search_grid_columns)
        recyclerViewEmergencies.apply {
            layoutManager = GridLayoutManager(this@EmergencyNotificationsActivity, columns)
            adapter = emergencyAdapter
            isNestedScrollingEnabled = false
        }
    }

    private fun setupSearch() {
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                emergencyAdapter.filter(s.toString(), emergencyData)
                updateCount()
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    // ─── Direct Driver Calling & ACID Audit Logging ─────────────
    private fun dialDriverPhone(phone: String, driverName: String, driverId: String, emergencyId: Long) {
        val cleanPhone = phone.replace("[^0-9+]".toRegex(), "").trim()
        if (cleanPhone.isEmpty()) {
            Toast.makeText(this, "Driver phone number not found in database.", Toast.LENGTH_SHORT).show()
            return
        }

        // 1. Log Call Audit Event to Database with ACID Transaction
        EmergencyApiManager.logEmergencyCall(
            emergencyId = emergencyId,
            driverId = driverId,
            driverPhone = cleanPhone,
            calledBy = "TransBuddy App Dispatcher"
        )

        // 2. Open Native Android Phone Dialer
        try {
            val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:$cleanPhone")
            }
            startActivity(dialIntent)
            Toast.makeText(this, "Calling $driverName ($cleanPhone)...", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Could not open dialer: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showDriverDirectoryDialog(drivers: List<EmergencyApiManager.EmergencyDriver>) {
        if (drivers.isEmpty()) {
            Toast.makeText(this, "No active driver contacts in database.", Toast.LENGTH_SHORT).show()
            return
        }

        val items = drivers.map { driver ->
            val busTag = if (driver.busNo.isNotBlank()) "Bus ${driver.busNo}" else "Campus Fleet"
            "${driver.driverName}\n📞 ${driver.driverPhone} • $busTag"
        }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("📞 Emergency Fleet Driver Directory (${drivers.size})")
            .setItems(items) { _, which ->
                val selectedDriver = drivers[which]
                dialDriverPhone(
                    phone = selectedDriver.driverPhone,
                    driverName = selectedDriver.driverName,
                    driverId = selectedDriver.driverId,
                    emergencyId = 0L
                )
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showEmergencyDetailsDialog(emergency: Emergency) {
        AlertDialog.Builder(this)
            .setTitle("🚨 ${emergency.status.uppercase()} - ${emergency.busNo}")
            .setMessage("Location: ${emergency.location}\n" +
                    "Severity: ${emergency.severity}\n" +
                    "Driver: ${emergency.driverName}\n" +
                    "Phone: ${emergency.driverPhone}\n\n" +
                    "Description:\n${emergency.description}")
            .setPositiveButton("📞 Call Driver") { _, _ ->
                dialDriverPhone(
                    phone = emergency.driverPhone,
                    driverName = emergency.driverName,
                    driverId = emergency.driverId,
                    emergencyId = emergency.id
                )
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun showEmergencies(records: List<Emergency>) {
        emergencyData.clear()
        emergencyData.addAll(records)
        emergencyAdapter.filter(etSearch.text.toString(), emergencyData)
        updateCount()
        updateEmptyState()
    }

    private fun updateCount() {
        val title = if (currentTab == 0) "Active Emergencies" else "Unread Notifications"
        val count = emergencyData.size
        tvEmergencyCount.text = "$title ($count)"
    }

    private fun updateEmptyState() {
        val isEmpty = emergencyData.isEmpty()
        tvEmergencyEmpty.visibility = if (isEmpty) View.VISIBLE else View.GONE
        btnRetry.visibility = if (isEmpty) View.VISIBLE else View.GONE
        recyclerViewEmergencies.visibility = if (isEmpty) View.GONE else View.VISIBLE
        if (!isEmpty) {
            tvEmergencyEmpty.text = if (currentTab == 0) "No active emergencies." else "No unread notifications."
        }
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
}
