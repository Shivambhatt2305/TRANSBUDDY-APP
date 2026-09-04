package com.transbuddy.app.network

import com.transbuddy.app.models.FaceUploadResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface FaceHardwareApiService {

    @Multipart
    @POST("upload")
    suspend fun verifyFace(
        @Part image: MultipartBody.Part,
        @Part("gps_lat") gpsLat: RequestBody,
        @Part("gps_lon") gpsLon: RequestBody,
        @Part("stop_name") stopName: RequestBody,
        @Part("stop_lat") stopLat: RequestBody,
        @Part("stop_lon") stopLon: RequestBody,
        @Part("captured_at") capturedAt: RequestBody? = null,
        @Part("image_quality") imageQuality: RequestBody? = null,
        @Part("pickup_id") pickupId: RequestBody? = null,
        @Part("skip_gr_list") skipGrList: RequestBody? = null,
        @Part("stop_city") stopCity: RequestBody? = null,
        @Part("stop_state") stopState: RequestBody? = null,
        @Part("stop_country") stopCountry: RequestBody? = null,
        @Part("bus_id") busId: RequestBody? = null,
        @Part("bus_no") busNo: RequestBody? = null
    ): Response<FaceUploadResponse>
}
