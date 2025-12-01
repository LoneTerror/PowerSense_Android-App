package com.powersense.data

import android.util.Log
import com.google.firebase.firestore.PropertyName
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// --- 1. Data Models ---

@Serializable
data class SensorData(
    val voltage: Double = 0.0,
    val current: Double = 0.0,
    val power: Double = 0.0,
    val energy: Double = 0.0,
    val timestamp: String = ""
)

// NEW: Data model for a single history point from backend
@Serializable
data class HistoryPoint(
    val timestamp: String,
    val value: Double
)

// NEW: Full response model for /api/sensor-data
@Serializable
data class HistoricalSensorData(
    val current: Double = 0.0,
    val avgCurrent: Double = 0.0,
    val voltage: Double = 0.0,
    val instPower: Double = 0.0,
    val avgPower: Double = 0.0,
    val currentHistory: List<HistoryPoint> = emptyList(),
    val avgCurrentHistory: List<HistoryPoint> = emptyList(),
    val voltageHistory: List<HistoryPoint> = emptyList(),
    val powerHistory: List<HistoryPoint> = emptyList()
)

@Serializable
data class RelayDevice(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val connectionUrl: String = "",

    @get:PropertyName("isOn")
    @set:PropertyName("isOn")
    var isOn: Boolean = false,

    @get:PropertyName("isFavorite")
    @set:PropertyName("isFavorite")
    var isFavorite: Boolean = false,

    val pin: Int = 0
)

@Serializable
data class ToggleRequest(
    val state: Boolean
)

// --- 2. Ktor Client Setup ---

object PowerSenseClient {
    private const val TAG = "PowerSenseClient"
    private const val BASE_URL = "https://backend.powersense.site"

    private val client = HttpClient(Android) {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            })
        }
    }

    // --- API Calls ---

    suspend fun getLatestReadings(): Result<SensorData> {
        val url = "$BASE_URL/api/sensors/latest"
        return try {
            val response: SensorData = client.get(url).body()
            Result.success(response)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch sensors: ${e.message}")
            Result.failure(e)
        }
    }

    // NEW: Fetch historical data for charts
    suspend fun getHistoricalData(intervalHours: Int = 24): Result<HistoricalSensorData> {
        val url = "$BASE_URL/api/sensor-data?interval=$intervalHours"
        Log.d(TAG, "Fetching history from: $url")
        return try {
            val response: HistoricalSensorData = client.get(url).body()
            Log.d(TAG, "Success fetching history. Points: ${response.powerHistory.size}")
            Result.success(response)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch history: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun toggleRelay(id: String, newState: Boolean): Result<Boolean> {
        val url = "$BASE_URL/api/relays/$id/toggle"
        return try {
            client.post(url) {
                contentType(ContentType.Application.Json)
                setBody(ToggleRequest(state = newState))
            }
            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to toggle relay: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun addRelay(device: RelayDevice): Result<RelayDevice> = Result.success(device)
    suspend fun updateRelay(device: RelayDevice): Result<Boolean> = Result.success(true)
    suspend fun deleteRelay(id: String): Result<Boolean> = Result.success(true)
}