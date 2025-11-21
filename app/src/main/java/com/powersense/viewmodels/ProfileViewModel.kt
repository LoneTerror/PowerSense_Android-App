package com.powersense.viewmodels

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.ktx.auth
import com.google.firebase.auth.ktx.userProfileChangeRequest
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class UserProfile(
    val uid: String = "",
    val email: String = "",
    val fullName: String = "",
    val username: String = "",
    val phone: String = "",
    val profileImageUrl: String = ""
)

sealed class ProfileState {
    object Idle : ProfileState()
    object Loading : ProfileState()
    data class Success(val message: String) : ProfileState()
    data class Error(val message: String) : ProfileState()
}

class ProfileViewModel : ViewModel() {

    private val auth = Firebase.auth
    private val db = Firebase.firestore

    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile = _userProfile.asStateFlow()

    private val _profileState = MutableStateFlow<ProfileState>(ProfileState.Idle)
    val profileState = _profileState.asStateFlow()

    init {
        loadUserProfile()
    }

    fun loadUserProfile() {
        val firebaseUser = auth.currentUser ?: return

        _userProfile.value = UserProfile(
            uid = firebaseUser.uid,
            email = firebaseUser.email ?: "",
            fullName = firebaseUser.displayName ?: "",
            profileImageUrl = firebaseUser.photoUrl?.toString() ?: ""
        )

        viewModelScope.launch {
            try {
                val snapshot = db.collection("users").document(firebaseUser.uid).get().await()
                val firestoreProfile = snapshot.toObject<UserProfile>()

                _userProfile.value = _userProfile.value?.copy(
                    fullName = firestoreProfile?.fullName ?: _userProfile.value?.fullName ?: "",
                    username = firestoreProfile?.username ?: "",
                    phone = firestoreProfile?.phone ?: "",
                    profileImageUrl = firestoreProfile?.profileImageUrl ?: _userProfile.value?.profileImageUrl ?: ""
                )
            } catch (e: Exception) {
                _profileState.value = ProfileState.Error(e.message ?: "Failed to load profile")
            }
        }
    }

    fun saveProfile(fullName: String, username: String, phone: String) {
        val uid = auth.currentUser?.uid ?: return
        _profileState.value = ProfileState.Loading

        val updatedProfile = _userProfile.value?.copy(
            fullName = fullName,
            username = username,
            phone = phone
        ) ?: return

        viewModelScope.launch {
            try {
                db.collection("users").document(uid).set(updatedProfile).await()

                val authProfileUpdates = userProfileChangeRequest {
                    displayName = fullName
                }
                auth.currentUser?.updateProfile(authProfileUpdates)?.await()

                _userProfile.value = updatedProfile
                _profileState.value = ProfileState.Success("Profile saved!")
            } catch (e: Exception) {
                _profileState.value = ProfileState.Error(e.message ?: "Failed to save profile")
            }
        }
    }

    // Updated to accept a String URL instead of a Uri file
    fun updateProfileImage(imageUrl: String) {
        val uid = auth.currentUser?.uid ?: return
        _profileState.value = ProfileState.Loading

        viewModelScope.launch {
            try {
                val authProfileUpdates = userProfileChangeRequest {
                    photoUri = Uri.parse(imageUrl)
                }
                auth.currentUser?.updateProfile(authProfileUpdates)?.await()

                db.collection("users").document(uid).update("profileImageUrl", imageUrl).await()

                _userProfile.value = _userProfile.value?.copy(profileImageUrl = imageUrl)
                _profileState.value = ProfileState.Success("Avatar updated!")
            } catch (e: Exception) {
                // Fallback: if document doesn't exist, create it
                try {
                    val newProfile = _userProfile.value?.copy(profileImageUrl = imageUrl) ?: UserProfile(uid=uid, profileImageUrl=imageUrl)
                    db.collection("users").document(uid).set(newProfile).await()
                    _userProfile.value = newProfile
                    _profileState.value = ProfileState.Success("Avatar updated!")
                } catch (retryEx: Exception) {
                    _profileState.value = ProfileState.Error(retryEx.message ?: "Failed to update avatar")
                }
            }
        }
    }

    fun resetState() {
        _profileState.value = ProfileState.Idle
    }
}