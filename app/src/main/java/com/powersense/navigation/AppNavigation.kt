package com.powersense.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.powersense.screens.AvatarSelectionScreen
import com.powersense.screens.ChangePasswordScreen
import com.powersense.screens.ForgotPasswordScreen
import com.powersense.screens.LoginScreen
import com.powersense.screens.MainAppScreen
import com.powersense.screens.PrivacySettingsScreen
import com.powersense.screens.ProfileScreen
import com.powersense.screens.ResetPasswordScreen
import com.powersense.screens.SignUpScreen
import com.powersense.ui.theme.PowerSenseTheme
import com.powersense.viewmodels.ThemeOption

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "login"
    ) {
        composable("login") {
            PowerSenseTheme(themeOption = ThemeOption.Dark) {
                LoginScreen(
                    onLoginSuccess = {
                        navController.navigate("main") {
                            popUpTo("login") { inclusive = true }
                        }
                    },
                    onNavigateToSignUp = {
                        navController.navigate("signup")
                    },
                    onNavigateToForgotPassword = {
                        navController.navigate("forgot_password")
                    }
                )
            }
        }
        composable("signup") {
            PowerSenseTheme(themeOption = ThemeOption.Dark) {
                SignUpScreen(
                    onSignUpSuccess = {
                        navController.navigate("main") {
                            popUpTo("signup") { inclusive = true }
                        }
                    },
                    onNavigateToLogin = {
                        navController.popBackStack()
                    }
                )
            }
        }
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
            MainAppScreen(
                appNavController = navController
            )
        }
        composable("profile") {
            ProfileScreen(
                appNavController = navController,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onLogout = {
                    navController.navigate("login") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable("change_password") {
            ChangePasswordScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToForgotPassword = { navController.navigate("reset_password_internal") }
            )
        }

        // --- NEW ROUTE FOR LOGGED-IN USERS ---
        composable("reset_password_internal") {
            ResetPasswordScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable("privacy_settings") {
            PrivacySettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // NEW ROUTE FOR AVATAR SELECTION
        composable("avatar_selection") {
            AvatarSelectionScreen(
                onNavigateBack = { navController.popBackStack() },
                onAvatarSelected = { url ->
                    // Pass the URL back to the previous screen (ProfileScreen)
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("selected_avatar", url)
                    navController.popBackStack()
                }
            )
        }
    }
}