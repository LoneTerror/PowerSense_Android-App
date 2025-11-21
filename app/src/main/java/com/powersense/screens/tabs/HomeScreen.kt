package com.powersense.screens.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.powersense.ui.theme.PowerSenseGreen
import com.powersense.ui.theme.PowerSensePurple
import com.powersense.viewmodels.ProfileViewModel
import java.util.Calendar

@Composable
fun HomeScreen(
    profileViewModel: ProfileViewModel = viewModel()
) {
    // 1. Get the user profile state
    val userProfile by profileViewModel.userProfile.collectAsState()

    // 2. Get the first name (or default to "User" if loading/empty)
    val displayName = remember(userProfile) {
        val fullName = userProfile?.fullName
        if (!fullName.isNullOrEmpty()) {
            fullName.split(" ").firstOrNull() ?: fullName
        } else {
            "User"
        }
    }

    // 3. Determine greeting based on time of day
    val greeting = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when (hour) {
            in 0..11 -> "Good Morning"
            in 12..16 -> "Good Afternoon"
            else -> "Good Evening"
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // --- Greeting ---
            item {
                Text(
                    text = "$greeting, $displayName", // Use dynamic greeting and name
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            // --- "Hero" Metric: Current Usage ---
            item {
                HeroUsageCard(
                    currentUsageW = 1245.7,
                    percentChange = -12.5
                )
            }

            // --- Quick Summary Cards ---
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    QuickMetricCard(
                        title = "Today's Total",
                        value = "8.2",
                        unit = "kWh",
                        modifier = Modifier.weight(1f)
                    )
                    QuickMetricCard(
                        title = "Est. Cost Today",
                        value = "$1.15",
                        unit = "USD",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // --- Favorite Devices ---
            item {
                FavoriteDevicesSection()
            }

            // --- Mini Chart Card ---
            item {
                MiniChartCard()
            }
        }
    }
}

// --- Helper Composables (Unchanged) ---

@Composable
fun HeroUsageCard(currentUsageW: Double, percentChange: Double) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = PowerSensePurple),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "CURRENTLY USING",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White.copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "%.1f W".format(currentUsageW),
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "%.1f%% vs yesterday".format(percentChange),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.9f)
            )
        }
    }
}

@Composable
fun QuickMetricCard(title: String, value: String, unit: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = unit,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun FavoriteDevicesSection() {
    val favoriteDevices = listOf(
        Pair("Living Room", Icons.Default.Lightbulb),
        Pair("Bedroom Fan", Icons.Default.WbSunny),
        Pair("Kitchen", Icons.Default.Lightbulb)
    )

    var switchStates by remember { mutableStateOf(mapOf("Living Room" to true, "Bedroom Fan" to false, "Kitchen" to true)) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Favorite Devices",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(favoriteDevices) { (name, icon) ->
                FavoriteDeviceCard(
                    name = name,
                    icon = icon,
                    isOn = switchStates[name] ?: false,
                    onToggle = {
                        switchStates = switchStates.toMutableMap().apply {
                            this[name] = !this[name]!!
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun FavoriteDeviceCard(name: String, icon: ImageVector, isOn: Boolean, onToggle: (Boolean) -> Unit) {
    Card(
        modifier = Modifier.size(140.dp, 160.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = icon,
                contentDescription = name,
                tint = if (isOn) PowerSenseGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(28.dp)
            )
            Column {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (isOn) "ON" else "OFF",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isOn) PowerSenseGreen else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = isOn,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = PowerSenseGreen,
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = MaterialTheme.colorScheme.outline
                ),
                modifier = Modifier.align(Alignment.End)
            )
        }
    }
}

@Composable
fun MiniChartCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Today's Usage",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "[24-Hour Line Chart Placeholder]",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}