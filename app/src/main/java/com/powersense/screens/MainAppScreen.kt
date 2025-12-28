package com.powersense.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController

// --- IMPORTS FOR SCREENS ---
import com.powersense.screens.tabs.HomeScreen
import com.powersense.screens.tabs.MetricsScreen
import com.powersense.screens.tabs.SettingsScreen
import com.powersense.screens.tabs.SimulationScreen

// --- IMPORTS FOR THEME ---
import com.powersense.ui.theme.PowerSensePurple

// --- IMPORTS FOR VIEWMODELS (CRITICAL) ---
import com.powersense.viewmodels.AuthViewModel
import com.powersense.viewmodels.HomeViewModel
import com.powersense.viewmodels.ProfileViewModel
import com.powersense.viewmodels.RelayViewModel
import com.powersense.viewmodels.ThemeViewModel

sealed class BottomNavItem(val route: String, val title: String, val icon: ImageVector) {
    object Home : BottomNavItem("home", "Home", Icons.Default.Home)
    object Simulation : BottomNavItem("simulation", "Simulation", Icons.Default.Bolt)
    object Metrics : BottomNavItem("metrics", "Metrics", Icons.Default.BarChart)
    object Settings : BottomNavItem("settings", "Settings", Icons.Default.Settings)
}

@Composable
fun MainAppScreen(
    appNavController: NavHostController,
    profileViewModel: ProfileViewModel = viewModel(),
    themeViewModel: ThemeViewModel = viewModel(),
    authViewModel: AuthViewModel = viewModel()
) {
    // Internal NavController for Bottom Tabs
    val bottomNavController = rememberNavController()
    val navBackStackEntry by bottomNavController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Shared ViewModels
    // We create them here so the same instance is reused across tabs
    val homeViewModel: HomeViewModel = viewModel()
    val relayViewModel: RelayViewModel = viewModel()

    val items = listOf(
        BottomNavItem.Home,
        BottomNavItem.Simulation,
        BottomNavItem.Metrics,
        BottomNavItem.Settings
    )

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                items.forEach { item ->
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = item.title) },
                        label = { Text(item.title) },
                        selected = currentRoute == item.route,
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.White,
                            selectedTextColor = PowerSensePurple,
                            indicatorColor = PowerSensePurple,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        onClick = {
                            bottomNavController.navigate(item.route) {
                                popUpTo(bottomNavController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = bottomNavController,
            startDestination = BottomNavItem.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            // 1. HOME TAB
            composable(BottomNavItem.Home.route) {
                HomeScreen(
                    profileViewModel = profileViewModel,
                    homeViewModel = homeViewModel,
                    relayViewModel = relayViewModel,
                    themeViewModel = themeViewModel
                )
            }

            // 2. SIMULATION (CONTROL) TAB
            composable(BottomNavItem.Simulation.route) {
                SimulationScreen(
                    relayViewModel = relayViewModel,
                    themeViewModel = themeViewModel
                )
            }

            // 3. METRICS TAB
            composable(BottomNavItem.Metrics.route) {
                MetricsScreen(
                    themeViewModel = themeViewModel
                )
            }

            // 4. SETTINGS TAB
            composable(BottomNavItem.Settings.route) {
                SettingsScreen(
                    onNavigateToProfile = { appNavController.navigate("profile") },
                    onLogout = {
                        authViewModel.logout()
                        appNavController.navigate("login") {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    appNavController = appNavController,
                    themeViewModel = themeViewModel,
                    profileViewModel = profileViewModel
                )
            }
        }
    }
}