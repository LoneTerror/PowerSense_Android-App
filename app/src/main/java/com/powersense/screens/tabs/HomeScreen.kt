package com.powersense.screens.tabs

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.animateScrollBy // FIX: Added this import
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.compose.chart.scroll.rememberChartScrollState
import com.patrykandpatrick.vico.compose.component.textComponent
import com.patrykandpatrick.vico.compose.style.ProvideChartStyle
import com.patrykandpatrick.vico.core.axis.AxisPosition
import com.patrykandpatrick.vico.core.axis.formatter.AxisValueFormatter
import com.patrykandpatrick.vico.core.entry.FloatEntry
import com.patrykandpatrick.vico.core.entry.entryModelOf
import com.powersense.R
import com.powersense.data.HistoryPoint
import com.powersense.data.RelayDevice
import com.powersense.screens.components.PieChart
import com.powersense.screens.components.PieChartData
import com.powersense.ui.theme.PowerSenseGreen
import com.powersense.ui.theme.PowerSenseOrange
import com.powersense.ui.theme.PowerSensePurple
import com.powersense.utils.FeedbackUtils
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

    val isSummaryEnabled by themeViewModel.isSummaryEnabled.collectAsState()
    val summaryInterval by themeViewModel.summaryIntervalState.collectAsState()
    val homeChartData by homeViewModel.homeChartData.collectAsState()

    val relayUsage by homeViewModel.relayUsage.collectAsState()

    val allDevices by relayViewModel.relays.collectAsState()
    val favoriteDevices = remember(allDevices) { allDevices.filter { it.isFavorite } }

    val isHapticsEnabled by themeViewModel.isHapticsEnabled.collectAsState()
    val context = LocalContext.current

    val weatherData by homeViewModel.weatherState.collectAsState()
    val searchError by homeViewModel.searchError.collectAsState()
    val citySuggestions by homeViewModel.citySuggestions.collectAsState()

    var showLocationDialog by remember { mutableStateOf(false) }
    var locationSearchQuery by remember { mutableStateOf("") }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
            homeViewModel.fetchLocationAndWeather(context)
        }
    }

    LaunchedEffect(Unit) {
        locationPermissionLauncher.launch(
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        )
    }

    LaunchedEffect(searchError) {
        if (searchError != null) {
            Toast.makeText(context, searchError, Toast.LENGTH_SHORT).show()
            homeViewModel.clearSearchError()
        }
    }

    var showStatusMenu by remember { mutableStateOf(false) }

    LaunchedEffect(isSummaryEnabled, summaryInterval) {
        if (isSummaryEnabled) homeViewModel.fetchHomeChartData(summaryInterval.hours)
    }

    val displayName = remember(userProfile) {
        val fullName = userProfile?.fullName
        if (!fullName.isNullOrEmpty()) fullName.split(" ").firstOrNull() ?: fullName else "User"
    }

    val greetingMessage = remember(displayName) {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when (hour) {
            in 0..11 -> "Good Morning"
            in 12..16 -> "Good Afternoon"
            else -> "Good Evening"
        }
    }

    var showGreeting by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        showGreeting = true
        delay(3000)
        showGreeting = false
    }

    val applianceUsageData = remember(relayUsage, allDevices) {
        if (isSummaryEnabled && relayUsage != null && allDevices.isNotEmpty()) {
            val list = mutableListOf<PieChartData>()
            val dev1 = allDevices.firstOrNull()
            if (dev1 != null && relayUsage!!.relay1Hours > 0) {
                list.add(PieChartData(dev1.name, relayUsage!!.relay1Hours.toFloat(), PowerSensePurple))
            }
            val dev2 = allDevices.getOrNull(1)
            if (dev2 != null && relayUsage!!.relay2Hours > 0) {
                list.add(PieChartData(dev2.name, relayUsage!!.relay2Hours.toFloat(), PowerSenseGreen))
            }
            list
        } else emptyList()
    }

    val systemDark = isSystemInDarkTheme()
    val isAppInDarkTheme = when (currentTheme) {
        ThemeOption.Light -> false
        ThemeOption.Dark -> true
        ThemeOption.System -> systemDark
    }

    val aestheticDarkHeader = Color(0xFF4E463F)
    val headerContainerColor = if (isAppInDarkTheme) MaterialTheme.colorScheme.background else aestheticDarkHeader
    val headerContentColor = Color.White
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
                    modifier = Modifier.statusBarsPadding().fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_powersense_logo),
                            contentDescription = "Logo",
                            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp))
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        AnimatedContent(
                            targetState = showGreeting,
                            transitionSpec = {
                                (slideInVertically { -it } + fadeIn(tween(600))).togetherWith(
                                    slideOutVertically { it } + fadeOut(tween(600))
                                )
                            }, label = "Header"
                        ) { isGreeting ->
                            Text(
                                text = if (isGreeting) greetingMessage else "PowerSense",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = headerContentColor
                            )
                        }
                    }
                    Box {
                        Box(
                            modifier = Modifier.size(12.dp).clip(CircleShape)
                                .background(if (status == "Connected") PowerSenseGreen else Color.Red)
                                .clickable { showStatusMenu = true }
                        )
                        DropdownMenu(
                            expanded = showStatusMenu,
                            onDismissRequest = { showStatusMenu = false },
                            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                        ) {
                            DropdownMenuItem(
                                text = { Text(if (status == "Connected") "System Online" else "System Offline", fontWeight = FontWeight.Bold, color = if (status == "Connected") PowerSenseGreen else Color.Red) },
                                onClick = {}, enabled = false
                            )
                            if (status == "Connected") {
                                var timeElapsed by remember { mutableStateOf("00:00:00") }
                                LaunchedEffect(connectionStart, showStatusMenu) {
                                    if (connectionStart != null && showStatusMenu) {
                                        while (true) {
                                            val diff = System.currentTimeMillis() - connectionStart!!
                                            val h = diff / 3600000
                                            val m = (diff % 3600000) / 60000
                                            val s = (diff % 60000) / 1000
                                            timeElapsed = "%02d:%02d:%02d".format(h, m, s)
                                            delay(1000)
                                        }
                                    }
                                }
                                val formattedStartTime = remember(connectionStart) { if (connectionStart != null) SimpleDateFormat("h:mm:ss a", Locale.getDefault()).format(Date(connectionStart!!)) else "--:--" }
                                Divider()
                                DropdownMenuItem(text = { Column { Text("Connected At:", style = MaterialTheme.typography.labelSmall); Text(formattedStartTime, style = MaterialTheme.typography.bodyMedium) } }, onClick = {})
                                DropdownMenuItem(text = { Column { Text("Uptime:", style = MaterialTheme.typography.labelSmall); Text(timeElapsed, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = PowerSensePurple) } }, onClick = {})
                            } else {
                                DropdownMenuItem(text = { Text("Check internet connection.", style = MaterialTheme.typography.bodySmall) }, onClick = {})
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(16.dp)) }

            if (weatherData != null) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { locationSearchQuery = ""; homeViewModel.clearSuggestions(); showLocationDialog = true },
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.5f))
                    ) {
                        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Outlined.LocationOn, contentDescription = null, tint = PowerSenseOrange, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(weatherData!!.locationName, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                }
                                Text("Tap to change", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.WbSunny, contentDescription = null, tint = Color(0xFFFFB300), modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("${weatherData!!.temperature}°C", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                }
            }

            item { HeroUsageCard(sensorData?.power ?: 0.0, sensorData?.voltage ?: 0.0) }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    QuickMetricCard("Energy (kWh)", "%.2f".format(sensorData?.energy ?: 0.0), "kWh", Modifier.weight(1f))
                    QuickMetricCard("Current (A)", "%.2f".format(sensorData?.current ?: 0.0), "Amps", Modifier.weight(1f))
                }
            }

            if (favoriteDevices.isNotEmpty()) {
                item { Text("Favorite Devices", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground) }
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(favoriteDevices) { device ->
                            FavoriteDeviceCard(device, {
                                FeedbackUtils.triggerFeedback(context, !device.isOn, isHapticsEnabled)
                                relayViewModel.toggleRelay(device)
                            }, { relayViewModel.toggleFavorite(device) })
                        }
                    }
                }
            }

            if (isSummaryEnabled) {
                item { Text("Usage Summary (${summaryInterval.label})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground) }

                item {
                    HomeLineChart(
                        title = "Power Trend (W)",
                        dataPoints = homeChartData?.powerHistory ?: emptyList(),
                        lineColor = PowerSenseGreen,
                        labelColor = chartLabelColor
                    )
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Appliance On-Time (Hours)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Spacer(modifier = Modifier.height(16.dp))
                            if (applianceUsageData.isNotEmpty()) {
                                PieChart(data = applianceUsageData)
                            } else {
                                Text("No usage data recorded yet.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(24.dp)) }
        }

        if (showLocationDialog) {
            AlertDialog(
                onDismissRequest = { showLocationDialog = false },
                title = { Text("Change Location") },
                text = {
                    Column {
                        OutlinedTextField(
                            value = locationSearchQuery,
                            onValueChange = { locationSearchQuery = it; homeViewModel.searchCities(context, it) },
                            label = { Text("City Name") },
                            leadingIcon = { Icon(Icons.Default.Search, null) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (citySuggestions.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            LazyColumn(modifier = Modifier.heightIn(max = 200.dp).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.3f))) {
                                items(citySuggestions) { city ->
                                    Text(city, modifier = Modifier.fillMaxWidth().clickable { homeViewModel.updateLocation(context, city); showLocationDialog = false }.padding(12.dp))
                                    Divider()
                                }
                            }
                        }
                    }
                },
                confirmButton = { Button(onClick = { homeViewModel.updateLocation(context, locationSearchQuery); showLocationDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = PowerSensePurple)) { Text("Search") } },
                dismissButton = { TextButton(onClick = { showLocationDialog = false }) { Text("Cancel") } }
            )
        }
    }
}

// --- HELPER COMPOSABLES ---

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
fun HomeLineChart(title: String, dataPoints: List<HistoryPoint>, lineColor: Color, labelColor: Color) {
    Card(
        modifier = Modifier.fillMaxWidth().height(300.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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

                // FIXED SCROLLING
                val scrollState = rememberChartScrollState()
                LaunchedEffect(dataPoints.size) {
                    // Scroll to the very end
                    scrollState.animateScrollBy(scrollState.maxValue - scrollState.value)
                }

                val dateTimeFormatter = remember(dataPoints) {
                    AxisValueFormatter<AxisPosition.Horizontal.Bottom> { value, _ ->
                        val index = value.toInt()
                        val point = dataPoints.getOrNull(index)
                        if (point != null) {
                            try {
                                val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
                                parser.timeZone = TimeZone.getTimeZone("UTC")
                                val date = parser.parse(point.timestamp)
                                SimpleDateFormat("hh:mm a", Locale.getDefault()).format(date ?: Date())
                            } catch (e: Exception) { "" }
                        } else ""
                    }
                }

                val axisLabel = textComponent(color = labelColor, textSize = 10.sp)

                ProvideChartStyle {
                    Chart(
                        chart = lineChart(lines = listOf(com.patrykandpatrick.vico.compose.chart.line.lineSpec(lineColor = lineColor))),
                        model = chartEntryModel,
                        startAxis = rememberStartAxis(label = axisLabel, valueFormatter = { value, _ -> "%.1f".format(value) }),
                        bottomAxis = rememberBottomAxis(label = axisLabel, valueFormatter = dateTimeFormatter, guideline = null),
                        chartScrollState = scrollState,
                        isZoomEnabled = true,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}