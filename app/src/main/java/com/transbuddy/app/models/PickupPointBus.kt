package com.transbuddy.app.models

/**
 * PickupPointBus — MODEL (MVC)
 * Represents a pickup point and its assigned bus & driver information.
 */
data class PickupPointBus(
    val pickupPointName: String,  // e.g. "Central Station, Gate 2"
    val areaZone: String,         // e.g. "Downtown / Zone A"
    val assignedBusId: String,    // e.g. "TB-102"
    val driverName: String,       // e.g. "Michael Scott"
    val pickupTime: String,       // e.g. "07:30 AM"
    val studentCount: Int,        // e.g. 18
    val driverPhotoUrl: String = "" // Driver / Staff profile photo URL
)
