package com.transbuddy.app.models

/**
 * Penalty MODEL (MVC)
 * Represents a single disciplinary action / fine entry matching the Database table schema:
 * id, target_type, target_id, target_name, target_email, infraction_category, infraction_reason, amount, notes, status, assigned_by, created_at
 */
data class Penalty(
    val id: Long = 0,
    val targetType: String = "DRIVER",          // "DRIVER" or "STUDENT"
    val targetId: String = "",                 // e.g. "DRV-101" or "202100584"
    val targetName: String = "",               // e.g. "MR. SUDHIRBHAI BHUTA" or "Aarav Patel"
    val targetEmail: String = "",              // e.g. "aarav.patel@marwadi.edu"
    val infractionCategory: String = "",        // e.g. "Over-Speeding", "Misbehavior"
    val infractionReason: String = "",          // e.g. "Over-Speeding In Campus Zone"
    val amountNum: Double = 500.00,             // Double for MySQL decimal column
    val amount: String = "₹500",                // UI display string
    val notes: String = "Assigned via TransBuddy App",
    val status: String = "PENDING",
    val assignedBy: String = "Android App",
    val photoUrl: String = "",
    val createdAt: String = "",
    val iconType: String = "speeding",
    val driverInfo: String = "",                // UI label fallback
    val isError: Boolean = true,
    val title: String = ""                     // UI label title
)

