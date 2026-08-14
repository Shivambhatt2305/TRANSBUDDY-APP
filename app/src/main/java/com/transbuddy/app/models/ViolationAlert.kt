package com.transbuddy.app.models

/**
 * ViolationAlert MODEL (MVC)
 *
 * @param alertType   "unauthorized" | "unpaid" | "invalid_scan" — drives icon + tint
 * @param title       Alert title (e.g. "Unauthorized Rider")
 * @param busInfo     Sub-label (e.g. "Bus ID: TX-8492 • Route 15")
 * @param time        Display time string (e.g. "10:42 AM")
 * @param isResolved  True → show "Resolved" badge; false → show "Review" button
 */
data class ViolationAlert(
    val alertType: String,
    val title: String,
    val busInfo: String,
    val time: String,
    val isResolved: Boolean = false
)
