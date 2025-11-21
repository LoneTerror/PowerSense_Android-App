package com.powersense.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility // <-- ADDED THIS
import androidx.compose.material.icons.filled.VisibilityOff // <-- ADDED THIS
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
import androidx.compose.ui.text.input.VisualTransformation // <-- ADDED THIS
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
// Removing 'public' keyword as it's redundant for enum classes
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

    // --- 1. NEW STATE FOR MATCHING ---
    // null = pristine, true = match, false = mismatch
    var passwordMatchState by remember { mutableStateOf<Boolean?>(null) }

    // State for confirm password visibility toggle
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    val authState by authViewModel.authState.collectAsState()
    val context = LocalContext.current
    // --- 7. ADD KEYBOARD/FOCUS CONTROLLERS ---
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current


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

    // --- 2. NEW EFFECT TO CHECK FOR MATCH ---
    LaunchedEffect(password, confirmPassword) {
        // Only show validation if the user has started typing in the confirm field
        if (confirmPassword.isNotEmpty()) {
            passwordMatchState = (password == confirmPassword)
        } else {
            passwordMatchState = null // Reset if empty
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
            modifier = Modifier.padding(bottom = 48.dp)
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
            // --- 9. ADD KEYBOARD OPTIONS/ACTIONS ---
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            )
        )
        Spacer(modifier = Modifier.height(16.dp))

        // --- Password Field (UPDATED to use PasswordTextField with Focus) ---
        PasswordTextField(
            value = password,
            onValueChange = { password = it },
            label = "Create your password",
            leadingIcon = Icons.Default.Lock,
            readOnly = authState is AuthState.Loading,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
            modifier = Modifier.onFocusChanged { isPasswordFocused = it.isFocused }
        )
        Spacer(modifier = Modifier.height(8.dp))

        // --- Password Strength Indicator ---
        PasswordStrengthIndicator(strength = strength)

        Spacer(modifier = Modifier.height(16.dp))

        // --- Confirm Password Field (UPDATED with visibility toggle + dynamic border) ---
        val (matchIcon, iconTint, borderColor) = when (passwordMatchState) {
            true -> Triple(Icons.Default.Check, PowerSenseGreen, PowerSenseGreen)
            false -> Triple(Icons.Default.Close, Color.Red, Color.Red)
            null -> Triple(null, MaterialTheme.colorScheme.onSurfaceVariant, MaterialTheme.colorScheme.outline)
        }

        // --- Confirm Password Field (Updated) ---
        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = { Text("Re-enter your password") },
            leadingIcon = { Icon(Icons.Default.Lock, "Confirm") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            readOnly = authState is AuthState.Loading,
            singleLine = true,
            // Add visual transformation logic
            visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            // Add trailing icon logic manually here to combine the match icon AND the eye icon
            trailingIcon = {
                Row {
                    if (matchIcon != null) {
                        Icon(matchIcon, "Match Status", tint = iconTint, modifier = Modifier.padding(end = 8.dp))
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
                else if (strength != PasswordStrength.GOOD && strength != PasswordStrength.STRONG) { Toast.makeText(context, "Password too weak.", Toast.LENGTH_SHORT).show() }
                else { authViewModel.signUp(email.trim(), password.trim(), fullName.trim()) }
            })
        )

        Spacer(modifier = Modifier.height(16.dp))

        // --- Password Requirements Checklist ---
        AnimatedVisibility(visible = isPasswordFocused) {
            Column {
                PasswordRequirements(requirements = requirements)
            }
        }

        // This spacer fills the space when the checklist is hidden
        if (!isPasswordFocused) {
            // Height is based on 4 lines of text + padding
            Spacer(modifier = Modifier.height(104.dp))
        } else {
            Spacer(modifier = Modifier.height(24.dp))
        }

        // --- Sign Up Button ---
        Button(
            onClick = {
                keyboardController?.hide()
                if (password != confirmPassword) {
                    Toast.makeText(context, "Passwords do not match.", Toast.LENGTH_SHORT).show()
                } else if (strength != PasswordStrength.GOOD && strength != PasswordStrength.STRONG) {
                    Toast.makeText(context, "Password is not strong enough.", Toast.LENGTH_SHORT).show()
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

// --- NEW COMPOSABLE for Strength Text ---
@Composable
fun PasswordStrengthIndicator(  strength: PasswordStrength?) {
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

// --- NEW COMPOSABLE for Requirements Checklist ---
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

// --- NEW COMPOSABLE for a single requirement row ---
@Composable
fun RequirementRow(text: String, isMet: Boolean) {
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
        Text(
            text = text,
            color = color
        )
    }
}