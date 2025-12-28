package com.powersense.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.powersense.data.HistoricalSensorData
import com.powersense.data.PowerSenseClient
import com.powersense.data.RelayUsage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

enum class CostTimePeriod(val label: String, val hours: Double) {
    None("Select Period", 0.0),
    OneMinute("Last 1 Minute", 1.0 / 60.0),
    FiveMinutes("Last 5 Minutes", 5.0 / 60.0),
    TenMinutes("Last 10 Minutes", 10.0 / 60.0),
    ThirtyMinutes("Last 30 Minutes", 0.5),
    OneHour("Last 1 Hour", 1.0),
    SixHours("Last 6 Hours", 6.0),
    TwelveHours("Last 12 Hours", 12.0),
    TwentyFourHours("Last 24 Hours", 24.0)
}

class MetricsViewModel : ViewModel() {

    private val _historyData = MutableStateFlow<HistoricalSensorData?>(null)
    val historyData: StateFlow<HistoricalSensorData?> = _historyData.asStateFlow()

    private val _relayUsage = MutableStateFlow<RelayUsage?>(null)
    val relayUsage: StateFlow<RelayUsage?> = _relayUsage.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    private val _selectedCostPeriod = MutableStateFlow(CostTimePeriod.None)
    val selectedCostPeriod = _selectedCostPeriod.asStateFlow()

    private val _costPerKwh = MutableStateFlow("10.0")
    val costPerKwh = _costPerKwh.asStateFlow()

    private val _estimatedCost = MutableStateFlow(0.0)
    val estimatedCost = _estimatedCost.asStateFlow()

    private var pollingJob: Job? = null

    init {
        fetchHistory()
    }

    fun setCostPeriod(period: CostTimePeriod) {
        _selectedCostPeriod.value = period
        // Restart polling to immediately fetch the correct amount of data
        stopPolling()
        if (period != CostTimePeriod.None) {
            startPolling()
        } else {
            _estimatedCost.value = 0.0
        }
    }

    fun setCostPerKwh(price: String) {
        _costPerKwh.value = price
        calculateEstimatedCost()
    }

    fun fetchHistory() {
        startPolling()
    }

    private fun startPolling() {
        if (pollingJob?.isActive == true) return

        pollingJob = viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            while (true) {
                val period = _selectedCostPeriod.value

                // 1. Fetch Buffer Logic
                // If the user wants "Last 1 Hour", we fetch "2 Hours" from backend.
                // This ensures we have data points covering the very start of the window.
                val hoursNeeded = if (period == CostTimePeriod.None) 24.0 else period.hours
                val hoursToFetch = max(1, ceil(hoursNeeded + 1.5).toInt())

                val result = PowerSenseClient.getHistoricalData(hoursToFetch)

                result.onSuccess { data ->
                    _historyData.value = data
                    _error.value = null
                    calculateEstimatedCost()
                }.onFailure { e ->
                    if (_historyData.value == null) {
                        _error.value = "Failed to load charts: ${e.message}"
                    }
                }

                // 2. Fetch Pie Chart Data (Always 24h)
                val pieResult = PowerSenseClient.getRelayUsage(24)
                pieResult.onSuccess {
                    _relayUsage.value = it
                }

                _isLoading.value = false
                delay(15000)
            }
        }
    }

    private fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }

    private fun calculateEstimatedCost() {
        viewModelScope.launch(Dispatchers.Default) {
            val data = _historyData.value ?: return@launch
            val price = _costPerKwh.value.toDoubleOrNull() ?: 0.0
            val period = _selectedCostPeriod.value

            if (period == CostTimePeriod.None || price <= 0.0) {
                _estimatedCost.value = 0.0
                return@launch
            }

            try {
                // 1. Parse Dates ONCE
                val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
                format.timeZone = TimeZone.getTimeZone("UTC")

                // 2. Convert all valid points to (Time, Power)
                val allPoints = data.powerHistory.mapNotNull { point ->
                    try {
                        val time = format.parse(point.timestamp)?.time ?: return@mapNotNull null
                        time to point.value
                    } catch (e: Exception) { null }
                }.sortedBy { it.first }

                if (allPoints.isEmpty()) {
                    _estimatedCost.value = 0.0
                    return@launch
                }

                // 3. ANCHOR LOGIC (Fixes 1min/5min issues)
                // Instead of using Phone Time (System.currentTimeMillis), use the
                // Latest Data Point Time. This syncs calculation to the server's reality.
                val latestDataTime = allPoints.last().first
                val targetDurationMillis = (period.hours * 3600 * 1000).toLong()
                val cutoffTime = latestDataTime - targetDurationMillis

                // 4. Filter for window
                val relevantPoints = allPoints.filter { it.first >= cutoffTime }

                if (relevantPoints.size < 2) {
                    // Not enough data points to integrate, use Simple Average as fallback
                    if (relevantPoints.isNotEmpty()) {
                        val avgW = relevantPoints.first().second
                        _estimatedCost.value = (avgW / 1000.0) * period.hours * price
                    } else {
                        _estimatedCost.value = 0.0
                    }
                    return@launch
                }

                // 5. Riemann Sum (Trapezoidal Rule) for Accuracy
                var totalWattSeconds = 0.0
                for (i in 0 until relevantPoints.size - 1) {
                    val (t1, p1) = relevantPoints[i]
                    val (t2, p2) = relevantPoints[i + 1]

                    val timeDiffSec = (t2 - t1) / 1000.0
                    // Filter large gaps (e.g., sensor offline for > 5 mins)
                    if (timeDiffSec > 0 && timeDiffSec < 300) {
                        val avgPower = (p1 + p2) / 2.0
                        totalWattSeconds += avgPower * timeDiffSec
                    }
                }

                val totalKwh = totalWattSeconds / 3_600_000.0
                _estimatedCost.value = totalKwh * price

            } catch (e: Exception) {
                e.printStackTrace()
                _estimatedCost.value = 0.0
            }
        }
    }

    override fun onCleared() {
        stopPolling()
        super.onCleared()
    }
}