package com.transbuddy.app.utils

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.transbuddy.app.config.AppConfig
import com.transbuddy.app.models.Penalty
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

/**
 * Syncs penalties through the backend REST API.
 *
 * Database credentials must never be used by the Android client. Android
 * does not support the complete JDBC API and embedding DB credentials exposes
 * the database to every installed copy of the app.
 */
object CloudDatabaseManager {

    private const val TAG = "CloudDatabaseManager"
    private const val CONNECT_TIMEOUT_MS = 10_000
    private const val READ_TIMEOUT_MS = 15_000
    private val mainHandler = Handler(Looper.getMainLooper())

    fun isConfigured(): Boolean = AppConfig.API_BASE_URL.trim().isNotEmpty()

    fun syncPenaltyToCloud(
        penalty: Penalty,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        if (!isConfigured()) {
            onError("Cloud sync is not configured. Set API_BASE_URL to the deployed backend URL.")
            return
        }

        Thread {
            try {
                request("POST", endpoint("penalties"), penalty.toJson().toString())
                mainHandler.post(onSuccess)
            } catch (error: Exception) {
                Log.e(TAG, "Penalty sync failed", error)
                mainHandler.post { onError(error.userMessage()) }
            }
        }.start()
    }

    fun syncAllLocalPenaltiesToCloud(
        localPenalties: List<Penalty>,
        onComplete: (Int) -> Unit = {}
    ) {
        if (localPenalties.isEmpty() || !isConfigured()) {
            onComplete(0)
            return
        }

        Thread {
            val syncedCount = try {
                val payload = JSONObject().put(
                    "penalties",
                    JSONArray().apply { localPenalties.forEach { put(it.toJson()) } }
                )
                val response = request("POST", endpoint("penalties/batch"), payload.toString())
                JSONObject(response).optInt("synced", localPenalties.size)
            } catch (error: Exception) {
                Log.w(TAG, "Batch penalty sync failed: ${error.message}")
                0
            }
            mainHandler.post { onComplete(syncedCount) }
        }.start()
    }

    fun fetchAllPenaltiesFromCloud(onResult: (List<Penalty>) -> Unit) {
        if (!isConfigured()) {
            onResult(emptyList())
            return
        }

        Thread {
            val penalties = try {
                val records = JSONArray(request("GET", endpoint("penalties")))
                buildList {
                    for (index in 0 until records.length()) add(records.getJSONObject(index).toPenalty())
                }
            } catch (error: Exception) {
                Log.w(TAG, "Fetching cloud penalties failed: ${error.message}")
                emptyList()
            }
            mainHandler.post { onResult(penalties) }
        }.start()
    }

    private fun endpoint(path: String): String =
        "${AppConfig.API_BASE_URL.trimEnd('/')}/$path"

    private fun request(method: String, urlString: String, body: String? = null): String {
        val connection = (URL(urlString).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            setRequestProperty("Accept", "application/json")
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

    private fun Penalty.toJson() = JSONObject().apply {
        put("target_type", targetType)
        put("target_id", targetId)
        put("target_name", targetName.ifBlank { driverInfo })
        put("target_email", targetEmail)
        put("infraction_category", infractionCategory.ifBlank { title })
        put("infraction_reason", infractionReason.ifBlank { title })
        put("amount", amountNum)
        put("notes", notes)
        put("status", status)
        put("assigned_by", assignedBy)
        put("created_at", createdAt)
    }

    private fun JSONObject.toPenalty(): Penalty {
        val targetType = optString("target_type", "DRIVER")
        val targetName = optString("target_name", "Unknown")
        val reason = optString("infraction_reason", optString("infraction_category", "Penalty"))
        val amount = optDouble("amount", 0.0)
        return Penalty(
            id = optLong("id", 0), targetType = targetType, targetId = optString("target_id"),
            targetName = targetName, targetEmail = optString("target_email"),
            infractionCategory = optString("infraction_category"), infractionReason = reason,
            amountNum = amount, amount = "₹${String.format(Locale.US, "%.0f", amount)}",
            notes = optString("notes"), status = optString("status", "PENDING"),
            assignedBy = optString("assigned_by", "Android App"), createdAt = optString("created_at"),
            iconType = if (targetType.equals("STUDENT", true)) "student" else "speeding",
            title = reason, driverInfo = "$targetName • $targetType", isError = true
        )
    }

    private fun Exception.userMessage(): String = when (this) {
        is java.net.UnknownHostException -> "Backend URL could not be reached. Check API_BASE_URL and your network."
        is java.net.SocketTimeoutException -> "Backend request timed out. Please try again."
        else -> message ?: "Cloud sync failed. The penalty is saved locally."
    }
}
