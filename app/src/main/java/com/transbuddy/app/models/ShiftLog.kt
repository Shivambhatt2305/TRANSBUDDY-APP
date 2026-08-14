package com.transbuddy.app.models

/**
 * ShiftLog MODEL (MVC)
 * Represents a single entry in the Recent Logs list.
 *
 * @param logType    "clock_in" | "fuel" | "route" | "other" — drives icon
 * @param title      Primary label (e.g. "Shift Started")
 * @param subtitle   Secondary label (e.g. "Terminal A")
 * @param timestamp  Display timestamp string (e.g. "Yesterday", "Oct 22")
 */
data class ShiftLog(
    val logType: String,
    val title: String,
    val subtitle: String,
    val timestamp: String
)
