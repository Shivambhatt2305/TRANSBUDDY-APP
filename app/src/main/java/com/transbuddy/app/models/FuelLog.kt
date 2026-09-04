package com.transbuddy.app.models

/**
 * FuelLog MODEL (MVC)
 * Represents a single refuel and odometer log entry backed by SQLite Database.
 */
data class FuelLog(
    val id: Long = 0L,
    val stationName: String,
    val timestamp: String,
    val liters: Double,
    val totalCost: Double,
    val tripKm: Int,
    val isRecent: Boolean = false,
    val receiptUri: String? = null
)
