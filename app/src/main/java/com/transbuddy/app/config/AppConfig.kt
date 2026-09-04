package com.transbuddy.app.config

/**
 * AppConfig — Central Configuration Object for Transbuddy Android App
 *
 * Holds public client configuration only. Database credentials belong in the backend.
 */
object AppConfig {
    // Google Maps API Key
    const val GOOGLE_MAPS_API_KEY = "AIzaSyDe5pFoiZuP1zGA2yR8amD4iRl5nVlzfqg"

    // Base URL of the backend API server.
    // - For local Android Emulator: "http://10.0.2.2:5000/api"
    // - For deployed Production server: "https://transbuddy-web-backend.onrender.com/api"
    const val API_BASE_URL = "https://transbuddy-web-backend.onrender.com/api"

    // Base URL of the AI Face Recognition Hardware Service
    const val FACE_RECOGNITION_API_BASE_URL = "https://shivam2307-transbuddy-hardware.hf.space"
}

