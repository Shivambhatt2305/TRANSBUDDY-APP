package com.transbuddy.app.network

import com.transbuddy.app.config.AppConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    private const val CONNECT_TIMEOUT_SEC = 45L
    private const val READ_TIMEOUT_SEC = 120L
    private const val WRITE_TIMEOUT_SEC = 120L

    private val okHttpClient: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        OkHttpClient.Builder()
            .connectTimeout(CONNECT_TIMEOUT_SEC, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT_SEC, TimeUnit.SECONDS)
            .writeTimeout(WRITE_TIMEOUT_SEC, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .build()
    }

    val faceHardwareApiService: FaceHardwareApiService by lazy {
        Retrofit.Builder()
            .baseUrl(AppConfig.FACE_RECOGNITION_API_BASE_URL.let { if (it.endsWith("/")) it else "$it/" })
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(FaceHardwareApiService::class.java)
    }

    fun getFaceHardwareApiService(customBaseUrl: String? = null): FaceHardwareApiService {
        val url = (customBaseUrl ?: AppConfig.FACE_RECOGNITION_API_BASE_URL).let {
            if (it.endsWith("/")) it else "$it/"
        }
        return Retrofit.Builder()
            .baseUrl(url)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(FaceHardwareApiService::class.java)
    }
}
