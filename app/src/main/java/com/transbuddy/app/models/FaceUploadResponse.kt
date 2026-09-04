package com.transbuddy.app.models

import com.google.gson.annotations.SerializedName

data class FaceUploadResponse(
    @SerializedName("face_count") val faceCount: Int = 0,
    @SerializedName("is_idle") val isIdle: Boolean = false,
    @SerializedName("multi_face") val multiFace: Boolean = false,
    @SerializedName("status") val status: String? = null,       // "no_face", "busy", "error", etc.
    @SerializedName("message") val message: String? = null,
    @SerializedName("results") val results: List<FaceResult>? = null,
    @SerializedName("summary") val summary: ScanSummary? = null,
    @SerializedName("timestamp") val timestamp: String? = null
)

data class FaceResult(
    @SerializedName("face_index") val faceIndex: Int = 0,
    @SerializedName("status") val status: String = "",        // "valid_with_bus", "valid_without_bus", "not_uni", etc.
    @SerializedName("route") val route: String = "",
    @SerializedName("gr_no") val grNo: String? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("enrollment_no") val enrollmentNo: String? = null,
    @SerializedName("confidence") val confidence: Double? = null,
    @SerializedName("message") val message: String? = null
) {
    val accessStatus: FaceAccessStatus
        get() = FaceAccessStatus.from(status)
}

data class ScanSummary(
    @SerializedName("valid_with_bus") val validWithBus: Int = 0,
    @SerializedName("unpaid") val unpaid: Int = 0,
    @SerializedName("invalid") val invalid: Int = 0,
    @SerializedName("not_uni") val notUni: Int = 0,
    @SerializedName("cooldown") val cooldown: Int = 0
)

enum class FaceAccessStatus(
    val code: String,
    val title: String,
    val emoji: String,
    val description: String
) {
    ACCESS_GRANTED("valid_with_bus", "ACCESS GRANTED", "🟢", "Student verified & bus fee paid."),
    UNPAID_FEE("valid_without_bus", "UNPAID FEE", "🟠", "Student verified, but fee pending."),
    INVALID("invalid_person", "INVALID", "🔴", "No bus policy / not in database."),
    UNKNOWN("not_uni", "UNKNOWN", "❓", "Face not recognized as any registered student."),
    ON_COOLDOWN("on_cooldown", "ALREADY CHECKED", "⏳", "Student was already marked present this shift."),
    OTHER("other", "OTHER", "ℹ️", "Status code not mapped.");

    companion object {
        fun from(status: String?): FaceAccessStatus {
            return when (status?.lowercase()?.trim()) {
                "valid_with_bus" -> ACCESS_GRANTED
                "valid_without_bus", "unpaid" -> UNPAID_FEE
                "invalid_person", "invalid_database", "invalid" -> INVALID
                "not_uni" -> UNKNOWN
                "on_cooldown", "cooldown" -> ON_COOLDOWN
                else -> OTHER
            }
        }
    }
}
