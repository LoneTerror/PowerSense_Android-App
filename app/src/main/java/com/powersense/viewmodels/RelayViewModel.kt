package com.powersense.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.powersense.data.PowerSenseClient
import com.powersense.data.RelayDevice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RelayViewModel : ViewModel() {

    // Firebase instances
    private val auth = Firebase.auth
    private val db = Firebase.firestore

    private val _relays = MutableStateFlow<List<RelayDevice>>(emptyList())
    val relays: StateFlow<List<RelayDevice>> = _relays.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    // Holds the database connection so we can close it when the viewmodel dies
    private var snapshotListener: ListenerRegistration? = null

    init {
        subscribeToRealtimeUpdates()
    }

    private fun subscribeToRealtimeUpdates() {
        val user = auth.currentUser
        if (user == null) {
            _relays.value = emptyList()
            return
        }

        _isLoading.value = true
        val collectionRef = db.collection("users").document(user.uid).collection("switches")

        // This listener automatically fires whenever data changes on the server OR locally
        snapshotListener = collectionRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                _isLoading.value = false
                return@addSnapshotListener
            }

            if (snapshot != null) {
                // Parse the documents into our RelayDevice list
                val devices = snapshot.toObjects(RelayDevice::class.java)
                _relays.value = devices
            }
            _isLoading.value = false
        }
    }

    fun addRelay(name: String, desc: String, url: String) {
        val user = auth.currentUser ?: return

        // Generate a unique ID for the new switch
        val newDocRef = db.collection("users").document(user.uid).collection("switches").document()

        val newDevice = RelayDevice(
            id = newDocRef.id, // Use the generated ID
            name = name,
            description = desc,
            connectionUrl = url,
            isOn = false,
            isFavorite = false
        )

        // Save to Firestore
        newDocRef.set(newDevice)
    }

    fun updateRelay(id: String, name: String, desc: String, url: String) {
        val user = auth.currentUser ?: return

        val updates = mapOf(
            "name" to name,
            "description" to desc,
            "connectionUrl" to url
        )

        db.collection("users").document(user.uid).collection("switches").document(id)
            .update(updates)
    }

    fun deleteRelay(id: String) {
        val user = auth.currentUser ?: return

        db.collection("users").document(user.uid).collection("switches").document(id)
            .delete()
    }

    fun toggleRelay(device: RelayDevice) {
        val user = auth.currentUser ?: return

        // Optimistic update
        val updatedList = _relays.value.map {
            if (it.id == device.id) it.copy(isOn = !it.isOn) else it
        }
        _relays.value = updatedList

        // Update Database
        db.collection("users").document(user.uid).collection("switches").document(device.id)
            .update("isOn", !device.isOn)

        // Hit the external API
        viewModelScope.launch {
            val targetId = if (device.connectionUrl.isNotBlank()) device.connectionUrl else device.id
            PowerSenseClient.toggleRelay(targetId, !device.isOn)
        }
    }

    fun toggleFavorite(device: RelayDevice) {
        val user = auth.currentUser ?: return

        // Optimistic update
        val updatedList = _relays.value.map {
            if (it.id == device.id) it.copy(isFavorite = !it.isFavorite) else it
        }
        _relays.value = updatedList

        // Persist to Firestore
        db.collection("users").document(user.uid).collection("switches").document(device.id)
            .update("isFavorite", !device.isFavorite)
    }

    // Cleanup listener when ViewModel is cleared (e.g., user logs out)
    override fun onCleared() {
        snapshotListener?.remove()
        super.onCleared()
    }
}