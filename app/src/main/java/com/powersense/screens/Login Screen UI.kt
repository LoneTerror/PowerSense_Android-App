package com.powersense.screens

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.powersense.R
import com.powersense.ui.theme.PowerSensePurple
import com.powersense.ui.theme.PowerSenseTheme
import com.powersense.viewmodels.AuthViewModel
import com.powersense.viewmodels.AuthState

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onNavigateToSignUp: () -> Unit,
    onNavigateToForgotPassword: () -> Unit,
    authViewModel: AuthViewModel = viewModel()
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    val authState by authViewModel.authState.collectAsState()
    val context = LocalContext.current

    // --- 7. ADD KEYBOARD/FOCUS CONTROLLERS ---
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.Success -> {
                Toast.makeText(context, "Login successful!", Toast.LENGTH_SHORT).show()
                onLoginSuccess()
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
            // ADD THIS BACKGROUND MODIFIER
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_powersense_logo),
            contentDescription = "PowerSense Logo",
            modifier = Modifier
                .size(80.dp)
                .clip(RoundedCornerShape(12.dp))
        )
        Text(
            "PowerSense",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            "Your Home's Energy, Intelligently Managed.",
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 48.dp)
        )

        // --- Email Field ---
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            leadingIcon = { Icon(Icons.Default.Person, "Email") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            readOnly = authState is AuthState.Loading,
            singleLine = true,
            // --- 9. ADD KEYBOARD OPTIONS/ACTIONS ---
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            )

        )

        Spacer(modifier = Modifier.height(16.dp))

        // --- Password Field ---
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            leadingIcon = { Icon(Icons.Default.Lock, "Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            readOnly = authState is AuthState.Loading,
            singleLine = true,
            // --- 11. ADD KEYBOARD OPTIONS/ACTIONS ---
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(
                onDone = {
                    keyboardController?.hide()
                    if (authState !is AuthState.Loading) {
                        authViewModel.logIn(email.trim(), password.trim())
                    }
                }
            )
        )

        Spacer(modifier = Modifier.height(24.dp))

        // --- Login Button ---
        Button(
            onClick = {
                keyboardController?.hide()
                authViewModel.logIn(email.trim(), password.trim())
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
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
                Text("Log In", fontSize = 18.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- Bottom Links ---
        TextButton(
            onClick = onNavigateToSignUp,
            enabled = authState !is AuthState.Loading
        ) {
            Text("Don't have an account? Sign Up", color = PowerSensePurple)
        }
        TextButton(
            onClick = onNavigateToForgotPassword,
            enabled = authState !is AuthState.Loading
        ) {
            Text(
                "Forgot Password?",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}