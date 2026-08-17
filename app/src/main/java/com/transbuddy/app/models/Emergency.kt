package com.transbuddy.app.models

data class Emergency(
    val id: Long = 0,
    val busId: String = "",
    val driverId: String = "",
    val location: String = "",
    val status: String = "",
    val severity: String = "",
    val description: String = "",
    val createdAt: String = "",
    val updatedAt: String = "",
    val driverName: String = "",
    val username: String = "",
    val busNo: String = ""
)
