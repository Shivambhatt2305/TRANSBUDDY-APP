package com.transbuddy.app.models

/**
 * User — Data model representing an authenticated user in TransBuddy.
 */
data class User(
    val id: Long = 0L,
    val username: String = "",
    val password: String = "",
    val fullName: String = "",
    val role: String = "ADMIN",
    val email: String = "",
    val phone: String = "",
    val createdAt: String = ""
)
