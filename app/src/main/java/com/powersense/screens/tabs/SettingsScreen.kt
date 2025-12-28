package com.powersense.screens.tabs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.powersense.ui.theme.PowerSenseGreen
import com.powersense.ui.theme.PowerSensePeach
import com.powersense.ui.theme.PowerSensePurple
import com.powersense.ui.theme.PowerSenseTheme
import com.powersense.viewmodels.ProfileViewModel
import com.powersense.viewmodels.SummaryInterval
import com.powersense.viewmodels.ThemeOption
import com.powersense.viewmodels.ThemeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateToProfile: () -> Unit,
    onLogout: () -> Unit,
    appNavController: NavHostController,
    themeViewModel: ThemeViewModel = viewModel(),
    profileViewModel: ProfileViewModel = viewModel()
) {
    val currentTheme by themeViewModel.themeState.collectAsState()
    val userProfile by profileViewModel.userProfile.collectAsState()

    val isSummaryEnabled by themeViewModel.isSummaryEnabled.collectAsState()
    val summaryInterval by themeViewModel.summaryIntervalState.collectAsState()

    val isHapticsEnabled by themeViewModel.isHapticsEnabled.collectAsState()

    var isEcoModeOn by remember { mutableStateOf(true) }
    var isAlertsOn by remember { mutableStateOf(true) }

    var showLogoutDialog by remember { mutableStateOf(false) }

    // --- HEADER COLOR LOGIC ---
    val systemDark = isSystemInDarkTheme()
    val isAppInDarkTheme = when (currentTheme) {
        ThemeOption.Light -> false
        ThemeOption.Dark -> true
        ThemeOption.System -> systemDark
    }

    val aestheticLightModeHeader = Color(0xFF4E463F)
    val aestheticDarkModeHeader = Color(0xFF202124)

    val headerContainerColor = if (isAppInDarkTheme) aestheticDarkModeHeader else aestheticLightModeHeader
    val headerContentColor = Color.White

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Settings",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = headerContainerColor,
                    titleContentColor = headerContentColor,
                    navigationIconContentColor = headerContentColor,
                    actionIconContentColor = headerContentColor
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            // --- PROFILE SECTION (UPDATED) ---
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp)
                        .clickable { onNavigateToProfile() },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // --- ADVANCED AVATAR ---
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .border(2.dp, PowerSensePurple, CircleShape)
                                .padding(2.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center // Center content if fallback icon
                        ) {
                            if (userProfile?.profileImageUrl.isNullOrEmpty()) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "Profile",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(12.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(userProfile?.profileImageUrl)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = "Profile Picture",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop,
                                    // Add placeholder/error handling to visualize loading state
                                    error = null, // You could pass a painterResource here if you had a fallback drawable
                                    onLoading = { /* Optional: Show loading spinner logic */ },
                                    onError = {
                                        // If error, fallback to icon logic naturally by ensuring state is handled
                                        // For simple AsyncImage, if it fails, it just shows nothing unless 'error' param is set.
                                        // Since we wrapped it in 'if (url.isNullOrEmpty())',
                                        // if the URL is valid but load fails (e.g. 404), it might show blank.
                                        // Let's improve:
                                    }
                                )

                                // Improved Fallback for Load Failure:
                                // Actually, standard AsyncImage pattern is cleaner:
                                /*
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(userProfile?.profileImageUrl)
                                        .crossfade(true)
                                        .error(R.drawable.ic_launcher_foreground) // Example fallback if you had one
                                        .build(),
                                    ...
                                )
                                */
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column {
                            Text(
                                text = userProfile?.fullName ?: "User",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = userProfile?.email ?: "No Email",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Edit",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // --- DISPLAY SECTION ---
            item { SectionHeader("Display") }
            item {
                ThemeSelector(
                    selectedTheme = currentTheme,
                    onThemeChange = { themeViewModel.setTheme(it) }
                )
            }

            // --- INTERFACE / FEEDBACK ---
            item { SectionHeader("Interface") }

            item {
                SettingsRow(
                    title = "Daily Consumption Summary",
                    subtitle = "Receive a periodic report of usage.",
                    trailingContent = {
                        Switch(
                            checked = isSummaryEnabled,
                            onCheckedChange = { themeViewModel.setSummaryEnabled(it) },
                            colors = SwitchDefaults.colors(checkedTrackColor = PowerSenseGreen)
                        )
                    }
                )
            }

            if (isSummaryEnabled) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp, horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Summary Interval",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        IntervalDropdown(
                            currentInterval = summaryInterval,
                            onIntervalSelected = { themeViewModel.setSummaryInterval(it) }
                        )
                    }
                }
            }

            // NEW: Haptics Toggle
            item {
                SettingsRow(
                    title = "Haptic Feedback",
                    subtitle = "Vibrate when toggling switches.",
                    trailingContent = {
                        Switch(
                            checked = isHapticsEnabled,
                            onCheckedChange = { themeViewModel.setHapticsEnabled(it) },
                            colors = SwitchDefaults.colors(checkedTrackColor = PowerSenseGreen)
                        )
                    }
                )
            }

            // --- POWER MANAGEMENT ---
            item { SectionHeader("Power Management") }
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
            item { SectionHeader("Notifications") }

            // Max Sensor Capacity Alert
            item {
                SettingsRow(
                    title = "Max Sensor Capacity Alert",
                    subtitle = "Notify if total current exceeds 30A.",
                    trailingContent = {
                        val isMaxEnabled by themeViewModel.isMaxSensorSpikeEnabled.collectAsState()
                        Switch(
                            checked = isMaxEnabled,
                            onCheckedChange = { themeViewModel.setMaxSensorSpikeEnabled(it) },
                            colors = SwitchDefaults.colors(checkedTrackColor = PowerSenseGreen)
                        )
                    }
                )
            }

            // Per Appliance Alert
            item {
                SettingsRow(
                    title = "Appliance Usage Alerts",
                    subtitle = "Notify if specific devices exceed their set thresholds.",
                    trailingContent = {
                        val isApplianceEnabled by themeViewModel.isPerApplianceAlertEnabled.collectAsState()
                        Switch(
                            checked = isApplianceEnabled,
                            onCheckedChange = { themeViewModel.setPerApplianceAlertEnabled(it) },
                            colors = SwitchDefaults.colors(checkedTrackColor = PowerSenseGreen)
                        )
                    }
                )
            }


            // --- ACCOUNT ---
            item { SectionHeader("Account") }
            item {
                SettingsRow(
                    title = "Change Password",
                    subtitle = "Update your account password.",
                    onClick = { appNavController.navigate("change_password") },
                    trailingContent = {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Navigate",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
            }
            item {
                SettingsRow(
                    title = "Logout",
                    subtitle = "Sign out of your account.",
                    onClick = { showLogoutDialog = true },
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

        if (showLogoutDialog) {
            AlertDialog(
                onDismissRequest = { showLogoutDialog = false },
                title = { Text("Log Out") },
                text = { Text("Are you sure you want to log out of PowerSense?") },
                confirmButton = {
                    Button(
                        onClick = {
                            showLogoutDialog = false
                            onLogout()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PowerSensePeach)
                    ) {
                        Text("Log Out", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showLogoutDialog = false }) {
                        Text("Cancel", color = MaterialTheme.colorScheme.onSurface)
                    }
                },
                containerColor = MaterialTheme.colorScheme.surface
            )
        }
    }
}

// ... (Rest of Helpers remain unchanged) ...
@Composable
fun IntervalDropdown(
    currentInterval: SummaryInterval,
    onIntervalSelected: (SummaryInterval) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.wrapContentSize(Alignment.TopStart)) {
        Button(
            onClick = { expanded = true },
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(currentInterval.label)
            Spacer(modifier = Modifier.size(8.dp))
            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
        ) {
            SummaryInterval.entries.forEach { interval ->
                DropdownMenuItem(
                    text = { Text(interval.label) },
                    onClick = {
                        onIntervalSelected(interval)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
    )
}

@Composable
fun ThemeSelector(
    selectedTheme: ThemeOption,
    onThemeChange: (ThemeOption) -> Unit
) {
    Column {
        Text(
            "Theme",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            "Choose your preferred app theme.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ThemeButton(
                text = "Light",
                isSelected = selectedTheme == ThemeOption.Light,
                onClick = { onThemeChange(ThemeOption.Light) }
            )
            ThemeButton(
                text = "Dark",
                isSelected = selectedTheme == ThemeOption.Dark,
                onClick = { onThemeChange(ThemeOption.Dark) }
            )
            ThemeButton(
                text = "System",
                isSelected = selectedTheme == ThemeOption.System,
                onClick = { onThemeChange(ThemeOption.System) }
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
            containerColor = if (isSelected) PowerSenseGreen else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
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
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                subtitle,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        trailingContent()
    }
}