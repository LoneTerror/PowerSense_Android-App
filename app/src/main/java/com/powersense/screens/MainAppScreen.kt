package com.powersense.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.SettingsApplications
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.powersense.screens.tabs.HomeScreen
import com.powersense.screens.tabs.MetricsScreen
import com.powersense.screens.tabs.SettingsScreen
import com.powersense.screens.tabs.SimulationScreen
import com.powersense.ui.theme.PowerSenseTheme
import com.powersense.viewmodels.ThemeViewModel
import com.powersense.viewmodels.RelayViewModel

sealed class BottomBarScreen(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    object Home : BottomBarScreen("home", "Home", Icons.Default.Home)
    object Simulation : BottomBarScreen("simulation", "Simulation", Icons.Outlined.SettingsApplications)
    object Metrics : BottomBarScreen("metrics", "Metrics", Icons.Outlined.BarChart)
    object Settings : BottomBarScreen("settings", "Settings", Icons.Default.Settings)
}

@Composable
fun MainAppScreen(
    appNavController: NavHostController
) {
    val navController = rememberNavController()
    val themeViewModel: ThemeViewModel = viewModel()

    // Create ONE instance of RelayViewModel here and pass it down
    val relayViewModel: RelayViewModel = viewModel()

    Scaffold(
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination

            val screens = listOf(
                BottomBarScreen.Home,
                BottomBarScreen.Simulation,
                BottomBarScreen.Metrics,
                BottomBarScreen.Settings,
            )

            NavigationBar {
                screens.forEach { screen ->
                    val isSelected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.title) },
                        label = { Text(screen.title) },
                        selected = isSelected,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.startDestinationId)
                                launchSingleTop = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = BottomBarScreen.Home.route,
            // Apply ONLY bottom padding to avoid double-padding at top
            modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding())
        ) {
            composable(BottomBarScreen.Home.route) {
                HomeScreen(relayViewModel = relayViewModel)
            }
            composable(BottomBarScreen.Simulation.route) {
                SimulationScreen(relayViewModel = relayViewModel)
            }
            composable(BottomBarScreen.Metrics.route) {
                MetricsScreen()
            }
            composable(BottomBarScreen.Settings.route) {
                SettingsScreen(
                    onNavigateToProfile = {
                        appNavController.navigate("profile")
                    },
                    onLogout = {
                        Firebase.auth.signOut()
                        appNavController.navigate("login") {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    themeViewModel = themeViewModel,
                    appNavController = appNavController
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainAppScreenPreview() {
    PowerSenseTheme {
        val fakeNavController = rememberNavController()
        MainAppScreen(appNavController = fakeNavController)
    }
}