package com.transbuddy.app.controllers

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import com.google.android.material.button.MaterialButton
import com.transbuddy.app.R
import com.transbuddy.app.utils.EmailSender
import com.transbuddy.app.utils.ImageLoader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * StudentDetailActivity — CONTROLLER (MVC)
 * Displays complete profile details for a selected student/faculty member and provides
 * penalty notice composition with automatic background email dispatch via SMTP.
 */
class StudentDetailActivity : AppCompatActivity() {

    // View references
    private lateinit var btnBack: ImageButton
    private lateinit var ivDetailPhoto: ImageView
    private lateinit var tvDetailName: TextView
    private lateinit var tvDetailMemberType: TextView
    private lateinit var tvDetailStatus: TextView
    private lateinit var tvDetailGrEnroll: TextView
    private lateinit var tvDetailDept: TextView
    private lateinit var tvDetailSemShift: TextView
    private lateinit var tvDetailPickup: TextView
    private lateinit var tvDetailBusRoute: TextView
    private lateinit var tvDetailEmail: TextView
    private lateinit var tvDetailPhone: TextView

    private lateinit var etPenaltyReason: EditText
    private lateinit var etPenaltyAmount: EditText
    private lateinit var btnSendPenaltyEmail: MaterialButton

    // Student Data
    private var grNumber: String = ""
    private var enrollmentNo: String = ""
    private var studentName: String = ""
    private var department: String = ""
    private var semester: String = ""
    private var shift: String = ""
    private var studentEmail: String = ""
    private var studentPhone: String = ""
    private var busId: String = ""
    private var pickupPoint: String = ""
    private var route: String = ""
    private var status: String = ""
    private var memberType: String = ""
    private var photoUrl: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_student_detail)

        extractIntentExtras()
        bindViews()
        populateStudentData()
        setupListeners()
    }

    private fun extractIntentExtras() {
        intent?.let {
            grNumber     = it.getStringExtra("GR_NUMBER") ?: ""
            enrollmentNo = it.getStringExtra("ENROLLMENT_NO") ?: ""
            studentName  = it.getStringExtra("STUDENT_NAME") ?: "Student"
            department   = it.getStringExtra("DEPARTMENT") ?: "N/A"
            semester     = it.getStringExtra("SEMESTER") ?: "N/A"
            shift        = it.getStringExtra("SHIFT") ?: "N/A"
            studentEmail = it.getStringExtra("EMAIL") ?: ""
            studentPhone = it.getStringExtra("PHONE") ?: ""
            busId        = it.getStringExtra("BUS_ID") ?: "TB-102"
            pickupPoint  = it.getStringExtra("PICKUP_POINT") ?: "Central Station"
            route        = it.getStringExtra("ROUTE") ?: "Route 9"
            status       = it.getStringExtra("STATUS") ?: "Paid"
            memberType   = it.getStringExtra("MEMBER_TYPE") ?: "Student"
            photoUrl     = it.getStringExtra("PHOTO_URL") ?: ""
        }
    }

    private fun bindViews() {
        btnBack            = findViewById(R.id.btnBack)
        ivDetailPhoto      = findViewById(R.id.ivDetailPhoto)
        tvDetailName       = findViewById(R.id.tvDetailName)
        tvDetailMemberType = findViewById(R.id.tvDetailMemberType)
        tvDetailStatus     = findViewById(R.id.tvDetailStatus)
        tvDetailGrEnroll   = findViewById(R.id.tvDetailGrEnroll)
        tvDetailDept       = findViewById(R.id.tvDetailDept)
        tvDetailSemShift   = findViewById(R.id.tvDetailSemShift)
        tvDetailPickup     = findViewById(R.id.tvDetailPickup)
        tvDetailBusRoute   = findViewById(R.id.tvDetailBusRoute)
        tvDetailEmail      = findViewById(R.id.tvDetailEmail)
        tvDetailPhone      = findViewById(R.id.tvDetailPhone)

        etPenaltyReason     = findViewById(R.id.etPenaltyReason)
        etPenaltyAmount     = findViewById(R.id.etPenaltyAmount)
        btnSendPenaltyEmail = findViewById(R.id.btnSendPenaltyEmail)
    }

    private fun populateStudentData() {
        tvDetailName.text       = studentName
        tvDetailMemberType.text = "$memberType Profile"
        tvDetailStatus.text     = status
        tvDetailGrEnroll.text   = if (enrollmentNo.isNotEmpty()) "GR: $grNumber | ENROLL: $enrollmentNo" else "ID: $grNumber"
        tvDetailDept.text       = department
        tvDetailSemShift.text   = "$semester • $shift"
        tvDetailPickup.text     = pickupPoint
        tvDetailBusRoute.text   = "$busId ($route)"
        tvDetailEmail.text      = if (studentEmail.isNotEmpty()) studentEmail else "N/A"
        tvDetailPhone.text      = if (studentPhone.isNotEmpty()) studentPhone else "N/A"

        // Load profile photo from Marwadi photo API
        ImageLoader.loadImage(
            photoUrl,
            ivDetailPhoto,
            android.R.drawable.ic_menu_myplaces
        )
    }

    private fun setupListeners() {
        btnBack.setOnClickListener { finish() }

        btnSendPenaltyEmail.setOnClickListener {
            val reason = etPenaltyReason.text.toString().trim()
            val amount = etPenaltyAmount.text.toString().trim()

            if (reason.isEmpty()) {
                Toast.makeText(this, "Please enter the reason of penalty", Toast.LENGTH_SHORT).show()
                etPenaltyReason.requestFocus()
                return@setOnClickListener
            }

            val finalAmount = if (amount.isNotEmpty()) amount else "500"
            val targetEmail = if (studentEmail.isNotEmpty()) studentEmail else "${grNumber.lowercase()}@marwadi.edu"

            sendPenaltyEmailAutomatically(targetEmail, reason, finalAmount)
        }
    }

    private fun sendPenaltyEmailAutomatically(emailId: String, reasonOfPenalty: String, amount: String) {
        val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        val currentDate = dateFormat.format(Date())

        val subject = "[Transbuddy Notification] Official Transportation Penalty Notice - GR: $grNumber"

        val emailBody = """
            Dear $studentName,

            This is an official administrative notice regarding your campus transportation profile under Transbuddy Fleet Management.

            ─────────────────────────────────────────
            MEMBER DETAILS
            ─────────────────────────────────────────
            • Name: $studentName
            • GR Number: $grNumber
            • Enrollment No: ${if (enrollmentNo.isNotEmpty()) enrollmentNo else "N/A"}
            • Department: $department ($semester, $shift)
            • Pickup Point: $pickupPoint
            • Assigned Bus: $busId ($route)
            • Registered Email: $emailId

            ─────────────────────────────────────────
            PENALTY & NOTICE DETAILS
            ─────────────────────────────────────────
            • Reason of Penalty: $reasonOfPenalty
            • Penalty Fine Amount: ₹$amount
            • Date of Issue: $currentDate
            • Status: Pending Payment

            Please resolve this penalty fine at the campus transit desk or contact the transport administration office.

            Regards,
            Transbuddy Fleet Management & Logistics
            Marwadi University Campus
        """.trimIndent()

        // Disable button during background SMTP transmission
        btnSendPenaltyEmail.isEnabled = false
        btnSendPenaltyEmail.text = "⏳ SENDING AUTOMATIC EMAIL..."

        EmailSender.sendEmailInBackground(
            toEmail = emailId,
            subject = subject,
            bodyText = emailBody,
            onSuccess = {
                btnSendPenaltyEmail.isEnabled = true
                btnSendPenaltyEmail.text = "✉️  SEND PENALTY EMAIL"
                etPenaltyReason.text.clear()

                AlertDialog.Builder(this)
                    .setTitle("✅ Email Sent Automatically!")
                    .setMessage("Official penalty notice email was successfully sent automatically to:\n\n$emailId\n\nReason: $reasonOfPenalty\nFine Amount: ₹$amount")
                    .setPositiveButton("OK", null)
                    .show()
            },
            onError = { error ->
                btnSendPenaltyEmail.isEnabled = true
                btnSendPenaltyEmail.text = "✉️  SEND PENALTY EMAIL"

                AlertDialog.Builder(this)
                    .setTitle("❌ Email Dispatch Failed")
                    .setMessage("Could not send email automatically: $error")
                    .setPositiveButton("OK", null)
                    .show()
            }
        )
    }
}
