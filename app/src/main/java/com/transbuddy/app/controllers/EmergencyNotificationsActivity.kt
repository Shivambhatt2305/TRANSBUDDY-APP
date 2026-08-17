package com.transbuddy.app.controllers

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
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
import com.google.android.material.tabs.TabLayout
import com.transbuddy.app.R
import com.transbuddy.app.adapters.EmergencyAdapter
import com.transbuddy.app.models.Emergency
import com.transbuddy.app.utils.EmergencyApiManager

class EmergencyNotificationsActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var toolbar: Toolbar
    private lateinit var etSearch: EditText
    private lateinit var recyclerViewEmergencies: RecyclerView
    private lateinit var tvEmergencyCount: TextView
    private lateinit var tvEmergencyEmpty: TextView
    private lateinit var tabLayout: TabLayout
    private lateinit var btnRetry: android.widget.Button
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
        if (currentTab == 0) {
            EmergencyApiManager.fetchActiveEmergencies(
                onResult = { showEmergencies(it) },
                onError = { showEmptyWithError(it) }
            )
        } else {
            EmergencyApiManager.fetchUnreadNotifications(
                onResult = { showEmergencies(it) },
                onError = { showEmptyWithError(it) }
            )
        }
    }

    private fun showEmptyWithError(message: String) {
        emergencyData.clear()
        emergencyAdapter.filter(etSearch.text.toString(), emergencyData)
        updateCount()
        tvEmergencyEmpty.text = "Load failed: $message"
        btnRetry.visibility = android.view.View.VISIBLE
        updateEmptyState()
    }

    private fun bindViews() {
        drawerLayout           = findViewById(R.id.emergencyDrawerLayout)
        navigationView         = findViewById(R.id.emergencyNavigationView)
        toolbar                = findViewById(R.id.emergencyToolbar)
        etSearch               = findViewById(R.id.etEmergencySearch)
        recyclerViewEmergencies = findViewById(R.id.recyclerViewEmergencies)
        tvEmergencyCount       = findViewById(R.id.tvEmergencyCount)
        tvEmergencyEmpty       = findViewById(R.id.tvEmergencyEmpty)
        tabLayout              = findViewById(R.id.tabLayoutEmergency)
        btnRetry               = findViewById(R.id.btnRetry)
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

        findViewById<android.widget.ImageButton>(R.id.emergencyBtnMenu).setOnClickListener {
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
                R.id.drawer_emergency -> { /* already here */ }
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }
        navigationView.setCheckedItem(R.id.drawer_emergency)
    }

    private fun setupRecyclerView() {
        emergencyAdapter = EmergencyAdapter(emergencyData) { emergency ->
            Toast.makeText(this, "Selected: ${emergency.status}", Toast.LENGTH_SHORT).show()
        }
        recyclerViewEmergencies.apply {
            layoutManager = LinearLayoutManager(this@EmergencyNotificationsActivity)
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
        tvEmergencyEmpty.visibility = if (isEmpty) android.view.View.VISIBLE else android.view.View.GONE
        btnRetry.visibility = if (isEmpty) android.view.View.VISIBLE else android.view.View.GONE
        recyclerViewEmergencies.visibility = if (isEmpty) android.view.View.GONE else android.view.View.VISIBLE
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
