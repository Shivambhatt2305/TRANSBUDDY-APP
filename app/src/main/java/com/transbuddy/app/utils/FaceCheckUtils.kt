package com.transbuddy.app.utils

import android.util.Log
import com.transbuddy.app.models.FaceUploadResponse
import com.transbuddy.app.network.FaceHardwareApiService
import com.transbuddy.app.network.RetrofitClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val TAG = "FaceCheckUtils"

/**
 * Prepares multipart request data and invokes FaceHardwareApiService to verify faces in an image file.
 * All fields including busId are guaranteed to be serialized as String RequestBody.
 */
fun prepareAndSendFaceCheck(
    apiService: FaceHardwareApiService = RetrofitClient.faceHardwareApiService,
    imageFile: File,
    busId: String = "102",
    currentLat: Double,
    currentLon: Double,
    stopName: String,
    stopLat: Double,
    stopLon: Double,
    pickupId: String = "",
    skipGrNumbers: List<String> = emptyList(),
    stopCity: String = "Rajkot",
    stopState: String = "Gujarat",
    stopCountry: String = "India",
    onResult: (FaceUploadResponse?) -> Unit
) {
    val textPlain = "text/plain".toMediaTypeOrNull()

    // 1. Prepare image part
    val imageReqBody = imageFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
    val imagePart = MultipartBody.Part.createFormData("image", imageFile.name, imageReqBody)

    // 2. Prepare text parts — ensure bus_id is strictly a String
    val cleanBusId = busId.replace(Regex("[^0-9a-zA-Z_-]"), "").ifBlank { "102" }
    val busIdBody = cleanBusId.toRequestBody(textPlain)
    val busNoBody = cleanBusId.toRequestBody(textPlain)

    val latBody = currentLat.toString().toRequestBody(textPlain)
    val lonBody = currentLon.toString().toRequestBody(textPlain)
    val stopNameBody = stopName.toRequestBody(textPlain)
    val stopLatBody = stopLat.toString().toRequestBody(textPlain)
    val stopLonBody = stopLon.toString().toRequestBody(textPlain)

    val timeStamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(Date())
    val capturedAtBody = timeStamp.toRequestBody(textPlain)
    val qualityBody = "1.0".toRequestBody(textPlain)
    val pickupIdBody = pickupId.toRequestBody(textPlain)
    val skipListBody = skipGrNumbers.joinToString(",").toRequestBody(textPlain)
    val cityBody = stopCity.toRequestBody(textPlain)
    val stateBody = stopState.toRequestBody(textPlain)
    val countryBody = stopCountry.toRequestBody(textPlain)

    // 3. Execute via Coroutine
    CoroutineScope(Dispatchers.IO).launch {
        try {
            val response = apiService.verifyFace(
                image = imagePart,
                gpsLat = latBody,
                gpsLon = lonBody,
                stopName = stopNameBody,
                stopLat = stopLatBody,
                stopLon = stopLonBody,
                capturedAt = capturedAtBody,
                imageQuality = qualityBody,
                pickupId = pickupIdBody,
                skipGrList = skipListBody,
                stopCity = cityBody,
                stopState = stateBody,
                stopCountry = countryBody,
                busId = busIdBody,
                busNo = busNoBody
            )
            val resultBody = if (response.isSuccessful) response.body() else null
            if (!response.isSuccessful) {
                Log.e(TAG, "Face check failed: HTTP ${response.code()} - ${response.errorBody()?.string()}")
            }
            withContext(Dispatchers.Main) {
                onResult(resultBody)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during face check upload: ${e.message}", e)
            withContext(Dispatchers.Main) {
                onResult(null)
            }
        }
    }
}

/**
 * Overloaded variant accepting image ByteArray directly (e.g., from camera Bitmap).
 * All text fields including busId are guaranteed to be serialized as String RequestBody.
 */
fun prepareAndSendFaceCheck(
    apiService: FaceHardwareApiService = RetrofitClient.faceHardwareApiService,
    imageBytes: ByteArray,
    imageFileName: String = "camera_capture.jpg",
    busId: String = "102",
    currentLat: Double,
    currentLon: Double,
    stopName: String,
    stopLat: Double,
    stopLon: Double,
    pickupId: String = "",
    skipGrNumbers: List<String> = emptyList(),
    stopCity: String = "Rajkot",
    stopState: String = "Gujarat",
    stopCountry: String = "India",
    onResult: (FaceUploadResponse?) -> Unit
) {
    val textPlain = "text/plain".toMediaTypeOrNull()
    val imageReqBody = imageBytes.toRequestBody("image/jpeg".toMediaTypeOrNull(), 0, imageBytes.size)
    val imagePart = MultipartBody.Part.createFormData("image", imageFileName, imageReqBody)

    val cleanBusId = busId.replace(Regex("[^0-9a-zA-Z_-]"), "").ifBlank { "102" }
    val busIdBody = cleanBusId.toRequestBody(textPlain)
    val busNoBody = cleanBusId.toRequestBody(textPlain)

    val latBody = currentLat.toString().toRequestBody(textPlain)
    val lonBody = currentLon.toString().toRequestBody(textPlain)
    val stopNameBody = stopName.toRequestBody(textPlain)
    val stopLatBody = stopLat.toString().toRequestBody(textPlain)
    val stopLonBody = stopLon.toString().toRequestBody(textPlain)

    val timeStamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(Date())
    val capturedAtBody = timeStamp.toRequestBody(textPlain)
    val qualityBody = "1.0".toRequestBody(textPlain)
    val pickupIdBody = pickupId.toRequestBody(textPlain)
    val skipListBody = skipGrNumbers.joinToString(",").toRequestBody(textPlain)
    val cityBody = stopCity.toRequestBody(textPlain)
    val stateBody = stopState.toRequestBody(textPlain)
    val countryBody = stopCountry.toRequestBody(textPlain)

    CoroutineScope(Dispatchers.IO).launch {
        try {
            val response = apiService.verifyFace(
                image = imagePart,
                gpsLat = latBody,
                gpsLon = lonBody,
                stopName = stopNameBody,
                stopLat = stopLatBody,
                stopLon = stopLonBody,
                capturedAt = capturedAtBody,
                imageQuality = qualityBody,
                pickupId = pickupIdBody,
                skipGrList = skipListBody,
                stopCity = cityBody,
                stopState = stateBody,
                stopCountry = countryBody,
                busId = busIdBody,
                busNo = busNoBody
            )
            val resultBody = if (response.isSuccessful) response.body() else null
            if (!response.isSuccessful) {
                Log.e(TAG, "Face check failed: HTTP ${response.code()} - ${response.errorBody()?.string()}")
            }
            withContext(Dispatchers.Main) {
                onResult(resultBody)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during face check upload: ${e.message}", e)
            withContext(Dispatchers.Main) {
                onResult(null)
            }
        }
    }
}

/**
 * Suspend function for direct use within lifecycleScope / viewModelScope.
 */
suspend fun sendFaceCheck(
    apiService: FaceHardwareApiService = RetrofitClient.faceHardwareApiService,
    imageFile: File,
    busId: String = "102",
    currentLat: Double,
    currentLon: Double,
    stopName: String,
    stopLat: Double,
    stopLon: Double,
    pickupId: String = "",
    skipGrNumbers: List<String> = emptyList(),
    stopCity: String = "Rajkot",
    stopState: String = "Gujarat",
    stopCountry: String = "India"
): FaceUploadResponse? = withContext(Dispatchers.IO) {
    val textPlain = "text/plain".toMediaTypeOrNull()
    val imageReqBody = imageFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
    val imagePart = MultipartBody.Part.createFormData("image", imageFile.name, imageReqBody)

    val cleanBusId = busId.replace(Regex("[^0-9a-zA-Z_-]"), "").ifBlank { "102" }
    val busIdBody = cleanBusId.toRequestBody(textPlain)
    val busNoBody = cleanBusId.toRequestBody(textPlain)

    val latBody = currentLat.toString().toRequestBody(textPlain)
    val lonBody = currentLon.toString().toRequestBody(textPlain)
    val stopNameBody = stopName.toRequestBody(textPlain)
    val stopLatBody = stopLat.toString().toRequestBody(textPlain)
    val stopLonBody = stopLon.toString().toRequestBody(textPlain)

    val timeStamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(Date())
    val capturedAtBody = timeStamp.toRequestBody(textPlain)
    val qualityBody = "1.0".toRequestBody(textPlain)
    val pickupIdBody = pickupId.toRequestBody(textPlain)
    val skipListBody = skipGrNumbers.joinToString(",").toRequestBody(textPlain)
    val cityBody = stopCity.toRequestBody(textPlain)
    val stateBody = stopState.toRequestBody(textPlain)
    val countryBody = stopCountry.toRequestBody(textPlain)

    try {
        val response = apiService.verifyFace(
            image = imagePart,
            gpsLat = latBody,
            gpsLon = lonBody,
            stopName = stopNameBody,
            stopLat = stopLatBody,
            stopLon = stopLonBody,
            capturedAt = capturedAtBody,
            imageQuality = qualityBody,
            pickupId = pickupIdBody,
            skipGrList = skipListBody,
            stopCity = cityBody,
            stopState = stateBody,
            stopCountry = countryBody,
            busId = busIdBody,
            busNo = busNoBody
        )
        if (response.isSuccessful) response.body() else null
    } catch (e: Exception) {
        Log.e(TAG, "sendFaceCheck failed: ${e.message}", e)
        null
    }
}
