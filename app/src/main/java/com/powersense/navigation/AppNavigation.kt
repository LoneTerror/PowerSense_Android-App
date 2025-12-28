package com.powersense.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
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
    val profileViewModel: com.powersense.viewmodels.ProfileViewModel = androidx.lifecycle.viewmodel.compose.viewModel()

    // 1. Check if a user is currently logged in
    val currentUser = Firebase.auth.currentUser

    // 2. Decide where to start: 'main' if logged in, 'login' if not
    val startDest = if (currentUser != null) "main" else "login"

    NavHost(
        navController = navController,
        startDestination = startDest // Use the dynamic variable here
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
                appNavController = navController,
                profileViewModel = profileViewModel
            )
        }
        composable("profile") {
            ProfileScreen(
                appNavController = navController,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onLogout = {
                    // 3. ACTUAL SIGN OUT LOGIC
                    Firebase.auth.signOut()

                    navController.navigate("login") {
                        popUpTo(0) { inclusive = true }
                    }
                },
                profileViewModel = profileViewModel
            )
        }

        composable("change_password") {
            ChangePasswordScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToForgotPassword = { navController.navigate("reset_password_internal") }
            )
        }

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

        composable("avatar_selection") {
            AvatarSelectionScreen(
                onNavigateBack = { navController.popBackStack() },
                onAvatarSelected = { url ->
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("selected_avatar", url)
                    navController.popBackStack()
                }
            )
        }
    }
}