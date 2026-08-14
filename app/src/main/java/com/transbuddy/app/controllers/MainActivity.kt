package com.transbuddy.app.controllers

import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.EditText
import android.widget.LinearLayout
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.navigation.NavigationView
import com.transbuddy.app.R
import com.transbuddy.app.adapters.VehicleAdapter
import com.transbuddy.app.models.Vehicle

/**
 * MainActivity — CONTROLLER (MVC)
 * Manages the Fleet Dashboard screen:
 *  - Live Google Maps tracking with bus markers
 *  - Emergency alert banner
 *  - Searchable Active Fleet RecyclerView
 *  - Navigation Drawer + Bottom Nav Bar
 */
class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    // ─── View references ───────────────────────────────────────
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var toolbar: Toolbar
    private lateinit var etSearch: EditText
    private lateinit var recyclerViewFleet: RecyclerView
    private lateinit var markerTB102: android.widget.LinearLayout
    private lateinit var markerTB442: android.widget.LinearLayout
    private lateinit var mapView: MapView
    private var googleMap: GoogleMap? = null

    // ─── Adapter + Data ────────────────────────────────────────
    private lateinit var vehicleAdapter: VehicleAdapter
    private val fleetData = listOf(
        Vehicle("TB-102", "Route 9 North",  "Active",  "55 mph", "14 min"),
        Vehicle("TB-442", "Rerouting",      "Delay",   "15 mph", "+45 min", isAlert = true),
        Vehicle("TB-089", "Maintenance",    "Offline", "Garage", "2 Days")
    )

    // ─── Lifecycle ─────────────────────────────────────────────
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bindViews()
        setupToolbarAndDrawer()
        setupRecyclerView()
        setupSearch()
        positionMapMarkers()
        animateLivePulse()

        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)
    }

    // ─── View binding ──────────────────────────────────────────
    private fun bindViews() {
        drawerLayout      = findViewById(R.id.drawerLayout)
        navigationView    = findViewById(R.id.navigationView)
        toolbar           = findViewById(R.id.toolbar)
        etSearch          = findViewById(R.id.etSearch)
        recyclerViewFleet = findViewById(R.id.recyclerViewFleet)
        markerTB102       = findViewById(R.id.markerTB102)
        markerTB442       = findViewById(R.id.markerTB442)
        mapView           = findViewById(R.id.googleMapView)
    }

    // ─── Google Maps Callback ──────────────────────────────────
    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        map.mapType = GoogleMap.MAP_TYPE_NORMAL
        map.uiSettings.isZoomControlsEnabled = true
        map.uiSettings.isCompassEnabled = true

        // Center on Marwadi University / Rajkot Campus
        val marwadiCampus = LatLng(22.3688, 70.8016)
        val busTB102Loc   = LatLng(22.3021, 70.7839)
        val busTB442Loc   = LatLng(22.2525, 70.7919)

        map.addMarker(
            MarkerOptions()
                .position(marwadiCampus)
                .title("Marwadi University Campus")
                .snippet("Main Terminal & Bus Station")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET))
        )

        map.addMarker(
            MarkerOptions()
                .position(busTB102Loc)
                .title("TB-102 (Active)")
                .snippet("Route 9 North • 55 mph")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
        )

        map.addMarker(
            MarkerOptions()
                .position(busTB442Loc)
                .title("TB-442 (Rerouting - Delay)")
                .snippet("Traffic Delay • 15 mph")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE))
        )

        map.moveCamera(CameraUpdateFactory.newLatLngZoom(marwadiCampus, 11.5f))
    }

    // ─── Map Lifecycle Pass-throughs ───────────────────────────
    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        mapView.onPause()
        super.onPause()
    }

    override fun onDestroy() {
        mapView.onDestroy()
        super.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView.onSaveInstanceState(outState)
    }

    // ─── Toolbar & Navigation Drawer ───────────────────────────
    private fun setupToolbarAndDrawer() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        // Hamburger toggle
        val toggle = ActionBarDrawerToggle(
            this, drawerLayout,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        // Menu button (custom button in layout)
        findViewById<android.widget.ImageButton>(R.id.btnMenu).setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        // Drawer item selection
        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.drawer_home_search -> {
                    startActivity(Intent(this, HomeSearchActivity::class.java))
                }
                R.id.drawer_fuel -> {
                    startActivity(Intent(this, FuelLogsActivity::class.java))
                }
                R.id.drawer_penalties -> {
                    startActivity(Intent(this, PenaltiesActivity::class.java))
                }
                R.id.drawer_operations -> {
                    startActivity(Intent(this, OperationsActivity::class.java))
                }
                R.id.drawer_violations -> {
                    startActivity(Intent(this, ViolationsActivity::class.java))
                }
                R.id.drawer_dashboard -> { /* already here */ }
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }
        // Mark Fleet Dashboard as checked
        navigationView.setCheckedItem(R.id.drawer_dashboard)
    }

    // ─── RecyclerView ──────────────────────────────────────────
    private fun setupRecyclerView() {
        vehicleAdapter = VehicleAdapter(fleetData)
        recyclerViewFleet.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = vehicleAdapter
            isNestedScrollingEnabled = false
        }
    }

    // ─── Search bar ────────────────────────────────────────────
    private fun setupSearch() {
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                vehicleAdapter.filter(s.toString(), fleetData)
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    // ─── Map markers: position TB-102 at ~40% x, 30% y
    //                          TB-442 at ~70% x, 60% y ─────────
    private fun positionMapMarkers() {
        val mapCard = findViewById<androidx.cardview.widget.CardView>(R.id.mapCard)
        mapCard.post {
            val w = mapCard.width.toFloat()
            val h = mapCard.height.toFloat()

            // TB-102: top-left 40%/30%
            markerTB102.translationX = w * 0.38f
            markerTB102.translationY = h * 0.22f

            // TB-442: top-left 70%/60%
            markerTB442.translationX = w * 0.62f
            markerTB442.translationY = h * 0.50f
        }
    }

    // ─── Pulse animation for the Live dot ─────────────────────
    private fun animateLivePulse() {
        val liveDot = findViewById<android.view.View>(R.id.livePulse)
        val anim = ObjectAnimator.ofFloat(liveDot, "alpha", 1f, 0.2f, 1f).apply {
            duration = 1200
            repeatCount = ObjectAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
        }
        anim.start()
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