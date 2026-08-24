package com.transbuddy.app.models

/**
 * User — Model representing a TransBuddy user / administrator
 */
data class User(
    val id: Long = 0,
    val username: String = "",
    val password: String = "",
    val email: String = "",
    val fullName: String = "",
    val role: String = "Fleet Manager Admin",
    val createdAt: String = ""
)
