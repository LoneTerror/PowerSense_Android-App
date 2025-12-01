package com.powersense.screens.components

import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.powersense.ui.theme.PowerSenseGreen
import com.powersense.ui.theme.PowerSensePurple
import com.powersense.ui.theme.PowerSenseOrange

data class PieChartData(
    val label: String,
    val value: Float,
    val color: Color
)

@Composable
fun PieChart(
    data: List<PieChartData>,
    radiusOuter: Dp = 90.dp,
    chartBarWidth: Dp = 20.dp,
    animDuration: Int = 1000
) {
    val totalSum = data.sumOf { it.value.toDouble() }.toFloat()
    val floatValue = mutableListOf<Float>()

    // Calculate angles
    data.forEachIndexed { index, pieChartData ->
        floatValue.add(index, 360 * pieChartData.value / totalSum)
    }

    // Animation
    var animationPlayed by remember { mutableStateOf(false) }
    val lastValue = 0f

    // Animate the sweep angle
    val animateSize by animateFloatAsState(
        targetValue = if (animationPlayed) 360f else 0f,
        animationSpec = tween(
            durationMillis = animDuration,
            delayMillis = 0,
            easing = LinearOutSlowInEasing
        ), label = "PieAnimation"
    )

    LaunchedEffect(key1 = true) {
        animationPlayed = true
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // --- CHART ---
        Box(
            modifier = Modifier.size(radiusOuter * 2),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .size(radiusOuter * 2)
                    .rotate(-90f)
            ) {
                var startAngle = 0f

                floatValue.forEachIndexed { index, sweepAngle ->
                    drawArc(
                        color = data[index].color,
                        startAngle = startAngle,
                        sweepAngle = sweepAngle * (animateSize / 360), // Animate sweep
                        useCenter = false,
                        style = Stroke(width = chartBarWidth.toPx()),
                        size = Size(
                            width = (radiusOuter * 2 - chartBarWidth).toPx(),
                            height = (radiusOuter * 2 - chartBarWidth).toPx()
                        ),
                        topLeft = Offset(
                            x = chartBarWidth.toPx() / 2,
                            y = chartBarWidth.toPx() / 2
                        )
                    )
                    startAngle += sweepAngle
                }
            }

            // Center Text
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Total",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${totalSum.toInt()} W",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- LEGEND ---
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            data.forEach { item ->
                PieChartLegendItem(data = item, total = totalSum)
            }
        }
    }
}

@Composable
fun PieChartLegendItem(data: PieChartData, total: Float) {
    val percentage = (data.value / total * 100).toInt()

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(data.color, shape = RoundedCornerShape(4.dp))
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = data.label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Text(
            text = "${data.value.toInt()} W ($percentage%)",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}