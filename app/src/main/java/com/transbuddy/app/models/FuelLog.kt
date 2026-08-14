package com.transbuddy.app.models

/**
 * FuelLog MODEL (MVC)
 * Represents a single fuel log entry.
 */
data class FuelLog(
    val stationName: String,
    val timestamp: String,
    val liters: Double,
    val totalCost: Double,
    val tripKm: Int,
    val isRecent: Boolean = false
)
