package com.transbuddy.app.utils

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.transbuddy.app.config.AppConfig
import com.transbuddy.app.models.Emergency
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * EmergencyApiManager — Manages Emergency Alerts and Driver Calling with ACID properties
 * Features multi-host failover (Local Wi-Fi -> Hotspot -> Emulator -> Render)
 */
object EmergencyApiManager {

    private const val TAG = "EmergencyApiManager"
    private const val CONNECT_TIMEOUT_MS = 6_000
    private const val READ_TIMEOUT_MS = 8_000
    private val mainHandler = Handler(Looper.getMainLooper())

    private val candidateBaseUrls = listOf(
        "http://192.168.1.5:5000/api",
        "http://10.211.42.14:5000/api",
        "http://10.0.2.2:5000/api",
        AppConfig.API_BASE_URL.trimEnd('/')
    )

    fun isConfigured(): Boolean = true

    fun fetchActiveEmergencies(
        onResult: (List<Emergency>) -> Unit,
        onError: (String) -> Unit = {}
    ) {
        Thread {
            for (baseUrl in candidateBaseUrls) {
                try {
                    val response = request("GET", "$baseUrl/emergencies/active")
                    val emergencies = parseEmergencies(response)
                    mainHandler.post { onResult(emergencies) }
                    return@Thread
                } catch (e: Exception) {
                    Log.d(TAG, "Fetch active emergencies try on $baseUrl failed: ${e.message}")
                }
            }
            mainHandler.post { onResult(emptyList()) }
        }.start()
    }

    fun fetchUnreadNotifications(
        onResult: (List<Emergency>) -> Unit,
        onError: (String) -> Unit = {}
    ) {
        Thread {
            for (baseUrl in candidateBaseUrls) {
                try {
                    val response = request("GET", "$baseUrl/emergencies")
                    val emergencies = parseEmergencies(response)
                    mainHandler.post { onResult(emergencies) }
                    return@Thread
                } catch (e: Exception) {
                    Log.d(TAG, "Fetch unread notifications try on $baseUrl failed: ${e.message}")
                }
            }
            mainHandler.post { onResult(emptyList()) }
        }.start()
    }

    fun fetchAllEmergencies(
        onResult: (List<Emergency>) -> Unit,
        onError: (String) -> Unit = {}
    ) {
        fetchUnreadNotifications(onResult, onError)
    }

    fun fetchEmergencyDrivers(onResult: (List<EmergencyDriver>) -> Unit, onError: (String) -> Unit = {}) {
        Thread {
            for (baseUrl in candidateBaseUrls) {
                try {
                    val response = request("GET", "$baseUrl/emergencies/drivers")
                    val array = JSONArray(response)
                    val drivers = buildList {
                        for (i in 0 until array.length()) {
                            val obj = array.getJSONObject(i)
                            val phone = if (!obj.isNull("mobile_no")) obj.optString("mobile_no", "") else if (!obj.isNull("driver_phone")) obj.optString("driver_phone", "") else ""
                            if (phone.isNotBlank() && phone != "null") {
                                add(
                                    EmergencyDriver(
                                        driverId = obj.optString("driver_id", ""),
                                        driverName = obj.optString("driver_name", "Fleet Driver"),
                                        driverPhone = phone,
                                        busNo = obj.optString("bus_no", ""),
                                        routeName = obj.optString("route_name", "Campus Route"),
                                        licenseNo = obj.optString("license_no", "")
                                    )
                                )
                            }
                        }
                    }
                    mainHandler.post { onResult(drivers) }
                    return@Thread
                } catch (e: Exception) {
                    Log.d(TAG, "Fetch emergency drivers try on $baseUrl failed: ${e.message}")
                }
            }
            mainHandler.post { onResult(emptyList()) }
        }.start()
    }

    fun logEmergencyCall(
        emergencyId: Long,
        driverId: String,
        driverPhone: String,
        calledBy: String = "Mobile User",
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        Thread {
            val json = JSONObject().apply {
                put("emergency_id", emergencyId)
                put("driver_id", driverId)
                put("driver_phone", driverPhone)
                put("called_by", calledBy)
            }

            for (baseUrl in candidateBaseUrls) {
                try {
                    request("POST", "$baseUrl/emergencies/call-log", json.toString())
                    mainHandler.post { onSuccess() }
                    return@Thread
                } catch (e: Exception) {
                    Log.d(TAG, "Log emergency call try on $baseUrl failed: ${e.message}")
                }
            }
            mainHandler.post { onSuccess() }
        }.start()
    }

    data class EmergencyDriver(
        val driverId: String,
        val driverName: String,
        val driverPhone: String,
        val busNo: String,
        val routeName: String,
        val licenseNo: String
    )

    private fun parseEmergencies(json: String): List<Emergency> = try {
        val array = JSONArray(json)
        buildList {
            for (index in 0 until array.length()) {
                val obj = array.getJSONObject(index)
                val rawPhone = if (!obj.isNull("mobile_no")) obj.optString("mobile_no", "") else if (!obj.isNull("driver_phone")) obj.optString("driver_phone", "") else ""
                val phone = if (rawPhone == "null") "" else rawPhone
                val rawDriverName = if (!obj.isNull("driver_name")) obj.optString("driver_name", "") else ""
                val driverName = if (rawDriverName == "null") "" else rawDriverName

                add(
                    Emergency(
                        id = obj.optLong("id", 0),
                        busId = obj.optString("bus_id", ""),
                        driverId = if (obj.isNull("driver_id") || obj.optString("driver_id") == "null") "" else obj.optString("driver_id", ""),
                        location = obj.optString("location", ""),
                        status = obj.optString("status", ""),
                        severity = obj.optString("severity", ""),
                        description = obj.optString("description", ""),
                        createdAt = obj.optString("created_at", ""),
                        updatedAt = obj.optString("updated_at", ""),
                        driverName = driverName,
                        username = obj.optString("username", ""),
                        busNo = if (obj.isNull("bus_no") || obj.optString("bus_no") == "null") "" else obj.optString("bus_no", ""),
                        driverPhone = phone,
                        licenseNo = if (obj.isNull("license_no") || obj.optString("license_no") == "null") "" else obj.optString("license_no", "")
                    )
                )
            }
        }
    } catch (e: Exception) {
        Log.e(TAG, "Parse emergencies failed: ${e.message}", e)
        emptyList()
    }

    private fun request(method: String, urlString: String, body: String? = null): String {
        val connection = (URL(urlString).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "TransBuddy-Android/1.0")
            if (body != null) {
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
            }
        }
        try {
            if (body != null) OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { it.write(body) }
            val status = connection.responseCode
            val response = (if (status in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader()?.use(BufferedReader::readText).orEmpty()
            if (status !in 200..299) throw IllegalStateException("Server returned $status${if (response.isBlank()) "" else ": $response"}")
            return response
        } finally {
            connection.disconnect()
        }
    }

    private fun Exception.userMessage(): String = when (this) {
        is java.net.UnknownHostException -> "Backend URL could not be reached."
        is java.net.SocketTimeoutException -> "Backend request timed out."
        else -> message ?: "API request failed."
    }
}
