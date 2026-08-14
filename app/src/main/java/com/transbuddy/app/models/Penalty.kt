package com.transbuddy.app.models

/**
 * Penalty MODEL (MVC)
 * Represents a single disciplinary action / fine entry.
 *
 * @param iconType   One of "speeding", "route", "idle", "safety" — drives icon + tint
 * @param title      Infraction label (e.g. "Speeding Violation")
 * @param driverInfo Driver ID + name + date (e.g. "D-1042: Michael Chang • Oct 24, 2023")
 * @param amount     Dollar amount string OR "Warning" for non-monetary penalties
 * @param isError    True → amount shown in error/red; false → shown in on_surface
 */
data class Penalty(
    val iconType: String,
    val title: String,
    val driverInfo: String,
    val amount: String,
    val isError: Boolean = false
)
