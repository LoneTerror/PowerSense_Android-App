package com.powersense.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.powersense.screens.components.PasswordTextField
import com.powersense.ui.theme.PowerSenseGreen
import com.powersense.ui.theme.PowerSensePurple
import com.powersense.viewmodels.AuthState
import com.powersense.viewmodels.AuthViewModel

// --- PRIVATE PASSWORD LOGIC ---
private enum class CPPasswordStrength(val text: String, val color: Color) {
    WEAK("Weak", Color.Red),
    AVERAGE("Average", Color.Yellow),
    GOOD("Good", Color(0xFF9AE19D)),
    STRONG("Strong", PowerSenseGreen)
}

private fun calculateCPPasswordStrength(password: String): Pair<CPPasswordStrength, List<Boolean>> {
    val hasMinLength = password.length >= 8
    val hasUppercase = password.any { it.isUpperCase() }
    val hasDigit = password.any { it.isDigit() }
    val hasSpecial = password.any { !it.isLetterOrDigit() }

    val score = (if (hasMinLength) 1 else 0) +
            (if (hasUppercase) 1 else 0) +
            (if (hasDigit) 1 else 0) +
            (if (hasSpecial) 1 else 0)

    val strength = if (!hasMinLength) {
        CPPasswordStrength.WEAK
    } else {
        when (score) {
            1 -> CPPasswordStrength.WEAK
            2 -> CPPasswordStrength.AVERAGE
            3 -> CPPasswordStrength.GOOD
            4 -> CPPasswordStrength.STRONG
            else -> CPPasswordStrength.WEAK
        }
    }
    return Pair(strength, listOf(hasMinLength, hasUppercase, hasDigit, hasSpecial))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangePasswordScreen(
    onNavigateBack: () -> Unit,
    onNavigateToForgotPassword: () -> Unit,
    authViewModel: AuthViewModel = viewModel()
) {
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmNewPassword by remember { mutableStateOf("") }

    // State for confirm password visibility toggle
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    // State for strength & matching logic
    var strength by remember { mutableStateOf<CPPasswordStrength?>(null) }
    var requirements by remember { mutableStateOf(listOf(false, false, false, false)) }
    var isNewPasswordFocused by remember { mutableStateOf(false) }
    var passwordMatchState by remember { mutableStateOf<Boolean?>(null) }

    val authState by authViewModel.authState.collectAsState()
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    // Listen for success/error
    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.Success -> {
                Toast.makeText(context, "Password changed successfully!", Toast.LENGTH_SHORT).show()
                authViewModel.resetState()
                onNavigateBack()
            }
            is AuthState.Error -> {
                val errorMessage = (authState as AuthState.Error).message
                Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                authViewModel.resetState()
            }
            else -> Unit
        }
    }

    // Calculate strength for New Password
    LaunchedEffect(newPassword) {
        if (newPassword.isEmpty()) {
            strength = null
            requirements = listOf(false, false, false, false)
        } else {
            val (s, r) = calculateCPPasswordStrength(newPassword)
            strength = s
            requirements = r
        }
    }

    // Check matching
    LaunchedEffect(newPassword, confirmNewPassword) {
        if (confirmNewPassword.isNotEmpty()) {
            passwordMatchState = (newPassword == confirmNewPassword)
        } else {
            passwordMatchState = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Change Password", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()), // Make scrollable to fit checklist
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Text(
                "Create a new, strong password that you don't use for other websites.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            // Current Password
            PasswordTextField(
                value = currentPassword,
                onValueChange = { currentPassword = it },
                label = "Current Password",
                leadingIcon = Icons.Default.Lock,
                readOnly = authState is AuthState.Loading,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
            )

            Spacer(modifier = Modifier.height(16.dp))

            // New Password
            PasswordTextField(
                value = newPassword,
                onValueChange = { newPassword = it },
                label = "New Password",
                leadingIcon = Icons.Default.Lock,
                readOnly = authState is AuthState.Loading,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                modifier = Modifier.onFocusChanged { isNewPasswordFocused = it.isFocused }
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Strength Indicator
            CPPasswordStrengthIndicator(strength = strength)
            Spacer(modifier = Modifier.height(16.dp))

            // Match Colors
            val (trailingIcon, iconTint, borderColor) = when (passwordMatchState) {
                true -> Triple(Icons.Default.Check, PowerSenseGreen, PowerSenseGreen)
                false -> Triple(Icons.Default.Close, Color.Red, Color.Red)
                null -> Triple(null, MaterialTheme.colorScheme.onSurfaceVariant, MaterialTheme.colorScheme.outline)
            }

            // Confirm New Password
            OutlinedTextField(
                value = confirmNewPassword,
                onValueChange = { confirmNewPassword = it },
                label = { Text("Confirm New Password") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "Confirm Password") },
                visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                trailingIcon = {
                    Row {
                        if (trailingIcon != null) {
                            Icon(
                                imageVector = trailingIcon,
                                contentDescription = "Status",
                                tint = iconTint,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                        }
                        IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                            Icon(
                                imageVector = if (confirmPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                contentDescription = if (confirmPasswordVisible) "Hide" else "Show"
                            )
                        }
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = borderColor,
                    unfocusedBorderColor = borderColor
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    keyboardController?.hide()
                    // Validation logic
                    if (newPassword != confirmNewPassword) {
                        Toast.makeText(context, "New passwords do not match", Toast.LENGTH_SHORT).show()
                    } else if (strength != CPPasswordStrength.GOOD && strength != CPPasswordStrength.STRONG) {
                        Toast.makeText(context, "Password too weak", Toast.LENGTH_SHORT).show()
                    } else {
                        authViewModel.changePassword(currentPassword, newPassword)
                    }
                }),
                readOnly = authState is AuthState.Loading
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Requirements Checklist (Animated)
            AnimatedVisibility(visible = isNewPasswordFocused) {
                Column { CPPasswordRequirements(requirements) }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Update Button
            Button(
                onClick = {
                    keyboardController?.hide()
                    if (newPassword != confirmNewPassword) {
                        Toast.makeText(context, "New passwords do not match", Toast.LENGTH_SHORT).show()
                    } else if (strength != CPPasswordStrength.GOOD && strength != CPPasswordStrength.STRONG) {
                        Toast.makeText(context, "Password too weak", Toast.LENGTH_SHORT).show()
                    } else {
                        authViewModel.changePassword(currentPassword, newPassword)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PowerSenseGreen),
                shape = RoundedCornerShape(12.dp),
                enabled = authState !is AuthState.Loading
            ) {
                if (authState is AuthState.Loading) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Text("Update Password", fontSize = 16.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Forgot Password Link
            TextButton(
                onClick = onNavigateToForgotPassword,
                enabled = authState !is AuthState.Loading
            ) {
                Text("Forgot Password?", color = PowerSensePurple)
            }
        }
    }
}

// --- PRIVATE Helper Composables ---

@Composable
private fun CPPasswordStrengthIndicator(strength: CPPasswordStrength?) {
    val defaultColor = MaterialTheme.colorScheme.background
    val strengthColor by animateColorAsState(
        targetValue = strength?.color ?: defaultColor,
        label = "Strength Color"
    )
    AnimatedContent(
        targetState = strength?.text ?: "",
        label = "Strength Text"
    ) { text ->
        Text(
            text = if(text.isNotEmpty()) "Strength: $text" else "",
            color = strengthColor,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun CPPasswordRequirements(requirements: List<Boolean>) {
    val requirementLabels = listOf(
        "At least 8 characters",
        "An uppercase letter",
        "A number",
        "A special character"
    )
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        requirements.forEachIndexed { index, isMet ->
            CPRequirementRow(requirementLabels[index], isMet)
        }
    }
}

@Composable
private fun CPRequirementRow(text: String, isMet: Boolean) {
    val icon = if (isMet) Icons.Default.Check else Icons.Default.Close
    val color = if (isMet) PowerSenseGreen else MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(16.dp)
        )
        Text(text = text, color = color)
    }
}