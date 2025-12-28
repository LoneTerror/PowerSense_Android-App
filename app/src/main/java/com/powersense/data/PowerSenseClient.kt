// FIX: File annotations must go BEFORE the package declaration
@file:OptIn(kotlinx.serialization.InternalSerializationApi::class)

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
import kotlinx.serialization.SerialName
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

@Serializable
data class HistoryPoint(
    val timestamp: String,
    val value: Double
)

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

// Data model for Pie Chart (Relay Usage)
@Serializable
data class RelayUsage(
    @SerialName("relay1") val relay1Hours: Double = 0.0,
    @SerialName("relay2") val relay2Hours: Double = 0.0
)

@Serializable
data class RelayDevice(
    var id: String = "",
    var name: String = "",
    var description: String = "",
    var connectionUrl: String = "",

    @get:PropertyName("isOn")
    @set:PropertyName("isOn")
    var isOn: Boolean = false,

    @get:PropertyName("isFavorite")
    @set:PropertyName("isFavorite")
    var isFavorite: Boolean = false,

    var pin: Int = 0,
    var threshold: Double? = null,
    var thresholdUnit: String = "A"
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

    suspend fun getHistoricalData(intervalHours: Int = 24): Result<HistoricalSensorData> {
        val url = "$BASE_URL/api/sensor-data?interval=$intervalHours"
        return try {
            val response: HistoricalSensorData = client.get(url).body()
            Result.success(response)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch history: ${e.message}")
            Result.failure(e)
        }
    }

    // NEW: Fetch Pie Chart Data from Backend
    @Suppress("unused")
    suspend fun getRelayUsage(intervalHours: Int = 24): Result<RelayUsage> {
        val url = "$BASE_URL/api/relay-usage?interval=$intervalHours"
        return try {
            val response: RelayUsage = client.get(url).body()
            Result.success(response)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch relay usage: ${e.message}")
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

    // Stub functions without 'suspend' to avoid warnings
    @Suppress("unused")
    fun addRelay(device: RelayDevice): Result<RelayDevice> = Result.success(device)

    @Suppress("unused")
    fun updateRelay(device: RelayDevice): Result<Boolean> = Result.success(true)

    @Suppress("unused")
    fun deleteRelay(id: String): Result<Boolean> = Result.success(true)
}