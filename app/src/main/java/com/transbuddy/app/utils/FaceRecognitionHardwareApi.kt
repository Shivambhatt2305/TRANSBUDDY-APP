package com.transbuddy.app.utils

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.transbuddy.app.config.AppConfig
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

/**
 * FaceRecognitionHardwareApi — High Reliability OkHttp Client for Hugging Face Hardware Space
 * Handles direct multipart photo upload, multi-stage model warming retries, and comprehensive result parsing.
 */
object FaceRecognitionHardwareApi {

    private const val TAG = "FaceRecognitionHardware"
    val BASE_URL: String
        get() = AppConfig.FACE_RECOGNITION_API_BASE_URL.trimEnd('/')

    private val mainHandler = Handler(Looper.getMainLooper())

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(45, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(120, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    data class FaceMatchItem(
        val grNo: String,
        val studentName: String,
        val confidence: Double,
        val status: String,
        val category: String,
        val department: String,
        val semester: String,
        val shift: String,
        val feeStatus: String,
        val busId: String,
        val capturedB64: String
    ) {
        val isVerifiedStudent: Boolean
            get() = (status == "valid_with_bus" || status == "valid_without_bus" || status == "valid") && grNo.isNotBlank() && grNo != "—"
        
        val isFeeUnpaid: Boolean
            get() = status == "valid_without_bus" || category == "unpaid_fee" || category == "no_bus_policy"

        val isUnknown: Boolean
            get() = status == "not_uni" || category == "not_uni_student" || grNo.isBlank() || grNo == "—"
    }

    data class FaceScanResponse(
        val success: Boolean,
        val status: String,
        val busId: String,
        val faceCount: Int,
        val message: String,
        val results: List<FaceMatchItem>,
        val rawJson: String
    )

    /**
     * Fetch ALL 115+ fleet buses with fallback
     */
    fun fetchBuses(onResult: (List<String>) -> Unit) {
        val fullFleetBuses = buildList {
            add("All Buses (Auto-Detect)")
            for (i in 1..115) {
                add("Bus $i")
            }
            add("Bus 409")
        }

        val request = Request.Builder()
            .url("${AppConfig.API_BASE_URL.trimEnd('/')}/buses")
            .get()
            .build()

        okHttpClient.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                val list = mutableListOf("All Buses (Auto-Detect)")
                try {
                    val responseStr = response.body?.string() ?: ""
                    if (response.isSuccessful && responseStr.isNotBlank()) {
                        val array = if (responseStr.trim().startsWith("[")) {
                            JSONArray(responseStr)
                        } else {
                            val obj = JSONObject(responseStr)
                            obj.optJSONArray("data") ?: obj.optJSONArray("buses") ?: JSONArray()
                        }
                        for (i in 0 until array.length()) {
                            val item = array.getJSONObject(i)
                            val busNo = item.optString("bus_no", item.optString("bus_id", ""))
                            if (busNo.isNotBlank()) {
                                list.add("Bus $busNo")
                            }
                        }
                    }
                } catch (_: Exception) {
                    // Fallback to full list
                }

                mainHandler.post {
                    if (list.size > 2) {
                        onResult(list.distinct())
                    } else {
                        onResult(fullFleetBuses)
                    }
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                Log.d(TAG, "Backend bus fetch failed, using full fleet list: ${e.message}")
                mainHandler.post { onResult(fullFleetBuses) }
            }
        })
    }

    /**
     * Direct upload of photo and parameters to Face Recognition API.
     */
    fun uploadAndRecognize(
        imageBytes: ByteArray,
        busId: String,
        retryCount: Int = 4,
        totalRetries: Int = 4,
        onWarmingUp: ((attempt: Int, total: Int) -> Unit)? = null,
        onSuccess: (FaceScanResponse) -> Unit,
        onError: (String) -> Unit
    ) {
        val cleanBusId = busId.replace(Regex("[^0-9a-zA-Z_-]"), "").ifBlank { "102" }
        val uploadUrl = "$BASE_URL/upload"

        val timeStamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.format(Date())

        val imageReqBody = imageBytes.toRequestBody("image/jpeg".toMediaTypeOrNull(), 0, imageBytes.size)

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            // 1. Mandatory image file part
            .addFormDataPart("image", "camera_capture.jpg", imageReqBody)
            // 2. Parameters as String form data
            .addFormDataPart("bus_id", cleanBusId)
            .addFormDataPart("bus_no", cleanBusId)
            .addFormDataPart("gps_lat", "22.703052")
            .addFormDataPart("gps_lon", "70.793063")
            .addFormDataPart("stop_name", "Marwadi University")
            .addFormDataPart("stop_lat", "22.703052")
            .addFormDataPart("stop_lon", "70.793063")
            .addFormDataPart("stop_city", "Rajkot")
            .addFormDataPart("stop_state", "Gujarat")
            .addFormDataPart("stop_country", "India")
            .addFormDataPart("image_quality", "1.0")
            .addFormDataPart("captured_at", timeStamp)
            .addFormDataPart("pickup_id", "")
            .addFormDataPart("skip_gr_list", "")
            .build()

        val request = Request.Builder()
            .url(uploadUrl)
            .post(requestBody)
            .build()

        Log.d(TAG, "Uploading photo to $uploadUrl (bus_id: $cleanBusId, size: ${imageBytes.size} bytes, retryLeft: $retryCount)")

        okHttpClient.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                val statusCode = response.code
                val responseStr = response.body?.string() ?: ""

                Log.d(TAG, "Server response ($statusCode): $responseStr")

                // Handle 503/504 Space starting / model loading / sleep state
                val isSpaceStarting = statusCode == 503 ||
                        statusCode == 504 ||
                        responseStr.contains("Face model is still loading", ignoreCase = true) ||
                        responseStr.contains("\"status\":\"starting\"", ignoreCase = true) ||
                        responseStr.contains("is building", ignoreCase = true) ||
                        responseStr.contains("is sleeping", ignoreCase = true)

                if (isSpaceStarting && retryCount > 0) {
                    val attempt = (totalRetries - retryCount + 1)
                    mainHandler.post { onWarmingUp?.invoke(attempt, totalRetries) }
                    try {
                        Thread.sleep(6000)
                    } catch (_: InterruptedException) {}
                    uploadAndRecognize(imageBytes, busId, retryCount - 1, totalRetries, onWarmingUp, onSuccess, onError)
                    return
                }

                if (response.isSuccessful && responseStr.isNotBlank()) {
                    try {
                        val json = JSONObject(responseStr)
                        val status = json.optString("status", "ok")
                        val message = json.optString("message", "Scan processed")
                        val faceCount = json.optInt("face_count", 0)
                        val resultsArr = json.optJSONArray("results")

                        val resultList = mutableListOf<FaceMatchItem>()
                        if (resultsArr != null) {
                            for (i in 0 until resultsArr.length()) {
                                val r = resultsArr.getJSONObject(i)
                                val rawGr = r.optString("gr_no", r.optString("candidate_gr", "")).trim()
                                val gr = if (rawGr.equals("null", ignoreCase = true)) "" else rawGr

                                val rawName = r.optString("display_name", r.optString("student_name", r.optString("name", ""))).trim()
                                val name = if (rawName.equals("null", ignoreCase = true)) "" else rawName

                                val confidence = r.optDouble("confidence", 0.0)
                                val resStatus = r.optString("status", r.optString("state_label", "unknown"))
                                val category = r.optString("category", "")
                                val dept = r.optString("department", "").let { if (it.equals("null", ignoreCase = true)) "" else it }
                                val sem = r.optString("semester", "").let { if (it.equals("null", ignoreCase = true)) "" else it }
                                val shift = r.optString("shift", "").let { if (it.equals("null", ignoreCase = true)) "" else it }
                                val fee = r.optString("fee_status", "").let { if (it.equals("null", ignoreCase = true)) "" else it }
                                val bus = r.optString("bus_id", cleanBusId).let { if (it.equals("null", ignoreCase = true)) cleanBusId else it }
                                val capB64 = r.optString("captured_b64", "")

                                val finalGr = if (gr.isNotBlank()) gr else if (resStatus == "not_uni") "—" else ""
                                val finalName = if (name.isNotBlank()) name else if (resStatus == "not_uni") "Unknown Person" else "Rider"

                                resultList.add(
                                    FaceMatchItem(
                                        grNo = finalGr,
                                        studentName = finalName,
                                        confidence = confidence,
                                        status = resStatus,
                                        category = category,
                                        department = dept,
                                        semester = sem,
                                        shift = shift,
                                        feeStatus = fee,
                                        busId = bus,
                                        capturedB64 = capB64
                                    )
                                )
                            }
                        }

                        mainHandler.post {
                            onSuccess(
                                FaceScanResponse(
                                    success = true,
                                    status = status,
                                    busId = cleanBusId,
                                    faceCount = faceCount,
                                    message = message,
                                    results = resultList,
                                    rawJson = responseStr
                                )
                            )
                        }
                    } catch (e: Exception) {
                        mainHandler.post {
                            onError("Failed to parse server response: ${e.message}")
                        }
                    }
                } else {
                    mainHandler.post {
                        onError("Server Error ($statusCode): ${responseStr.ifBlank { response.message }}")
                    }
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "Request failed: ${e.message}", e)
                mainHandler.post {
                    val msg = when {
                        e is java.net.UnknownHostException ->
                            "Cannot resolve host \"$BASE_URL\". Please check your phone's internet connection (Wi-Fi/Mobile Data) and Private DNS settings."
                        e is java.net.SocketTimeoutException ->
                            "Connection timed out reaching AI server ($uploadUrl). The server may be cold-starting; please retry."
                        else ->
                            "Upload failed: ${e.localizedMessage ?: e.message}"
                    }
                    onError(msg)
                }
            }
        })
    }
}
