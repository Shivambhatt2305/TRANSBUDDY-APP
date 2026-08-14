package com.transbuddy.app.models

/**
 * StudentRider — MODEL (MVC)
 * Represents full details of a student or faculty member registered for campus transportation.
 * Sourced from `students_detail` and `faculty_detail` tables in database.
 */
data class StudentRider(
    val grNumber: String,        // e.g. "122434" or "EMP-102"
    val enrollmentNo: String = "",// e.g. "92100123045"
    val name: String,            // e.g. "Alex Morgan"
    val department: String = "", // e.g. "Computer Engineering"
    val semester: String = "",   // e.g. "Sem 6"
    val shift: String = "",      // e.g. "Shift 1"
    val email: String = "",      // e.g. "alex.morgan@marwadi.edu"
    val phone: String = "",      // e.g. "+91 9876543210"
    val busId: String,           // e.g. "TB-102"
    val pickupPoint: String,     // e.g. "Central Station, Gate 2"
    val route: String,           // e.g. "Route 9"
    val status: String,          // e.g. "Fee Paid", "Active"
    val memberType: String = "Student", // "Student" or "Faculty"
    val photoUrl: String = ""    // Photo API URL (SID for Student, Id for Faculty)
) {
    companion object {
        const val STUDENT_PHOTO_BASE = "https://marwadieducation.edu.in/MEFOnline/handler/getImage.ashx?SID="
        const val FACULTY_PHOTO_BASE = "https://marwadieducation.edu.in/MEFOnline/handler/getImage.ashx?Id="

        fun buildStudentPhotoUrl(grNo: String): String {
            val cleanId = grNo.trim().replace("GR-", "").replace("GR", "")
            return if (cleanId.isNotEmpty()) "$STUDENT_PHOTO_BASE$cleanId" else ""
        }

        fun buildFacultyPhotoUrl(empId: String): String {
            val cleanId = empId.trim().replace("EMP-", "").replace("EMP", "")
            return if (cleanId.isNotEmpty()) "$FACULTY_PHOTO_BASE$cleanId" else ""
        }
    }
}
