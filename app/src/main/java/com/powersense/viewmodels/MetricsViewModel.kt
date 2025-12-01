package com.powersense.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.powersense.data.HistoricalSensorData
import com.powersense.data.PowerSenseClient
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

enum class CostTimePeriod(val label: String, val hours: Double) {
    None("None", 0.0), // New Option
    OneMinute("Last 1 Minute", 1.0 / 60.0),
    FiveMinutes("Last 5 Minutes", 5.0 / 60.0),
    ThirtyMinutes("Last 30 Minutes", 0.5),
    OneHour("Last 1 Hour", 1.0),
    SixHours("Last 6 Hours", 6.0),
    TwelveHours("Last 12 Hours", 12.0),
    TwentyFourHours("Last 24 Hours", 24.0)
}

class MetricsViewModel : ViewModel() {

    private val _historyData = MutableStateFlow<HistoricalSensorData?>(null)
    val historyData: StateFlow<HistoricalSensorData?> = _historyData.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    private val _selectedCostPeriod = MutableStateFlow(CostTimePeriod.None) // Default to None
    val selectedCostPeriod = _selectedCostPeriod.asStateFlow()

    private val _costPerKwh = MutableStateFlow("10.0")
    val costPerKwh = _costPerKwh.asStateFlow()

    private val _estimatedCost = MutableStateFlow(0.0)
    val estimatedCost = _estimatedCost.asStateFlow()

    private var pollingJob: Job? = null

    init {
        // Don't fetch immediately if default is None.
        // Wait for user selection or manual refresh.
    }

    fun setCostPeriod(period: CostTimePeriod) {
        _selectedCostPeriod.value = period
        if (period == CostTimePeriod.None) {
            stopPolling()
            _estimatedCost.value = 0.0
        } else {
            // If we have data already, recalculate immediately for instant feedback
            if (_historyData.value != null) {
                calculateEstimatedCost()
            }
            // Ensure we are polling to keep data fresh
            startPolling()
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
                val result = PowerSenseClient.getHistoricalData(24)

                result.onSuccess { data ->
                    _historyData.value = data
                    _error.value = null
                    calculateEstimatedCost() // Recalculate whenever new data arrives
                }.onFailure { e ->
                    if (_historyData.value == null) {
                        _error.value = "Failed to load charts: ${e.message}"
                    }
                }

                _isLoading.value = false
                delay(30000)
            }
        }
    }

    private fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }

    private fun calculateEstimatedCost() {
        val data = _historyData.value ?: return
        val price = _costPerKwh.value.toDoubleOrNull() ?: 0.0
        val period = _selectedCostPeriod.value

        if (period == CostTimePeriod.None) {
            _estimatedCost.value = 0.0
            return
        }

        val now = System.currentTimeMillis()
        // Calculate start time for the window
        val cutoffTime = now - (period.hours * 60 * 60 * 1000).toLong()

        // 1. Filter points that actually exist in this window
        val relevantPoints = data.powerHistory.filter { point ->
            try {
                val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
                format.timeZone = TimeZone.getTimeZone("UTC")
                val time = format.parse(point.timestamp)?.time ?: 0L
                time >= cutoffTime
            } catch (e: Exception) {
                false
            }
        }

        // 2. CRITICAL CHECK: If no data points found in this window, cost is 0.
        if (relevantPoints.isNotEmpty()) {
            // Calculate average power from the available points
            val avgPowerW = relevantPoints.map { it.value }.average()
            val avgPowerkW = avgPowerW / 1000.0

            // Estimate energy based on the full duration of the selected period
            val energykWh = avgPowerkW * period.hours
            _estimatedCost.value = energykWh * price
        } else {
            _estimatedCost.value = 0.0
        }
    }

    override fun onCleared() {
        stopPolling()
        super.onCleared()
    }
}