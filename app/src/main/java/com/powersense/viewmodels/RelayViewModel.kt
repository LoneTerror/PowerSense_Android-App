package com.powersense.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.powersense.data.RelayDevice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit

data class TimerUiState(
    val totalMillis: Long,
    val remainingMillis: Long,
    val isRunning: Boolean = false
)

class RelayViewModel : ViewModel() {
    private val db = Firebase.firestore
    private val auth = Firebase.auth

    private val _relays = MutableStateFlow<List<RelayDevice>>(emptyList())
    val relays: StateFlow<List<RelayDevice>> = _relays.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _timerStates = MutableStateFlow<Map<String, TimerUiState>>(emptyMap())
    val timerStates: StateFlow<Map<String, TimerUiState>> = _timerStates.asStateFlow()
    private val timerJobs = mutableMapOf<String, Job>()

    init {
        fetchRelays()
    }

    // --- 1. CONFIGURATION ---
    fun fetchRelays() {
        val user = auth.currentUser
        if (user == null) {
            _isLoading.value = false
            return
        }

        db.collection("users").document(user.uid).collection("switches")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("RelayViewModel", "Listen failed.", e)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val loadedDevices = snapshot.documents.mapNotNull { doc ->
                        try {
                            val data = doc.data ?: return@mapNotNull null
                            val id = doc.id
                            val name = data["name"] as? String ?: "Switch"
                            val description = data["description"] as? String ?: ""
                            val connectionUrl = data["connectionUrl"] as? String ?: ""
                            val isOn = data["isOn"] as? Boolean ?: false
                            val isFavorite = data["isFavorite"] as? Boolean ?: false
                            val pin = (data["pin"] as? Number)?.toInt() ?: 0
                            val threshold = (data["threshold"] as? Number)?.toDouble()
                            val thresholdUnit = data["thresholdUnit"] as? String ?: "A"

                            RelayDevice(id, name, description, connectionUrl, isOn, isFavorite, pin, threshold, thresholdUnit)
                        } catch (e: Exception) {
                            null
                        }
                    }

                    val mergedDevices = loadedDevices.map { newDevice ->
                        val existing = _relays.value.find { it.id == newDevice.id }
                        if (existing != null) newDevice.copy(isOn = existing.isOn) else newDevice
                    }
                    _relays.value = mergedDevices
                    _isLoading.value = false
                }
            }
    }

    // --- 2. SYNC NAME WITH BACKEND ---
    private fun syncNameWithBackend(relayId: String, name: String, description: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (relayId.isBlank()) return@launch
                val endpoint = "https://backend.powersense.site/api/relays/$relayId/config"
                val url = URL(endpoint)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true

                val jsonBody = JSONObject().apply {
                    put("name", name)
                    put("description", description)
                }

                val writer = OutputStreamWriter(connection.outputStream)
                writer.write(jsonBody.toString())
                writer.flush()
                writer.close()

                if (connection.responseCode == 200) {
                    Log.d("RelayViewModel", "Synced name for Relay $relayId")
                }
                connection.disconnect()
            } catch (e: Exception) {
                Log.e("RelayViewModel", "Failed to sync name", e)
            }
        }
    }

    // --- 3. DATA MANAGEMENT ---
    fun addRelay(name: String, desc: String, url: String, threshold: Double?, unit: String) {
        val user = auth.currentUser ?: return
        val newDocRef = db.collection("users").document(user.uid).collection("switches").document()

        val newDevice = RelayDevice(id = newDocRef.id, name = name, description = desc, connectionUrl = url, isOn = false, isFavorite = false, threshold = threshold, thresholdUnit = unit)

        // Optimistic Update
        val currentList = _relays.value.toMutableList()
        currentList.add(newDevice)
        _relays.value = currentList

        val deviceMap = hashMapOf(
            "name" to name,
            "description" to desc,
            "connectionUrl" to url,
            "isOn" to false,
            "isFavorite" to false,
            "pin" to 0,
            "threshold" to threshold,
            "thresholdUnit" to unit
        )

        newDocRef.set(deviceMap).addOnSuccessListener {
            // NEW: Sync name to backend immediately
            syncNameWithBackend(url, name, desc)
        }.addOnFailureListener {
            val revertedList = _relays.value.toMutableList()
            revertedList.remove(newDevice)
            _relays.value = revertedList
            _errorMessage.value = "Failed to create switch."
        }
    }

    fun updateRelay(id: String, name: String, desc: String, url: String, threshold: Double?, unit: String) {
        val user = auth.currentUser ?: return
        _relays.value = _relays.value.map { if (it.id == id) it.copy(name = name, description = desc, connectionUrl = url, threshold = threshold, thresholdUnit = unit) else it }

        val updates = mapOf("name" to name, "description" to desc, "connectionUrl" to url, "threshold" to threshold, "thresholdUnit" to unit)
        db.collection("users").document(user.uid).collection("switches").document(id).update(updates)
            .addOnSuccessListener {
                // NEW: Sync name to backend on update
                syncNameWithBackend(url, name, desc)
            }
            .addOnFailureListener { _errorMessage.value = "Failed to update switch." }
    }

    fun deleteRelay(deviceId: String) {
        val user = auth.currentUser ?: return
        _relays.value = _relays.value.filter { it.id != deviceId }
        db.collection("users").document(user.uid).collection("switches").document(deviceId).delete()
    }

    // --- 4. CONTROL ---
    fun toggleRelay(passedDevice: RelayDevice) {
        val currentDevice = _relays.value.find { it.id == passedDevice.id } ?: passedDevice
        val newState = !currentDevice.isOn

        _relays.value = _relays.value.map { if (it.id == currentDevice.id) it.copy(isOn = newState) else it }

        viewModelScope.launch(Dispatchers.IO) {
            val success = sendApiRequest(currentDevice.connectionUrl, newState)
            if (!success) {
                _relays.value = _relays.value.map { if (it.id == currentDevice.id) it.copy(isOn = !newState) else it }
                _errorMessage.value = "Backend server is unreachable or turned off."
            }
        }
    }

    private fun sendApiRequest(relayId: String, isOn: Boolean): Boolean {
        return try {
            if (relayId.isBlank()) return false
            val endpoint = "https://backend.powersense.site/api/relays/$relayId/toggle"
            val url = URL(endpoint)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true
            connection.connectTimeout = 5000

            val jsonBody = JSONObject().apply { put("state", isOn) }
            val writer = OutputStreamWriter(connection.outputStream)
            writer.write(jsonBody.toString())
            writer.flush()
            writer.close()

            val responseCode = connection.responseCode
            connection.disconnect()
            responseCode == 200
        } catch (e: Exception) {
            false
        }
    }

    fun clearError() { _errorMessage.value = null }
    fun toggleFavorite(device: RelayDevice) {
        val user = auth.currentUser ?: return
        val newState = !device.isFavorite
        _relays.value = _relays.value.map { if (it.id == device.id) it.copy(isFavorite = newState) else it }
        db.collection("users").document(user.uid).collection("switches").document(device.id).update("isFavorite", newState)
    }

    // Timer logic ... (Keep existing timer methods unchanged)
    fun setTimer(device: RelayDevice, durationValue: Int, unit: String) {
        stopTimer(device.id)
        val totalMillis = when (unit) {
            "Seconds" -> TimeUnit.SECONDS.toMillis(durationValue.toLong())
            "Minutes" -> TimeUnit.MINUTES.toMillis(durationValue.toLong())
            "Hours"   -> TimeUnit.HOURS.toMillis(durationValue.toLong())
            "Days"    -> TimeUnit.DAYS.toMillis(durationValue.toLong())
            else      -> 0L
        }
        val newState = TimerUiState(totalMillis, totalMillis, isRunning = false)
        _timerStates.value = _timerStates.value.toMutableMap().apply { put(device.id, newState) }
    }

    fun startTimer(deviceId: String) {
        val currentState = _timerStates.value[deviceId] ?: return
        if (currentState.isRunning) return
        _timerStates.value = _timerStates.value.toMutableMap().apply { put(deviceId, currentState.copy(isRunning = true)) }
        val job = viewModelScope.launch {
            while (true) {
                delay(1000)
                val current = _timerStates.value[deviceId]
                if (current == null || !current.isRunning) break
                val newRemaining = current.remainingMillis - 1000
                if (newRemaining <= 0) {
                    val currentDevice = _relays.value.find { it.id == deviceId }
                    if (currentDevice != null) toggleRelay(currentDevice)
                    resetTimer(deviceId)
                    break
                } else {
                    _timerStates.value = _timerStates.value.toMutableMap().apply { put(deviceId, current.copy(remainingMillis = newRemaining)) }
                }
            }
        }
        timerJobs[deviceId] = job
    }

    fun stopTimer(deviceId: String) {
        timerJobs[deviceId]?.cancel()
        timerJobs.remove(deviceId)
        val currentState = _timerStates.value[deviceId] ?: return
        _timerStates.value = _timerStates.value.toMutableMap().apply { put(deviceId, currentState.copy(isRunning = false)) }
    }

    fun resetTimer(deviceId: String) {
        stopTimer(deviceId)
        val currentState = _timerStates.value[deviceId] ?: return
        _timerStates.value = _timerStates.value.toMutableMap().apply { put(deviceId, currentState.copy(remainingMillis = currentState.totalMillis, isRunning = false)) }
    }

    fun clearTimer(deviceId: String) {
        stopTimer(deviceId)
        _timerStates.value = _timerStates.value.toMutableMap().apply { remove(deviceId) }
    }
}