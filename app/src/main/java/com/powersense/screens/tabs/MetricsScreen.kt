package com.powersense.screens.tabs

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.powersense.R
import com.powersense.ui.theme.PowerSensePurple
import com.powersense.ui.theme.PowerSenseTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MetricsScreen() {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_powersense_logo),
                            contentDescription = "PowerSense Logo",
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(6.dp))
                        )
                        Text(
                            " PowerSense",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground // <-- THEME AWARE
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { /* TODO: Show filter dialog */ }) {
                        Icon(
                            Icons.Default.FilterAlt,
                            contentDescription = "Filter",
                            tint = MaterialTheme.colorScheme.onBackground // <-- THEME AWARE
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background // <-- THEME AWARE
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background // <-- THEME AWARE
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                TimeRangeSelector()
            }
            item {
                ChartCard(title = "Total Consumption (kWh)", chartType = "Line Chart")
            }
            item {
                ChartCard(title = "Consumption by Device", chartType = "Bar Chart")
            }
            item {
                ChartCard(title = "Consumption Breakdown", chartType = "Donut Chart")
            }
            item {
                ChartCard(title = "Daily Average Consumption", chartType = "Line Chart")
            }
            item {
                Spacer(modifier = Modifier.height(16.dp)) // Padding at the bottom
            }
        }
    }
}

@Composable
fun TimeRangeSelector() {
    var selectedRange by remember { mutableStateOf("Last 30 mins") }
    val ranges = listOf("Today", "Last 30 mins", "Past Hour", "Custom")

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ranges.take(3).forEach { range ->
                TimeButton(
                    text = range,
                    isSelected = selectedRange == range,
                    onClick = { selectedRange = range },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        TimeButton(
            text = ranges.last(),
            isSelected = selectedRange == ranges.last(),
            onClick = { selectedRange = ranges.last() },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun TimeButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = if (isSelected) {
        ButtonDefaults.buttonColors(containerColor = PowerSensePurple)
    } else {
        ButtonDefaults.outlinedButtonColors(
            containerColor = MaterialTheme.colorScheme.surface, // <-- THEME AWARE
            contentColor = MaterialTheme.colorScheme.onSurface // <-- THEME AWARE
        )
    }

    val border = if (!isSelected) ButtonDefaults.outlinedButtonBorder else null

    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(40.dp),
        shape = RoundedCornerShape(8.dp),
        colors = colors,
        border = border
    ) {
        Text(text)
    }
}

/**
 * A reusable placeholder card for each chart on the dashboard.
 */
@Composable
fun ChartCard(title: String, chartType: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), // <-- THEME AWARE
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                OutlinedButton(
                    onClick = { /* TODO: Navigate to details */ },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        width = 1.dp,
                        brush = SolidColor(MaterialTheme.colorScheme.outlineVariant)
                    )
                ) {
                    Text("Details")
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            // --- Chart Placeholder ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "[$chartType Placeholder]",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MetricsScreenPreview() {
    PowerSenseTheme {
        MetricsScreen()
    }
}