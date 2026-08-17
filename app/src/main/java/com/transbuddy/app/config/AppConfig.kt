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
    // - For local Android Emulator: "http://10.0.2.2:3000"
    // - For local Physical Device: "http://<YOUR_LOCAL_IP>:3000" (e.g. "http://192.168.1.50:3000")
    // - For deployed Production server: "https://your-backend-domain.com"
    const val API_BASE_URL = "https://transbuddy-web-backend.onrender.com/api"
}
