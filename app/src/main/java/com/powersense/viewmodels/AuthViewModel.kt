package com.powersense.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// This class defines the different states our UI can be in
sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    object Success : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel : ViewModel() {

    private val auth: FirebaseAuth = Firebase.auth

    // This StateFlow will emit the current state (Loading, Success, Error)
    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState = _authState.asStateFlow()

    fun signUp(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                // Firebase does all the work here
                auth.createUserWithEmailAndPassword(email, password).await()
                _authState.value = AuthState.Success
            } catch (e: Exception) {
                // Handle errors (e.g., "email already in use")
                _authState.value = AuthState.Error(e.message ?: "Sign-up failed")
            }
        }
    }

    fun logIn(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                // Firebase checks the email and password
                auth.signInWithEmailAndPassword(email, password).await()
                _authState.value = AuthState.Success
            } catch (e: Exception) {
                // Handle errors (e.g., "wrong password")
                _authState.value = AuthState.Error(e.message ?: "Login failed")
            }
        }
    }


    fun sendPasswordReset(email: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                auth.sendPasswordResetEmail(email).await()
                _authState.value = AuthState.Success
            } catch (e: Exception) {
                // Handle errors (e.g., "user not found")
                _authState.value = AuthState.Error(e.message ?: "Failed to send reset email")
            }
        }
    }

    // Helper to reset the state (e.g., after an error)
    fun resetState() {
        _authState.value = AuthState.Idle
    }
}