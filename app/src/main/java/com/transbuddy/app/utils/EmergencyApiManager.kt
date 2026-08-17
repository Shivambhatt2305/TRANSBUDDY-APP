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

object EmergencyApiManager {

    private const val TAG = "EmergencyApiManager"
    private const val CONNECT_TIMEOUT_MS = 10_000
    private const val READ_TIMEOUT_MS = 15_000
    private val mainHandler = Handler(Looper.getMainLooper())

    fun isConfigured(): Boolean = AppConfig.API_BASE_URL.trim().isNotEmpty()

    fun fetchActiveEmergencies(onResult: (List<Emergency>) -> Unit, onError: (String) -> Unit = {}) {
        if (!isConfigured()) {
            onError("API base URL is not configured.")
            return
        }

        Thread {
            try {
                val response = request("GET", endpoint("emergencies/active"))
                val emergencies = parseEmergencies(response)
                mainHandler.post { onResult(emergencies) }
            } catch (e: Exception) {
                Log.e(TAG, "Fetch active emergencies failed", e)
                mainHandler.post { onError(e.userMessage()) }
            }
        }.start()
    }

    fun fetchAllEmergencies(onResult: (List<Emergency>) -> Unit, onError: (String) -> Unit = {}) {
        if (!isConfigured()) {
            onError("API base URL is not configured.")
            return
        }

        Thread {
            try {
                val response = request("GET", endpoint("emergencies"))
                val emergencies = parseEmergencies(response)
                mainHandler.post { onResult(emergencies) }
            } catch (e: Exception) {
                Log.e(TAG, "Fetch all emergencies failed", e)
                mainHandler.post { onError(e.userMessage()) }
            }
        }.start()
    }

    fun fetchUnreadNotifications(onResult: (List<Emergency>) -> Unit, onError: (String) -> Unit = {}) {
        if (!isConfigured()) {
            onError("API base URL is not configured.")
            return
        }

        Thread {
            try {
                val response = request("GET", endpoint("notifications/unread"))
                val notifications = parseEmergencies(response)
                mainHandler.post { onResult(notifications) }
            } catch (e: Exception) {
                Log.e(TAG, "Fetch unread notifications failed", e)
                mainHandler.post { onError(e.userMessage()) }
            }
        }.start()
    }

    private fun parseEmergencies(json: String): List<Emergency> = try {
        val array = JSONArray(json)
        buildList {
            for (index in 0 until array.length()) {
                val obj = array.getJSONObject(index)
                add(
                    Emergency(
                        id = obj.optLong("id", 0),
                        busId = obj.optString("bus_id", ""),
                        driverId = obj.optString("driver_id", ""),
                        location = obj.optString("location", ""),
                        status = obj.optString("status", ""),
                        severity = obj.optString("severity", ""),
                        description = obj.optString("description", ""),
                        createdAt = obj.optString("created_at", ""),
                        updatedAt = obj.optString("updated_at", ""),
                        driverName = obj.optString("driver_name", ""),
                        username = obj.optString("username", ""),
                        busNo = obj.optString("bus_no", "")
                    )
                )
            }
        }
    } catch (e: Exception) {
        Log.e(TAG, "Parse emergencies failed: ${e.message}", e)
        emptyList()
    }

    private fun endpoint(path: String): String =
        "${AppConfig.API_BASE_URL.trimEnd('/')}/$path"

    private fun request(method: String, urlString: String, body: String? = null): String {
        android.util.Log.d(TAG, "HTTP $method $urlString")
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
            android.util.Log.d(TAG, "HTTP status: $status, response: $response")
            if (status !in 200..299) throw IllegalStateException("Server returned $status${if (response.isBlank()) "" else ": $response"}")
            return response
        } finally {
            connection.disconnect()
        }
    }

    private fun Exception.userMessage(): String = when (this) {
        is java.net.UnknownHostException -> "Backend URL could not be reached. Check API_BASE_URL and your network."
        is java.net.SocketTimeoutException -> "Backend request timed out. Please try again."
        else -> message ?: "API request failed."
    }
}
