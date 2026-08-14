package com.transbuddy.app.controllers

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.TextView
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
import com.transbuddy.app.adapters.ShiftLogAdapter
import com.transbuddy.app.models.ShiftLog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * OperationsActivity — CONTROLLER (MVC)
 *
 * Manages the Driver Hub / Operations screen:
 *  - Verify Attendance → Clock In Now (camera stub + live shift timer)
 *  - Shift Time counter (hh:mm:ss — ticks every second after clock-in)
 *  - Active Alerts count stat
 *  - Recent Logs RecyclerView (ShiftLogAdapter)
 *  - New Log Entry button
 *  - Violation Alerts empty state
 *  - Quick Tools: Route Map + Dispatch stubs
 *  - Navigation Drawer (Operations checked) + Bottom Nav (Ops active)
 */
class OperationsActivity : AppCompatActivity() {

    // ─── View references ───────────────────────────────────────
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var toolbar: Toolbar
    private lateinit var tvShiftTime: TextView
    private lateinit var tvActiveAlerts: TextView
    private lateinit var tvDateChip: TextView
    private lateinit var recyclerViewShiftLogs: RecyclerView

    // ─── Adapter + Data ────────────────────────────────────────
    private lateinit var shiftLogAdapter: ShiftLogAdapter

    private val logData = listOf(
        ShiftLog("clock_in", "Shift Started", "Terminal A",   "Yesterday"),
        ShiftLog("fuel",     "Fuel Logged",   "50 Gallons",   "Oct 22")
    )

    // ─── Shift timer state ────────────────────────────────────
    private var isClockedIn       = false
    private var shiftStartMillis  = 0L
    private val timerHandler      = Handler(Looper.getMainLooper())
    private lateinit var timerRunnable: Runnable

    // ─── Lifecycle ─────────────────────────────────────────────
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_operations)

        bindViews()
        setupToolbarAndDrawer()
        setupRecyclerView()
        setupDateChip()
        setupClockIn()
        setupNewLogEntry()
        setupQuickTools()
        initTimer()
    }

    override fun onDestroy() {
        super.onDestroy()
        timerHandler.removeCallbacks(timerRunnable)
    }

    // ─── View binding ──────────────────────────────────────────
    private fun bindViews() {
        drawerLayout          = findViewById(R.id.opsDrawerLayout)
        navigationView        = findViewById(R.id.opsNavigationView)
        toolbar               = findViewById(R.id.opsToolbar)
        tvShiftTime           = findViewById(R.id.tvShiftTime)
        tvActiveAlerts        = findViewById(R.id.tvActiveAlerts)
        tvDateChip            = findViewById(R.id.tvDateChip)
        recyclerViewShiftLogs = findViewById(R.id.recyclerViewShiftLogs)
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

        findViewById<android.widget.ImageButton>(R.id.opsBtnMenu).setOnClickListener {
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
                R.id.drawer_violations -> {
                    startActivity(Intent(this, ViolationsActivity::class.java)); finish()
                }
                R.id.drawer_operations -> { /* already here */ }
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }
        navigationView.setCheckedItem(R.id.drawer_operations)
    }

    // ─── Recent Logs RecyclerView ──────────────────────────────
    private fun setupRecyclerView() {
        shiftLogAdapter = ShiftLogAdapter(logData)
        recyclerViewShiftLogs.apply {
            layoutManager = LinearLayoutManager(this@OperationsActivity)
            adapter = shiftLogAdapter
            isNestedScrollingEnabled = false
        }
    }

    // ─── Date chip: show today's date ─────────────────────────
    private fun setupDateChip() {
        val fmt = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        tvDateChip.text = fmt.format(Date())
    }

    // ─── Clock In / Clock Out button ──────────────────────────
    private fun setupClockIn() {
        val btn      = findViewById<CardView>(R.id.btnClockIn)
        val btnLabel = findViewById<TextView>(R.id.tvClockInLabel)

        btn.setOnClickListener {
            if (!isClockedIn) {
                isClockedIn      = true
                shiftStartMillis = System.currentTimeMillis()
                btnLabel?.text   = "Clock Out"
                Toast.makeText(this, "✓ Clocked in! Shift timer started.", Toast.LENGTH_SHORT).show()
                // TODO: trigger camera / biometric verification
            } else {
                isClockedIn      = false
                btnLabel?.text   = "Clock In Now"
                timerHandler.removeCallbacks(timerRunnable)
                tvShiftTime.text = "00:00"
                Toast.makeText(this, "Clocked out. Have a safe trip!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ─── Shift timer (ticks every second when clocked in) ─────
    private fun initTimer() {
        timerRunnable = object : Runnable {
            override fun run() {
                if (isClockedIn) {
                    val elapsed = System.currentTimeMillis() - shiftStartMillis
                    val hours   = (elapsed / 3_600_000).toInt()
                    val minutes = ((elapsed % 3_600_000) / 60_000).toInt()
                    val seconds = ((elapsed % 60_000) / 1_000).toInt()
                    tvShiftTime.text = if (hours > 0)
                        String.format("%02d:%02d:%02d", hours, minutes, seconds)
                    else
                        String.format("%02d:%02d", minutes, seconds)
                }
                timerHandler.postDelayed(this, 1_000)
            }
        }
        timerHandler.post(timerRunnable)
    }

    // ─── New Log Entry button ──────────────────────────────────
    private fun setupNewLogEntry() {
        findViewById<CardView>(R.id.btnNewLogEntry).setOnClickListener {
            startActivity(Intent(this, FuelLogsActivity::class.java))
        }
    }

    // ─── Quick Tools ───────────────────────────────────────────
    private fun setupQuickTools() {
        findViewById<CardView>(R.id.btnRouteMap).setOnClickListener {
            Toast.makeText(this, "Route Map — coming soon.", Toast.LENGTH_SHORT).show()
        }
        findViewById<CardView>(R.id.btnDispatch).setOnClickListener {
            Toast.makeText(this, "Dispatch — coming soon.", Toast.LENGTH_SHORT).show()
        }
        findViewById<TextView>(R.id.btnViewAllLogs).setOnClickListener {
            Toast.makeText(this, "All Logs — coming soon.", Toast.LENGTH_SHORT).show()
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
