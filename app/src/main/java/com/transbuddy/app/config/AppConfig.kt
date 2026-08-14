package com.transbuddy.app.config

/**
 * AppConfig — Central Configuration Object for Transbuddy Android App
 *
 * Holds database credentials and API key constants for network and maps integration.
 */
object AppConfig {
    // Database credentials (Aiven Cloud MySQL / PostgreSQL)
    const val DB_NAME     = "defaultdb"
    const val DB_HOST     = "transbuddy-db-1-transbuddy.e.aivencloud.com"
    const val DB_PORT     = 20742
    const val DB_USER     = "avnadmin"
    const val DB_PASSWORD = "AVNS_IxUzga3f6XjSmzEv6Ej"

    // Google Maps API Key
    const val GOOGLE_MAPS_API_KEY = "AIzaSyDe5pFoiZuP1zGA2yR8amD4iRl5nVlzfqg"
}
