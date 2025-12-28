package com.powersense.screens.tabs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.gestures.animateScrollBy // FIX: Added this import
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FabPosition
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.compose.chart.scroll.rememberChartScrollState
import com.patrykandpatrick.vico.compose.style.ProvideChartStyle
import com.patrykandpatrick.vico.core.axis.AxisPosition
import com.patrykandpatrick.vico.core.axis.formatter.AxisValueFormatter
import com.patrykandpatrick.vico.core.entry.FloatEntry
import com.patrykandpatrick.vico.core.entry.entryModelOf
import com.patrykandpatrick.vico.compose.component.textComponent
import com.powersense.R
import com.powersense.data.HistoryPoint
import com.powersense.screens.components.PieChart
import com.powersense.screens.components.PieChartData
import com.powersense.ui.theme.PowerSenseGreen
import com.powersense.ui.theme.PowerSenseOrange
import com.powersense.ui.theme.PowerSensePurple
import com.powersense.viewmodels.CostTimePeriod
import com.powersense.viewmodels.MetricsViewModel
import com.powersense.viewmodels.ThemeOption
import com.powersense.viewmodels.ThemeViewModel
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MetricsScreen(
    metricsViewModel: MetricsViewModel = viewModel(),
    themeViewModel: ThemeViewModel = viewModel()
) {
    val historyData by metricsViewModel.historyData.collectAsState()
    val isLoading by metricsViewModel.isLoading.collectAsState()
    val error by metricsViewModel.error.collectAsState()
    val currentTheme by themeViewModel.themeState.collectAsState()

    val selectedCostPeriod by metricsViewModel.selectedCostPeriod.collectAsState()
    val costPerKwh by metricsViewModel.costPerKwh.collectAsState()
    val estimatedCost by metricsViewModel.estimatedCost.collectAsState()

    val currentTotalPower = historyData?.instPower?.toFloat() ?: 0f

    val pieChartData = remember(currentTotalPower) {
        if (currentTotalPower > 0) {
            listOf(
                PieChartData("Living Room", currentTotalPower * 0.6f, PowerSensePurple),
                PieChartData("Bedroom", currentTotalPower * 0.3f, PowerSenseGreen),
                PieChartData("Other", currentTotalPower * 0.1f, PowerSenseOrange)
            )
        } else {
            emptyList()
        }
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
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_powersense_logo),
                            contentDescription = "Logo",
                            modifier = Modifier.size(32.dp).clip(RoundedCornerShape(6.dp))
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Metrics Dashboard", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = headerContentColor)
                    }
                },
                actions = { IconButton(onClick = { metricsViewModel.fetchHistory() }) { Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = headerContentColor) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = headerContainerColor, titleContentColor = headerContentColor, navigationIconContentColor = headerContentColor, actionIconContentColor = headerContentColor)
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { metricsViewModel.fetchHistory() }, containerColor = PowerSensePurple, contentColor = Color.White) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh Data")
            }
        },
        floatingActionButtonPosition = FabPosition.End,
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->

        if (isLoading && historyData == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = PowerSensePurple) }
        } else if (error != null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(text = error ?: "Unknown Error", color = Color.Red, modifier = Modifier.padding(16.dp)) }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item { Spacer(modifier = Modifier.height(16.dp)) }

                // Cost Estimator
                item {
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Cost Estimator", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.weight(1f)) {
                                    var expanded by remember { mutableStateOf(false) }
                                    OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)) {
                                        Text(selectedCostPeriod.label, maxLines = 1, fontSize = 12.sp)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp))
                                    }
                                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
                                        CostTimePeriod.entries.forEach { period -> DropdownMenuItem(text = { Text(period.label) }, onClick = { metricsViewModel.setCostPeriod(period); expanded = false }) }
                                    }
                                }
                                OutlinedTextField(value = costPerKwh, onValueChange = { metricsViewModel.setCostPerKwh(it) }, label = { Text("Price/kWh") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(0.8f), textStyle = LocalTextStyle.current.copy(fontSize = 14.sp), enabled = selectedCostPeriod != CostTimePeriod.None)
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Estimated Cost: ", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(text = if (selectedCostPeriod == CostTimePeriod.None) "--" else if (estimatedCost < 0.01 && estimatedCost > 0) "Rs. < 0.01" else "Rs. %.2f".format(estimatedCost), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = if (selectedCostPeriod == CostTimePeriod.None) Color.Gray else PowerSenseGreen)
                            }
                        }
                    }
                }

                // Realtime Pie Chart
                item {
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text("Realtime Breakdown", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Spacer(modifier = Modifier.height(24.dp))
                            if (pieChartData.isNotEmpty()) PieChart(data = pieChartData) else Box(modifier = Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) { Text("No Power Usage Detected", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        }
                    }
                }

                // Charts
                item { MetricsLineChart("Current (A) History", historyData?.currentHistory ?: emptyList(), Color(0xFF2196F3), chartLabelColor) }
                item { MetricsLineChart("Avg Current (A) History", historyData?.avgCurrentHistory ?: emptyList(), Color(0xFF7E57C2), chartLabelColor) }
                item { MetricsLineChart("Voltage (V) History", historyData?.voltageHistory ?: emptyList(), Color(0xFFF44336), chartLabelColor) }
                item { MetricsLineChart("Power (W) History", historyData?.powerHistory ?: emptyList(), PowerSenseGreen, chartLabelColor) }

                item { Spacer(modifier = Modifier.height(20.dp)) }
            }
        }
    }
}

@Composable
fun MetricsLineChart(title: String, dataPoints: List<HistoryPoint>, lineColor: Color, labelColor: Color) {
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

                val scrollState = rememberChartScrollState()
                LaunchedEffect(dataPoints.size) {
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
                                SimpleDateFormat("hh:mm a", Locale.getDefault()).format(date ?: java.util.Date())
                            } catch (e: Exception) { "" }
                        } else { "" }
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