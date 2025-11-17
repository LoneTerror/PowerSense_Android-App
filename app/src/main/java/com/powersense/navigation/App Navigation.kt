package com.powersense.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.powersense.screens.ForgotPasswordScreen
import com.powersense.screens.LoginScreen
import com.powersense.screens.MainAppScreen
import com.powersense.screens.ProfileScreen
import com.powersense.screens.SignUpScreen
import com.powersense.ui.theme.PowerSenseTheme // <-- ADD THIS IMPORT
import com.powersense.viewmodels.ThemeOption // <-- ADD THIS IMPORT

// This Composable manages all the app's navigation.
@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "login" // App starts at the Login screen
    ) {
        composable("login") {
            // Wrap LoginScreen in a Dark Theme
            PowerSenseTheme(themeOption = ThemeOption.Dark) {
                LoginScreen(
                    onLoginSuccess = {
                        // After login, go to the main app and clear the back stack
                        navController.navigate("main") {
                            popUpTo("login") { inclusive = true }
                        }
                    },
                    onNavigateToSignUp = {
                        navController.navigate("signup")
                    },
                    // --- 2. ADD THIS NEW NAVIGATION ---
                    onNavigateToForgotPassword = {
                        navController.navigate("forgot_password")
                    }
                )
            }
        }

        composable("signup") {
            // Wrap SignUpScreen in a Dark Theme
            PowerSenseTheme(themeOption = ThemeOption.Dark) {
                SignUpScreen(
                    onSignUpSuccess = {
                        // After sign up, go to the main app (same as login)
                        navController.navigate("main") {
                            popUpTo("signup") { inclusive = true }
                        }
                    },
                    onNavigateToLogin = {
                        navController.popBackStack() // Go back to the previous screen (Login)
                    }
                )
            }
        }

        // --- 3. ADD THIS NEW ROUTE ---
        composable("forgot_password") {
            PowerSenseTheme(themeOption = ThemeOption.Dark) {
                ForgotPasswordScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
        }
        composable("main") {
            // This screen will use the *default* theme (Light/Dark/System)
            MainAppScreen(
                appNavController = navController
            )
        }
        composable("profile") {
            // This screen will also use the *default* theme
            ProfileScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onLogout = {
                    // On logout, go back to login screen and clear history
                    navController.navigate("login") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}