package com.powersense.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.powersense.R
import com.powersense.screens.components.PasswordTextField
import com.powersense.ui.theme.PowerSenseGreen
import com.powersense.ui.theme.PowerSensePurple
import com.powersense.ui.theme.PowerSenseTheme
import com.powersense.viewmodels.AuthViewModel
import com.powersense.viewmodels.AuthState

// --- PASSWORD STRENGTH HELPER ---
enum class PasswordStrength(val text: String, val color: Color) {
    WEAK("Weak", Color.Red),
    AVERAGE("Average", Color.Yellow),
    GOOD("Good", Color(0xFF9AE19D)), // Light Green
    STRONG("Strong", PowerSenseGreen)
}

private fun calculatePasswordStrength(password: String): Pair<PasswordStrength, List<Boolean>> {
    val hasMinLength = password.length >= 8
    val hasUppercase = password.any { it.isUpperCase() }
    val hasDigit = password.any { it.isDigit() }
    val hasSpecial = password.any { !it.isLetterOrDigit() }

    val score = (if (hasMinLength) 1 else 0) +
            (if (hasUppercase) 1 else 0) +
            (if (hasDigit) 1 else 0) +
            (if (hasSpecial) 1 else 0)

    val strength = if (!hasMinLength) {
        PasswordStrength.WEAK
    } else {
        when (score) {
            1 -> PasswordStrength.WEAK
            2 -> PasswordStrength.AVERAGE
            3 -> PasswordStrength.GOOD
            4 -> PasswordStrength.STRONG
            else -> PasswordStrength.WEAK
        }
    }

    return Pair(strength, listOf(hasMinLength, hasUppercase, hasDigit, hasSpecial))
}
// --- END OF HELPER ---


@Composable
fun SignUpScreen(
    onSignUpSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit,
    authViewModel: AuthViewModel = viewModel()
) {
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    // State for password strength
    var strength by remember { mutableStateOf<PasswordStrength?>(null) }
    var requirements by remember { mutableStateOf(listOf(false, false, false, false)) }

    // This state tracks if the password field is focused
    var isPasswordFocused by remember { mutableStateOf(false) }

    // Matching state: null = pristine, true = match, false = mismatch
    var passwordMatchState by remember { mutableStateOf<Boolean?>(null) }

    // State for confirm password visibility toggle
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    val authState by authViewModel.authState.collectAsState()
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    // Calculate if password is valid (all requirements met)
    // This controls the red error state of the password box
    val isPasswordValid = remember(requirements) {
        requirements.all { it }
    }

    // React to auth state changes
    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.Success -> {
                Toast.makeText(context, "Sign-up successful!", Toast.LENGTH_SHORT).show()
                onSignUpSuccess()
            }
            is AuthState.Error -> {
                val errorMessage = (authState as AuthState.Error).message
                Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                authViewModel.resetState()
            }
            else -> Unit
        }
    }

    // React to password changes to calculate strength
    LaunchedEffect(password) {
        if (password.isEmpty()) {
            strength = null
            requirements = listOf(false, false, false, false)
        } else {
            val (s, r) = calculatePasswordStrength(password)
            strength = s
            requirements = r
        }
    }

    // Check for match
    LaunchedEffect(password, confirmPassword) {
        if (confirmPassword.isNotEmpty()) {
            passwordMatchState = (password == confirmPassword)
        } else {
            passwordMatchState = null
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()), // Enable scrolling for smaller screens
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
            "Create Your Account",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            "Join PowerSense today!",
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        // --- Full Name Field ---
        OutlinedTextField(
            value = fullName,
            onValueChange = { fullName = it },
            label = { Text("Full Name") },
            leadingIcon = { Icon(Icons.Default.Person, "Name") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            readOnly = authState is AuthState.Loading,
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
        )
        Spacer(modifier = Modifier.height(16.dp))

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
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
        )
        Spacer(modifier = Modifier.height(16.dp))

        // --- REORDERED: Password Requirements Checklist (Now Above) ---
        // Only show if user is typing/focused on password, or if it's invalid and not empty
        val showRequirements = isPasswordFocused || (password.isNotEmpty() && !isPasswordValid)

        AnimatedVisibility(visible = showRequirements) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp) // Add padding below list before the input box
            ) {
                Text(
                    text = "Password must contain:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                PasswordRequirements(requirements = requirements)
            }
        }

        // --- Password Field (UPDATED) ---
        PasswordTextField(
            value = password,
            onValueChange = { password = it },
            label = "Create your password",
            leadingIcon = Icons.Default.Lock,
            readOnly = authState is AuthState.Loading,
            // Check for error: If user has typed something AND it's not valid, show error (Red Box)
            isError = password.isNotEmpty() && !isPasswordValid,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
            modifier = Modifier.onFocusChanged { isPasswordFocused = it.isFocused }
        )

        // Optional: Helper text below box if error
        if (password.isNotEmpty() && !isPasswordValid) {
            Text(
                text = "Password does not meet requirements",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.align(Alignment.Start).padding(start = 16.dp, top = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // --- Password Strength Indicator ---
        PasswordStrengthIndicator(strength = strength)

        Spacer(modifier = Modifier.height(16.dp))

        // --- Confirm Password Field ---
        val (matchIcon, iconTint, borderColor) = when (passwordMatchState) {
            true -> Triple(Icons.Default.Check, PowerSenseGreen, PowerSenseGreen)
            false -> Triple(Icons.Default.Close, Color.Red, Color.Red)
            null -> Triple(null, MaterialTheme.colorScheme.onSurfaceVariant, MaterialTheme.colorScheme.outline)
        }

        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = { Text("Re-enter your password") },
            leadingIcon = { Icon(Icons.Default.Lock, "Confirm") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            readOnly = authState is AuthState.Loading,
            singleLine = true,
            visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                // FIX: Center align vertically within Row
                Row(
                    verticalAlignment = Alignment.CenterVertically, // Ensures vertical centering
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    if (matchIcon != null) {
                        Icon(
                            imageVector = matchIcon,
                            contentDescription = "Match Status",
                            tint = iconTint,
                            modifier = Modifier.size(24.dp) // Explicit size to match standard icons
                        )
                        Spacer(modifier = Modifier.width(8.dp)) // Space between icons
                    }
                    IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                        Icon(
                            imageVector = if (confirmPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                            contentDescription = if (confirmPasswordVisible) "Hide password" else "Show password"
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
                if (password != confirmPassword) { Toast.makeText(context, "Passwords do not match.", Toast.LENGTH_SHORT).show() }
                else if (!isPasswordValid) { Toast.makeText(context, "Password requirements not met.", Toast.LENGTH_SHORT).show() }
                else { authViewModel.signUp(email.trim(), password.trim(), fullName.trim()) }
            })
        )

        Spacer(modifier = Modifier.height(32.dp))

        // --- Sign Up Button ---
        Button(
            onClick = {
                keyboardController?.hide()
                if (password != confirmPassword) {
                    Toast.makeText(context, "Passwords do not match.", Toast.LENGTH_SHORT).show()
                } else if (!isPasswordValid) {
                    Toast.makeText(context, "Password requirements not met.", Toast.LENGTH_SHORT).show()
                } else {
                    authViewModel.signUp(email.trim(), password.trim(), fullName.trim())
                }
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
                Text("Sign Up", fontSize = 18.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- Bottom Link ---
        TextButton(
            onClick = onNavigateToLogin,
            enabled = authState !is AuthState.Loading
        ) {
            Text("Already have an account? Log In", color = PowerSensePurple)
        }
    }
}

// --- Helper Composables ---
@Composable
fun PasswordStrengthIndicator(strength: PasswordStrength?) {
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
fun PasswordRequirements(requirements: List<Boolean>) {
    val requirementLabels = listOf(
        "At least 8 characters",
        "An uppercase letter",
        "A number",
        "A special character"
    )

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        requirements.forEachIndexed { index, isMet ->
            RequirementRow(
                text = requirementLabels[index],
                isMet = isMet
            )
        }
    }
}

@Composable
fun RequirementRow(text: String, isMet: Boolean) {
    val icon = if (isMet) Icons.Default.Check else Icons.Default.Close
    val color = if (isMet) PowerSenseGreen else MaterialTheme.colorScheme.error // Red if not met, Green if met

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
        Text(
            text = text,
            color = color,
            style = MaterialTheme.typography.bodySmall
        )
    }
}