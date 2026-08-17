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

        val htmlContent = buildPenaltyEmailHtml(
            studentName = studentName,
            grNumber = grNumber,
            enrollmentNo = enrollmentNo,
            department = department,
            semester = semester,
            shift = shift,
            pickupPoint = pickupPoint,
            busId = busId,
            route = route,
            emailId = emailId,
            reasonOfPenalty = reasonOfPenalty,
            amount = amount,
            currentDate = currentDate
        )

        // Disable button during background SMTP transmission
        btnSendPenaltyEmail.isEnabled = false
        btnSendPenaltyEmail.text = "⏳ SENDING AUTOMATIC EMAIL..."

        EmailSender.sendHtmlEmailInBackground(
            toEmail = emailId,
            subject = subject,
            htmlContent = htmlContent,
            onSuccess = {
                btnSendPenaltyEmail.isEnabled = true
                btnSendPenaltyEmail.text = "✉️  SEND PENALTY EMAIL"
                etPenaltyReason.text.clear()

                // Save student penalty to DB Table
                saveStudentPenaltyToDb(reasonOfPenalty, amount, emailId)

                AlertDialog.Builder(this)
                    .setTitle("✅ Email Sent & Saved to DB!")
                    .setMessage("Official penalty notice email was sent automatically and saved to Database Table for:\n\nName: $studentName\nEmail: $emailId\nReason: $reasonOfPenalty\nFine Amount: ₹$amount")
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

    private fun saveStudentPenaltyToDb(reasonOfPenalty: String, amount: String, targetEmail: String) {
        val dbHelper = com.transbuddy.app.utils.PenaltyDatabaseHelper.getInstance(this)
        val formattedAmount = if (amount.startsWith("₹")) amount else "₹$amount"
        val numericAmount = amount.replace("[^0-9.]".toRegex(), "").toDoubleOrNull() ?: 500.00
        val targetIdVal = if (enrollmentNo.isNotEmpty()) enrollmentNo else grNumber
        val infoStr = if (enrollmentNo.isNotEmpty()) "$studentName ($enrollmentNo) • STUDENT" else "$studentName (GR: $grNumber) • STUDENT"

        val studentPenalty = com.transbuddy.app.models.Penalty(
            targetType = "STUDENT",
            targetId = targetIdVal,
            targetName = studentName,
            targetEmail = targetEmail,
            infractionCategory = reasonOfPenalty,
            infractionReason = reasonOfPenalty,
            amountNum = numericAmount,
            amount = formattedAmount,
            notes = "Assigned via TransBuddy App",
            status = "PENDING",
            assignedBy = "Android App",
            iconType = "student",
            driverInfo = infoStr,
            isError = true
        )
        // 1. Local SQLite storage
        dbHelper.insertPenalty(studentPenalty)

        // 2. Sync through the backend API to MySQL Database.
        com.transbuddy.app.utils.CloudDatabaseManager.syncPenaltyToCloud(
            penalty = studentPenalty,
            onSuccess = {
                Toast.makeText(
                    this,
                    "☁️ Live Synced to Admin DB Table: $studentName ($reasonOfPenalty)",
                    Toast.LENGTH_LONG
                ).show()
            },
            onError = { err ->
                Toast.makeText(this, "Student penalty saved locally. Cloud sync: $err", Toast.LENGTH_LONG).show()
            }
        )
    }

    private fun buildPenaltyEmailHtml(
        studentName: String,
        grNumber: String,
        enrollmentNo: String,
        department: String,
        semester: String,
        shift: String,
        pickupPoint: String,
        busId: String,
        route: String,
        emailId: String,
        reasonOfPenalty: String,
        amount: String,
        currentDate: String
    ): String {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
            </head>
            <body style="margin:0; padding:0; background-color:#f4f6f9; font-family:'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; color:#333333;">
                <table width="100%" cellpadding="0" cellspacing="0" style="background-color:#f4f6f9; padding: 20px 0;">
                    <tr>
                        <td align="center">
                            <table width="650" cellpadding="0" cellspacing="0" style="background-color:#ffffff; border-radius: 8px; overflow:hidden; border: 1px solid #e0e0e0; box-shadow: 0 4px 12px rgba(0,0,0,0.05);">
                                <!-- Top Notification Bar -->
                                <tr>
                                    <td style="background-color: #0F294A; padding: 12px 25px; color: #ffffff;">
                                        <table width="100%" cellpadding="0" cellspacing="0">
                                            <tr>
                                                <td style="font-size: 13px; font-weight: 600; letter-spacing: 0.5px; text-transform: uppercase;">
                                                    Transbuddy Fleet Management System
                                                </td>
                                                <td align="right" style="font-size: 12px; opacity: 0.85;">
                                                    $currentDate
                                                </td>
                                            </tr>
                                        </table>
                                    </td>
                                </tr>

                                <!-- Main Penalty Notice Content -->
                                <tr>
                                    <td style="padding: 28px 30px;">
                                        <h2 style="color: #0F294A; margin-top: 0; margin-bottom: 16px; font-size: 20px; font-weight: bold; border-bottom: 2px solid #0F294A; padding-bottom: 8px;">
                                            Official Transportation Penalty Notice
                                        </h2>
                                        <p style="font-size: 15px; color: #333333; margin-bottom: 14px; line-height: 1.5;">Dear <strong>$studentName</strong>,</p>
                                        <p style="font-size: 14px; color: #555555; line-height: 1.6; margin-bottom: 20px;">
                                            This is an official administrative notice regarding your campus transportation profile under Transbuddy Fleet Management. A penalty has been logged against your account details below.
                                        </p>
                                        
                                        <!-- Member Details Card -->
                                        <table width="100%" cellpadding="10" cellspacing="0" style="background-color: #f8fafc; border-left: 4px solid #0F294A; border-radius: 0 6px 6px 0; margin-bottom: 20px; font-size: 13px; color: #333333;">
                                            <tr>
                                                <td style="font-size: 12px; font-weight: bold; color: #0F294A; text-transform: uppercase; letter-spacing: 0.5px; padding-bottom: 4px;" colspan="2">
                                                    MEMBER & TRANSPORT PROFILE
                                                </td>
                                            </tr>
                                            <tr><td width="35%"><strong>Name:</strong></td><td>$studentName</td></tr>
                                            <tr><td><strong>GR Number:</strong></td><td>$grNumber</td></tr>
                                            <tr><td><strong>Enrollment No:</strong></td><td>${if (enrollmentNo.isNotEmpty()) enrollmentNo else "N/A"}</td></tr>
                                            <tr><td><strong>Department / Sem:</strong></td><td>$department (${if (semester.isNotEmpty()) semester else "Degree"}, ${if (shift.isNotEmpty()) shift else "Regular"})</td></tr>
                                            <tr><td><strong>Pickup Point:</strong></td><td>$pickupPoint</td></tr>
                                            <tr><td><strong>Assigned Bus:</strong></td><td>$busId (${if (route.isNotEmpty()) route else "Campus Route"})</td></tr>
                                            <tr><td><strong>Registered Email:</strong></td><td>$emailId</td></tr>
                                        </table>

                                        <!-- Penalty & Notice Details Card -->
                                        <table width="100%" cellpadding="14" cellspacing="0" style="background-color: #fff5f5; border: 1px solid #fed7d7; border-radius: 6px; margin-bottom: 20px; font-size: 14px;">
                                            <tr>
                                                <td>
                                                    <div style="font-weight: bold; font-size: 15px; margin-bottom: 10px; color: #9b2c2c; text-transform: uppercase; letter-spacing: 0.5px;">
                                                        ⚠️ PENALTY DETAILS
                                                    </div>
                                                    <table width="100%" cellpadding="4" cellspacing="0" style="font-size: 13px; color: #2d3748;">
                                                        <tr>
                                                            <td width="35%"><strong>Reason of Penalty:</strong></td>
                                                            <td style="color: #c53030; font-weight: bold;">$reasonOfPenalty</td>
                                                        </tr>
                                                        <tr>
                                                            <td><strong>Fine Amount:</strong></td>
                                                            <td><span style="font-size: 18px; font-weight: 800; color: #e53e3e;">₹$amount</span></td>
                                                        </tr>
                                                        <tr>
                                                            <td><strong>Date of Issue:</strong></td>
                                                            <td>$currentDate</td>
                                                        </tr>
                                                        <tr>
                                                            <td><strong>Payment Status:</strong></td>
                                                            <td>
                                                                <span style="background-color: #feebc8; color: #744210; padding: 3px 8px; border-radius: 4px; font-weight: bold; font-size: 11px; display: inline-block;">
                                                                    PENDING PAYMENT
                                                                </span>
                                                            </td>
                                                        </tr>
                                                    </table>
                                                </td>
                                            </tr>
                                        </table>

                                        <p style="font-size: 13px; color: #666666; line-height: 1.5; margin-bottom: 0;">
                                            Please resolve this penalty fine at the campus transit desk or contact the transport administration office.
                                        </p>
                                    </td>
                                </tr>

                                <!-- OFFICIAL FOOTER BANNER (Matches Image) -->
                                <tr>
                                    <td style="background-color: #ffffff; border-top: 2px solid #e2e8f0; padding: 20px 15px;">
                                        
                                        <!-- Top 3-Column Header Section -->
                                        <table width="100%" cellpadding="0" cellspacing="0" style="border-collapse: collapse;">
                                            <tr>
                                                <!-- Left Column: Marwadi University Brand -->
                                                <td width="32%" align="left" valign="middle" style="padding-right: 8px;">
                                                    <table cellpadding="0" cellspacing="0">
                                                        <tr>
                                                            <td valign="middle" style="padding-right: 8px;">
                                                                <div style="width: 36px; height: 42px; border-radius: 0 0 16px 16px; background: linear-gradient(135deg, #005696 0%, #0088CC 100%); color: #ffffff; font-weight: bold; font-size: 20px; line-height: 38px; text-align: center; font-family: sans-serif;">
                                                                    M
                                                                </div>
                                                            </td>
                                                            <td valign="middle">
                                                                <div style="font-size: 17px; font-weight: 800; color: #002B49; font-family: sans-serif; line-height: 1.1;">Marwadi</div>
                                                                <div style="font-size: 17px; font-weight: 800; color: #002B49; font-family: sans-serif; line-height: 1.1;">University</div>
                                                                <div style="font-size: 9px; color: #007A3D; font-weight: 700; font-family: sans-serif; margin-top: 2px;">Marwadi Chandarana Group</div>
                                                            </td>
                                                        </tr>
                                                    </table>
                                                </td>

                                                <!-- Center Column: Solid Navy Transportation Dept Banner -->
                                                <td width="40%" align="center" valign="middle" style="background-color: #0F294A; padding: 14px 10px;">
                                                    <div style="font-size: 17px; font-weight: 900; color: #ffffff; letter-spacing: 1.2px; font-family: sans-serif; line-height: 1.2; text-transform: uppercase;">
                                                        TRANSPORTATION
                                                    </div>
                                                    <div style="font-size: 17px; font-weight: 900; color: #ffffff; letter-spacing: 1.2px; font-family: sans-serif; line-height: 1.2; text-transform: uppercase;">
                                                        DEPARTMENT
                                                    </div>
                                                    <div style="width: 40px; height: 3px; background-color: #D4AF37; margin: 6px auto 0 auto; border-radius: 2px;"></div>
                                                </td>

                                                <!-- Right Column: Contact & Website -->
                                                <td width="28%" align="left" valign="middle" style="padding-left: 12px; font-family: sans-serif;">
                                                    <div style="margin-bottom: 6px;">
                                                        <div style="font-size: 12px; font-weight: bold; color: #007A3D;">Contact</div>
                                                        <a href="mailto:transport.dept@marwadieducation.edu.in" style="font-size: 10px; color: #0F294A; text-decoration: none; word-break: break-all; font-weight: 500;">
                                                            transport.dept@marwadieducation.edu.in
                                                        </a>
                                                    </div>
                                                    <div>
                                                        <div style="font-size: 12px; font-weight: bold; color: #007A3D;">Website</div>
                                                        <a href="https://www.marwadiuniversity.ac.in" target="_blank" style="font-size: 10px; color: #0F294A; text-decoration: none; font-weight: 500;">
                                                            www.marwadiuniversity.ac.in
                                                        </a>
                                                    </div>
                                                </td>
                                            </tr>
                                        </table>

                                        <!-- Horizontal Line -->
                                        <div style="height: 1px; background-color: #CBD5E1; margin: 16px 0 12px 0;"></div>

                                        <!-- Bottom Accreditations Bar (6 Badges) -->
                                        <table width="100%" cellpadding="0" cellspacing="0" style="border-collapse: collapse; font-family: sans-serif;">
                                            <tr>
                                                <!-- Badge 1: QS Ranking -->
                                                <td align="center" valign="middle" style="padding: 2px 4px; border-right: 1px solid #E2E8F0;">
                                                    <div style="background-color: #F59E0B; color: #ffffff; font-size: 8px; font-weight: 800; padding: 1px 4px; border-radius: 2px; display: inline-block;">QS</div>
                                                    <div style="font-size: 8px; font-weight: bold; color: #1E293B; margin-top: 1px;">ASIA RANK 353</div>
                                                    <div style="font-size: 6px; color: #64748B;">WORLD UNIVERSITY RANKINGS</div>
                                                </td>

                                                <!-- Badge 2: NIRF -->
                                                <td align="center" valign="middle" style="padding: 2px 4px; border-right: 1px solid #E2E8F0;">
                                                    <div style="font-size: 10px; font-weight: 900; color: #1E40AF;">nirf</div>
                                                    <div style="font-size: 8px; font-weight: bold; color: #1E293B;">ALL INDIA TOP 100-150</div>
                                                    <div style="font-size: 6px; color: #64748B;">University Ranking</div>
                                                </td>

                                                <!-- Badge 3: NAAC A+ -->
                                                <td align="center" valign="middle" style="padding: 2px 4px; border-right: 1px solid #E2E8F0;">
                                                    <div style="background-color: #991B1B; color: #ffffff; font-size: 9px; font-weight: bold; padding: 1px 5px; border-radius: 3px; display: inline-block;">A+</div>
                                                    <div style="font-size: 7px; font-weight: bold; color: #991B1B; margin-top: 1px;">NAAC Grade</div>
                                                </td>

                                                <!-- Badge 4: TIER-1 -->
                                                <td align="center" valign="middle" style="padding: 2px 4px; border-right: 1px solid #E2E8F0;">
                                                    <div style="border: 1px dashed #0284C7; background-color: #E0F2FE; color: #0369A1; font-size: 7px; font-weight: bold; padding: 2px 3px; border-radius: 3px;">
                                                        TIER-1 ACCREDITATION
                                                    </div>
                                                </td>

                                                <!-- Badge 5: NBA -->
                                                <td align="center" valign="middle" style="padding: 2px 4px; border-right: 1px solid #E2E8F0;">
                                                    <div style="font-size: 12px; font-weight: 900; color: #0284C7; letter-spacing: 1px;">NBA</div>
                                                </td>

                                                <!-- Badge 6: CoE -->
                                                <td align="center" valign="middle" style="padding: 2px 4px;">
                                                    <div style="font-size: 11px; font-weight: 900; color: #0F172A;">CoE</div>
                                                    <div style="font-size: 6px; color: #64748B;">Centre of Excellence</div>
                                                </td>
                                            </tr>
                                        </table>

                                    </td>
                                </tr>
                            </table>
                        </td>
                    </tr>
                </table>
            </body>
            </html>
        """.trimIndent()
    }
}
