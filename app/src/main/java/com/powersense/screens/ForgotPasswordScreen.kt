package com.powersense.screens

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.powersense.R
import com.powersense.ui.theme.PowerSensePurple
import com.powersense.viewmodels.AuthViewModel
import com.powersense.viewmodels.AuthState

@Composable
fun ForgotPasswordScreen(
    onNavigateBack: () -> Unit,
    authViewModel: AuthViewModel = viewModel()
) {
    var email by remember { mutableStateOf("") }
    val authState by authViewModel.authState.collectAsState()
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current

    // Listen for state changes
    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.Success -> {
                Toast.makeText(context, "Password reset email sent!", Toast.LENGTH_LONG).show()
                authViewModel.resetState()
                onNavigateBack() // Go back to login
            }
            is AuthState.Error -> {
                val errorMessage = (authState as AuthState.Error).message
                Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                authViewModel.resetState()
            }
            else -> Unit
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // --- Back Button ---
        Box(modifier = Modifier.fillMaxWidth()) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to Login")
            }
        }

        Spacer(modifier = Modifier.weight(1f)) // Pushes content to center

        // --- Logo and Title ---
        Image(
            painter = painterResource(id = R.drawable.ic_powersense_logo),
            contentDescription = "PowerSense Logo",
            modifier = Modifier.size(80.dp).clip(RoundedCornerShape(12.dp))
        )
        Text(
            "Reset Password",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(top = 16.dp)
        )
        Text(
            "Enter your email and we'll send you a link to reset your password.",
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 48.dp, top = 8.dp)
        )

        // --- Email Field ---
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email Address") },
            leadingIcon = { Icon(Icons.Default.Email, "Email") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            readOnly = authState is AuthState.Loading,
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    keyboardController?.hide()
                    authViewModel.sendPasswordReset(email.trim())
                }
            )
        )
        Spacer(modifier = Modifier.height(24.dp))

        // --- Send Reset Button ---
        Button(
            onClick = {
                keyboardController?.hide()
                authViewModel.sendPasswordReset(email.trim())
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PowerSensePurple),
            shape = RoundedCornerShape(12.dp),
            enabled = authState !is AuthState.Loading
        ) {
            if (authState is AuthState.Loading) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Text("Send Reset Email", fontSize = 18.sp)
            }
        }

        // --- 1. ADD THIS SPACER ---
        Spacer(modifier = Modifier.height(8.dp))

        // --- 2. ADD THIS TEXT BUTTON ---
        TextButton(
            onClick = onNavigateBack, // Uses the same navigation as the top arrow
            enabled = authState !is AuthState.Loading
        ) {
            Text("Back to Login", color = PowerSensePurple)
        }
        // --- END OF CHANGES ---

        Spacer(modifier = Modifier.weight(1.5f)) // Pushes content up from bottom
    }
}