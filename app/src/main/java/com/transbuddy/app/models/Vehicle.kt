package com.transbuddy.app.models

// MODEL in MVC
data class Vehicle(
    val id: String,
    val route: String,
    val status: String,
    val speed: String,
    val eta: String,
    val isAlert: Boolean = false
)