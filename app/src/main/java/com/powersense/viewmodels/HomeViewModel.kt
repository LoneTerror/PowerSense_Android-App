package com.powersense.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.powersense.data.HistoricalSensorData
import com.powersense.data.PowerSenseClient
import com.powersense.data.SensorData
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {

    private val _sensorData = MutableStateFlow<SensorData?>(null)
    val sensorData: StateFlow<SensorData?> = _sensorData.asStateFlow()

    // NEW: State for historical data used in the Home Screen Chart
    private val _homeChartData = MutableStateFlow<HistoricalSensorData?>(null)
    val homeChartData: StateFlow<HistoricalSensorData?> = _homeChartData.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _connectionStatus = MutableStateFlow("Connecting...")
    val connectionStatus = _connectionStatus.asStateFlow()

    private val _connectionStartTime = MutableStateFlow<Long?>(null)
    val connectionStartTime = _connectionStartTime.asStateFlow()

    init {
        startPolling()
    }

    private fun startPolling() {
        viewModelScope.launch {
            _isLoading.value = true
            while (true) {
                // 1. Fetch Real-time data
                val result = PowerSenseClient.getLatestReadings()

                result.onSuccess { data ->
                    _sensorData.value = data
                    if (_connectionStatus.value != "Connected") {
                        _connectionStatus.value = "Connected"
                        if (_connectionStartTime.value == null) {
                            _connectionStartTime.value = System.currentTimeMillis()
                        }
                    }
                }.onFailure {
                    _connectionStatus.value = "Offline"
                    _connectionStartTime.value = null
                }

                if (_isLoading.value) _isLoading.value = false

                delay(3000)
            }
        }
    }

    // NEW: Fetch history based on interval (called when screen loads or settings change)
    fun fetchHomeChartData(intervalHours: Int) {
        viewModelScope.launch {
            // Re-using the getHistoricalData from PowerSenseClient
            val result = PowerSenseClient.getHistoricalData(intervalHours)
            result.onSuccess { data ->
                _homeChartData.value = data
            }
        }
    }
}