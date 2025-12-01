package com.powersense.screens.tabs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.compose.style.ProvideChartStyle
import com.patrykandpatrick.vico.core.axis.AxisPosition
import com.patrykandpatrick.vico.core.axis.formatter.AxisValueFormatter
import com.patrykandpatrick.vico.core.entry.FloatEntry
import com.patrykandpatrick.vico.core.entry.entryModelOf
import com.patrykandpatrick.vico.compose.component.textComponent
import com.powersense.R
import com.powersense.data.HistoryPoint
import com.powersense.data.RelayDevice
import com.powersense.screens.components.PieChart
import com.powersense.screens.components.PieChartData
import com.powersense.ui.theme.PowerSenseGreen
import com.powersense.ui.theme.PowerSenseOrange
import com.powersense.ui.theme.PowerSensePurple
import com.powersense.viewmodels.HomeViewModel
import com.powersense.viewmodels.ProfileViewModel
import com.powersense.viewmodels.RelayViewModel
import com.powersense.viewmodels.ThemeOption
import com.powersense.viewmodels.ThemeViewModel
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@Composable
fun HomeScreen(
    profileViewModel: ProfileViewModel = viewModel(),
    homeViewModel: HomeViewModel = viewModel(),
    relayViewModel: RelayViewModel = viewModel(),
    themeViewModel: ThemeViewModel = viewModel()
) {
    val userProfile by profileViewModel.userProfile.collectAsState()
    val sensorData by homeViewModel.sensorData.collectAsState()
    val status by homeViewModel.connectionStatus.collectAsState()
    val connectionStart by homeViewModel.connectionStartTime.collectAsState()
    val currentTheme by themeViewModel.themeState.collectAsState()

    // Settings for Charts
    val isSummaryEnabled by themeViewModel.isSummaryEnabled.collectAsState()
    val summaryInterval by themeViewModel.summaryIntervalState.collectAsState()
    val homeChartData by homeViewModel.homeChartData.collectAsState()

    val allDevices by relayViewModel.relays.collectAsState()
    val favoriteDevices = remember(allDevices) { allDevices.filter { it.isFavorite } }

    var showStatusDialog by remember { mutableStateOf(false) }

    // Fetch chart data when interval changes or on load
    LaunchedEffect(isSummaryEnabled, summaryInterval) {
        if (isSummaryEnabled) {
            homeViewModel.fetchHomeChartData(summaryInterval.hours)
        }
    }

    val displayName = remember(userProfile) {
        val fullName = userProfile?.fullName
        if (!fullName.isNullOrEmpty()) fullName.split(" ").firstOrNull() ?: fullName else "User"
    }

    val greeting = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when (hour) {
            in 0..11 -> "Good Morning"
            in 12..16 -> "Good Afternoon"
            else -> "Good Evening"
        }
    }

    // --- MOCK APPLIANCE USAGE PIE DATA ---
    val applianceUsageData = remember(allDevices, summaryInterval) {
        if (isSummaryEnabled && allDevices.isNotEmpty()) {
            val totalHours = summaryInterval.hours.toFloat()
            allDevices.take(3).mapIndexed { index, device ->
                val usage = if(device.isOn) totalHours * 0.8f else totalHours * 0.1f // Mock values
                val color = when(index % 3) {
                    0 -> PowerSensePurple
                    1 -> PowerSenseGreen
                    else -> PowerSenseOrange
                }
                PieChartData(device.name, usage, color)
            }
        } else emptyList()
    }

    // --- HEADER COLOR LOGIC ---
    val systemDark = isSystemInDarkTheme()
    val isAppInDarkTheme = when (currentTheme) {
        ThemeOption.Light -> false
        ThemeOption.Dark -> true
        ThemeOption.System -> systemDark
    }

    val aestheticDarkHeader = Color(0xFF4E463F)
    val headerContainerColor = if (isAppInDarkTheme) MaterialTheme.colorScheme.background else aestheticDarkHeader
    val headerContentColor = Color.White

    // --- CHART LABEL COLOR LOGIC ---
    val chartLabelColor = if (isAppInDarkTheme) Color(0xFFE0E0E0) else Color(0xFF4A4036)


    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Surface(
                color = headerContainerColor,
                contentColor = headerContentColor,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .statusBarsPadding()
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_powersense_logo),
                            contentDescription = "PowerSense Logo",
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                        Spacer(modifier = Modifier.width(12.dp))

                        Text(
                            text = "$greeting, $displayName",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = headerContentColor
                        )
                    }

                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { showStatusDialog = true },
                        color = if (status == "Connected") PowerSenseGreen.copy(alpha = 0.2f) else Color.Red.copy(alpha = 0.1f),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (status == "Connected") PowerSenseGreen else Color.Red)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = status,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (status == "Connected") PowerSenseGreen else Color.Red,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(16.dp)) }

            item { HeroUsageCard(currentUsageW = sensorData?.power ?: 0.0, voltage = sensorData?.voltage ?: 0.0) }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    QuickMetricCard("Energy (kWh)", "%.2f".format(sensorData?.energy ?: 0.0), "kWh", Modifier.weight(1f))
                    QuickMetricCard("Current (A)", "%.2f".format(sensorData?.current ?: 0.0), "Amps", Modifier.weight(1f))
                }
            }

            // --- REORDERED: Favorite Devices Section ---
            if (favoriteDevices.isNotEmpty()) {
                item { Text("Favorite Devices", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.padding(bottom = 8.dp)) }
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(favoriteDevices) { device ->
                            FavoriteDeviceCard(device, { relayViewModel.toggleRelay(device) }, { relayViewModel.toggleFavorite(device) })
                        }
                    }
                }
            }

            // --- SUMMARY CHARTS ---
            if (isSummaryEnabled) {
                item {
                    Text(
                        text = "Usage Summary (${summaryInterval.label})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                // Line Chart (Power) - USING RENAMED HomeLineChart to avoid conflict
                item {
                    HomeLineChart(
                        title = "Power Trend (W)",
                        dataPoints = homeChartData?.powerHistory ?: emptyList(),
                        lineColor = PowerSenseGreen,
                        labelColor = chartLabelColor
                    )
                }

                // Pie Chart (Appliance Usage Time)
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "Appliance On-Time",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            if (applianceUsageData.isNotEmpty()) {
                                PieChart(data = applianceUsageData)
                            } else {
                                Text("No active appliances found.", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }

        if (showStatusDialog) {
            StatusDetailsDialog(
                status = status,
                startTime = connectionStart,
                onDismiss = { showStatusDialog = false }
            )
        }
    }
}

// ... (Helpers: StatusDetailsDialog, HeroUsageCard, QuickMetricCard, FavoriteDeviceCard, MiniChartCard - kept as is) ...
@Composable
fun StatusDetailsDialog(status: String, startTime: Long?, onDismiss: () -> Unit) {
    var timeElapsed by remember { mutableStateOf("00:00:00") }
    LaunchedEffect(startTime) {
        if (startTime != null) {
            while (true) {
                val diff = System.currentTimeMillis() - startTime
                val seconds = (diff / 1000) % 60
                val minutes = (diff / (1000 * 60)) % 60
                val hours = (diff / (1000 * 60 * 60))
                timeElapsed = "%02d:%02d:%02d".format(hours, minutes, seconds)
                delay(1000)
            }
        }
    }
    val formattedStartTime = remember(startTime) {
        if (startTime != null) SimpleDateFormat("h:mm:ss a", Locale.getDefault()).format(Date(startTime)) else "--:--"
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(if (status == "Connected") Icons.Default.CheckCircle else Icons.Default.Error, null, tint = if (status == "Connected") PowerSenseGreen else Color.Red, modifier = Modifier.size(48.dp)) },
        title = { Text("Connection Status") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(status.uppercase(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = if (status == "Connected") PowerSenseGreen else Color.Red)
                if (status == "Connected") {
                    Spacer(modifier = Modifier.height(16.dp)); HorizontalDivider(); Spacer(modifier = Modifier.height(16.dp))
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) { Text("Connected At:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant); Text(formattedStartTime, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold) }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) { Text("Uptime:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant); Text(timeElapsed, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = PowerSensePurple) }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

@Composable
fun HeroUsageCard(currentUsageW: Double, voltage: Double) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = PowerSensePurple), elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)) {
        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(text = "REAL-TIME POWER", style = MaterialTheme.typography.titleMedium, color = Color.White.copy(alpha = 0.8f))
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "%.1f W".format(currentUsageW), style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Voltage: %.1f V".format(voltage), style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.9f))
        }
    }
}

@Composable
fun QuickMetricCard(title: String, value: String, unit: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            Text(unit, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun FavoriteDeviceCard(device: RelayDevice, onToggle: () -> Unit, onFavoriteToggle: () -> Unit) {
    Card(modifier = Modifier.size(150.dp, 170.dp), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))) {
        Column(modifier = Modifier.padding(12.dp).fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.Lightbulb, contentDescription = null, tint = if (device.isOn) PowerSenseGreen else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
                Icon(imageVector = Icons.Default.Star, contentDescription = "Unfavorite", tint = Color(0xFFFFC107), modifier = Modifier.size(24.dp).clickable { onFavoriteToggle() })
            }
            Column {
                Text(text = device.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
                Text(text = if (device.isOn) "ON" else "OFF", style = MaterialTheme.typography.bodySmall, color = if (device.isOn) PowerSenseGreen else MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = device.isOn, onCheckedChange = { onToggle() }, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = PowerSenseGreen, uncheckedThumbColor = Color.White, uncheckedTrackColor = MaterialTheme.colorScheme.outline), modifier = Modifier.align(Alignment.End))
        }
    }
}

@Composable
fun MiniChartCard() {
    // This chart logic was duplicated/conflicting, keeping as placeholder if needed,
    // otherwise relying on the dynamic charts added above in the main column.
    // To avoid confusion, I'll just render it as a static card for "Quick View"
    // or you can remove it if the dynamic charts cover it.
    // For now, leaving it to prevent 'Unresolved reference' if called elsewhere.
}

// --- RENAMED TO HomeLineChart TO FIX CONFLICT ---
@Composable
fun HomeLineChart(
    title: String,
    dataPoints: List<HistoryPoint>,
    lineColor: Color,
    labelColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth().height(300.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.height(16.dp))
            if (dataPoints.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No Data Available", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                val chartEntryModel = remember(dataPoints) {
                    val entries = dataPoints.mapIndexed { index, point -> FloatEntry(x = index.toFloat(), y = point.value.toFloat()) }
                    entryModelOf(entries)
                }

                // Calculate spacing
                val spacing = remember(dataPoints.size) {
                    if (dataPoints.size > 6) dataPoints.size / 6 else 1
                }

                val dateTimeFormatter = remember(dataPoints) {
                    AxisValueFormatter<AxisPosition.Horizontal.Bottom> { value, _ ->
                        val index = value.toInt()

                        if (index % spacing != 0 && index != dataPoints.lastIndex) {
                            return@AxisValueFormatter ""
                        }

                        val point = dataPoints.getOrNull(index)
                        if (point != null) {
                            try {
                                val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
                                parser.timeZone = TimeZone.getTimeZone("UTC")
                                val date = parser.parse(point.timestamp)
                                SimpleDateFormat("hh:mm a", Locale.getDefault()).format(date ?: java.util.Date())
                            } catch (e: Exception) { "" }
                        } else { "" }
                    }
                }

                val axisLabel = textComponent(
                    color = labelColor,
                    textSize = 10.sp,
                )

                ProvideChartStyle {
                    Chart(
                        chart = lineChart(lines = listOf(com.patrykandpatrick.vico.compose.chart.line.lineSpec(lineColor = lineColor))),
                        model = chartEntryModel,
                        startAxis = rememberStartAxis(
                            label = axisLabel,
                            valueFormatter = { value, _ -> "%.1f".format(value) }
                        ),
                        bottomAxis = rememberBottomAxis(
                            label = axisLabel,
                            valueFormatter = dateTimeFormatter,
                            guideline = null
                        ),
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}