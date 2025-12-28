package com.powersense.viewmodels

import android.annotation.SuppressLint
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.location.Location
import com.google.android.gms.location.LocationServices
import com.powersense.data.HistoricalSensorData
import com.powersense.data.PowerSenseClient
import com.powersense.data.RelayDevice
import com.powersense.data.RelayUsage
import com.powersense.data.SensorData
import com.powersense.data.WeatherData
import com.powersense.data.WeatherRepository
import com.powersense.utils.NotificationUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {

    private val _sensorData = MutableStateFlow<SensorData?>(null)
    val sensorData: StateFlow<SensorData?> = _sensorData.asStateFlow()

    private val _homeChartData = MutableStateFlow<HistoricalSensorData?>(null)
    val homeChartData: StateFlow<HistoricalSensorData?> = _homeChartData.asStateFlow()

    // Accurate Relay Usage for Pie Chart
    private val _relayUsage = MutableStateFlow<RelayUsage?>(null)
    val relayUsage: StateFlow<RelayUsage?> = _relayUsage.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _connectionStatus = MutableStateFlow("Connecting...")
    val connectionStatus = _connectionStatus.asStateFlow()

    private val _connectionStartTime = MutableStateFlow<Long?>(null)
    val connectionStartTime = _connectionStartTime.asStateFlow()

    // Weather & Search
    private val _weatherState = MutableStateFlow<WeatherData?>(null)
    val weatherState: StateFlow<WeatherData?> = _weatherState.asStateFlow()

    private val _searchError = MutableStateFlow<String?>(null)
    val searchError: StateFlow<String?> = _searchError.asStateFlow()

    private val _citySuggestions = MutableStateFlow<List<String>>(emptyList())
    val citySuggestions: StateFlow<List<String>> = _citySuggestions.asStateFlow()

    private var pollCounter = 0
    private var currentIntervalHours = 24

    init {
        startPolling()
    }

    private fun startPolling() {
        viewModelScope.launch {
            _isLoading.value = true
            while (true) {
                // 1. Fetch Real-time Readings (Every 3s)
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

                // 2. Fetch Charts & Pie Data (Every ~9-10s)
                if (pollCounter % 3 == 0) {
                    fetchHomeChartData(currentIntervalHours)
                    fetchRelayUsage(currentIntervalHours)
                }

                pollCounter++
                if (_isLoading.value) _isLoading.value = false
                delay(3000)
            }
        }
    }

    fun fetchHomeChartData(intervalHours: Int) {
        currentIntervalHours = intervalHours
        viewModelScope.launch {
            val result = PowerSenseClient.getHistoricalData(intervalHours)
            result.onSuccess { data ->
                _homeChartData.value = data
            }
        }
    }

    private fun fetchRelayUsage(intervalHours: Int) {
        viewModelScope.launch {
            val result = PowerSenseClient.getRelayUsage(intervalHours)
            result.onSuccess { data ->
                _relayUsage.value = data
            }
        }
    }

    // --- Weather & Search ---
    @SuppressLint("MissingPermission")
    fun fetchLocationAndWeather(context: Context) {
        viewModelScope.launch {
            try {
                val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
                fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                    if (location != null) {
                        fetchWeather(context, location.latitude, location.longitude)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun updateLocation(context: Context, cityName: String) {
        viewModelScope.launch {
            _searchError.value = null
            val coords = WeatherRepository.getCoordinates(context, cityName)
            if (coords != null) {
                fetchWeather(context, coords.first, coords.second)
            } else {
                _searchError.value = "Location not found"
            }
        }
    }

    fun searchCities(context: Context, query: String) {
        viewModelScope.launch {
            if (query.length >= 3) {
                val results = WeatherRepository.searchCityNames(context, query)
                _citySuggestions.value = results
            } else {
                _citySuggestions.value = emptyList()
            }
        }
    }

    fun clearSuggestions() {
        _citySuggestions.value = emptyList()
    }

    fun clearSearchError() {
        _searchError.value = null
    }

    private fun fetchWeather(context: Context, lat: Double, lon: Double) {
        viewModelScope.launch {
            val data = WeatherRepository.getCurrentWeather(context, lat, lon)
            if (data != null) {
                _weatherState.value = data
            }
        }
    }

    // --- Alerts ---
    fun checkForAbnormalUsage(context: Context, isMaxEnabled: Boolean, isApplianceEnabled: Boolean, activeRelays: List<RelayDevice>) {
        val data = _sensorData.value ?: return
        val currentAmps = data.current
        val currentPowerW = data.power

        if (isMaxEnabled && currentAmps > 30.0) {
            val timestamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
            NotificationUtils.sendAlertNotification(context, "CRITICAL: Sensor Overload!", "Current ${"%.2f".format(currentAmps)}A > 30A! at $timestamp", 1001)
        }

        if (isApplianceEnabled) {
            activeRelays.filter { it.isOn && it.threshold != null }.forEach { device ->
                val limit = device.threshold!!
                val isWatts = device.thresholdUnit == "W"
                val limitInWatts: Double = if (isWatts) limit else limit * data.voltage
                val thresholdWithBuffer: Double = limitInWatts * 1.05

                if (currentPowerW > thresholdWithBuffer) {
                    val timestamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
                    NotificationUtils.sendAlertNotification(context, "Abnormal: ${device.name}", "Usage > ${"%.2f".format(limit)}${device.thresholdUnit}. at $timestamp", device.id.hashCode())
                }
            }
        }
    }
}