package com.powersense.screens.tabs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState // <-- IMPORT
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel // <-- IMPORT
import com.powersense.ui.theme.DarkText
import com.powersense.ui.theme.LightGreyBackground
import com.powersense.ui.theme.LightText
import com.powersense.ui.theme.PowerSenseGreen
import com.powersense.ui.theme.PowerSenseGrey
import com.powersense.ui.theme.PowerSenseTheme
import com.powersense.viewmodels.ThemeOption // <-- IMPORT
import com.powersense.viewmodels.ThemeViewModel // <-- IMPORT

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateToProfile: () -> Unit,
    onLogout: () -> Unit,
    // Get the ViewModel (it will be the same instance as MainActivity)
    themeViewModel: ThemeViewModel = viewModel()
) {
    // Collect the theme state from the ViewModel
    val currentTheme by themeViewModel.themeState.collectAsState()

    // State for the toggles
    var isEcoModeOn by remember { mutableStateOf(true) }
    var isDailySummaryOn by remember { mutableStateOf(true) }
    var isAlertsOn by remember { mutableStateOf(true) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background // Use theme color
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background // Use theme color
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            // --- PROFILE SECTION ---
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = "Profile",
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.onBackground // Use theme color
                        )
                        Text(
                            "Profile",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground, // Use theme color
                            modifier = Modifier.padding(start = 16.dp)
                        )
                    }
                    TextButton(
                        onClick = onNavigateToProfile,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Edit Profile")
                    }
                }
            }

            // --- DISPLAY SECTION ---
            item {
                SectionHeader("Display")
            }
            item {
                // Pass the current theme and the update function
                ThemeSelector(
                    selectedTheme = currentTheme,
                    onThemeChange = { themeViewModel.setTheme(it) }
                )
            }

            // --- POWER MANAGEMENT ---
            item {
                SectionHeader("Power Management")
            }
            item {
                SettingsRow(
                    title = "Eco-Mode Schedule",
                    subtitle = "Activate power-saving during off-peak hours.",
                    trailingContent = {
                        Switch(
                            checked = isEcoModeOn,
                            onCheckedChange = { isEcoModeOn = it },
                            colors = SwitchDefaults.colors(checkedTrackColor = PowerSenseGreen)
                        )
                    }
                )
            }

            // --- NOTIFICATIONS ---
            item {
                SectionHeader("Notifications")
            }
            item {
                SettingsRow(
                    title = "Daily Consumption Summary",
                    subtitle = "Receive a daily report of your power usage.",
                    trailingContent = {
                        Switch(
                            checked = isDailySummaryOn,
                            onCheckedChange = { isDailySummaryOn = it },
                            colors = SwitchDefaults.colors(checkedTrackColor = PowerSenseGreen)
                        )
                    }
                )
            }
            item {
                SettingsRow(
                    title = "Abnormal Usage Alerts",
                    subtitle = "Get notified of unusual power spikes or drops.",
                    trailingContent = {
                        Switch(
                            checked = isAlertsOn,
                            onCheckedChange = { isAlertsOn = it },
                            colors = SwitchDefaults.colors(checkedTrackColor = PowerSenseGreen)
                        )
                    }
                )
            }

            // --- ACCOUNT ---
            item {
                SectionHeader("Account")
            }
            item {
                SettingsRow(
                    title = "Manage Profile",
                    subtitle = "Update your personal information.",
                    onClick = onNavigateToProfile,
                    trailingContent = {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Navigate",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant // Use theme color
                        )
                    }
                )
            }
            item {
                SettingsRow(
                    title = "Change Password",
                    subtitle = "Update your account password.",
                    onClick = { /* TODO: Navigate to Change Password Screen */ },
                    trailingContent = {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Navigate",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant // Use theme color
                        )
                    }
                )
            }
            item {
                SettingsRow(
                    title = "Logout",
                    subtitle = "Sign out of your account.",
                    onClick = onLogout,
                    trailingContent = {
                        Icon(
                            Icons.AutoMirrored.Filled.Logout,
                            contentDescription = "Logout",
                            tint = Color.Red
                        )
                    }
                )
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

// --- Reusable Components for this Screen ---

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground, // Use theme color
        modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
    )
}

@Composable
fun ThemeSelector(
    selectedTheme: ThemeOption, // ACCEPT THE THEME STATE
    onThemeChange: (ThemeOption) -> Unit // ACCEPT THE UPDATE FUNCTION
) {
    Column {
        Text(
            "Theme",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground // Use theme color
        )
        Text(
            "Choose your preferred app theme.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant // Use theme color
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ThemeButton(
                text = "Light",
                isSelected = selectedTheme == ThemeOption.Light,
                onClick = { onThemeChange(ThemeOption.Light) } // UPDATE ONCLICK
            )
            ThemeButton(
                text = "Dark",
                isSelected = selectedTheme == ThemeOption.Dark,
                onClick = { onThemeChange(ThemeOption.Dark) } // UPDATE ONCLICK
            )
            ThemeButton(
                text = "System",
                isSelected = selectedTheme == ThemeOption.System,
                onClick = { onThemeChange(ThemeOption.System) } // UPDATE ONCLICK
            )
        }
    }
}

@Composable
fun ThemeButton(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) PowerSenseGreen else MaterialTheme.colorScheme.surfaceVariant, // Use theme color
            contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant // Use theme color
        )
    ) {
        Text(text)
    }
}

@Composable
fun SettingsRow(
    title: String,
    subtitle: String,
    onClick: (() -> Unit)? = null,
    trailingContent: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) Modifier.clickable { onClick() } else Modifier
            )
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onBackground // Use theme color
            )
            Text(
                subtitle,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant // Use theme color
            )
        }
        trailingContent()
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    PowerSenseTheme {
        SettingsScreen(onNavigateToProfile = {}, onLogout = {})
    }
}