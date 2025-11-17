package com.powersense.screens.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.powersense.ui.theme.PowerSenseGreen
import com.powersense.ui.theme.PowerSensePurple
import com.powersense.ui.theme.PowerSenseTheme

@Composable
fun HomeScreen() {
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
                    text = "Good morning, Alex", // TODO: Replace with dynamic user name
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            // --- "Hero" Metric: Current Usage ---
            item {
                HeroUsageCard(
                    currentUsageW = 1245.7, // TODO: Replace with real-time data
                    percentChange = -12.5 // TODO: Replace with real data
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
                        value = "8.2", // TODO: Replace with real data
                        unit = "kWh",
                        modifier = Modifier.weight(1f)
                    )
                    QuickMetricCard(
                        title = "Est. Cost Today",
                        value = "$1.15", // TODO: Replace with real data
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

// --- Composable for the Main "Hero" Card ---
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
            // TODO: Add logic for up/down arrow and color
            Text(
                text = "%.1f%% vs yesterday".format(percentChange),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.9f)
            )
        }
    }
}

// --- Composable for the small summary cards ---
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

// --- Composable for the "Favorite Devices" section ---
@Composable
fun FavoriteDevicesSection() {
    // TODO: This data would come from a ViewModel
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
                        // TODO: Add real logic to update ViewModel/backend
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

// --- Composable for the Mini Chart ---
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
            // --- Chart Placeholder ---
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
            // TODO: Add "View Details" button that navigates to Metrics tab
        }
    }
}

@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_NO)
@Composable
fun HomeScreenPreviewLight() {
    PowerSenseTheme {
        HomeScreen()
    }
}

@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
fun HomeScreenPreviewDark() {
    PowerSenseTheme(themeOption = com.powersense.viewmodels.ThemeOption.Dark) {
        HomeScreen()
    }
}