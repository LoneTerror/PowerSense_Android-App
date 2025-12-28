package com.powersense.data

import android.content.Context
import android.location.Geocoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

data class WeatherData(
    val temperature: Double,
    val locationName: String
)

object WeatherRepository {

    suspend fun getCurrentWeather(context: Context, lat: Double, lon: Double): WeatherData? {
        return withContext(Dispatchers.IO) {
            try {
                // 1. Get City Name
                val geocoder = Geocoder(context, Locale.getDefault())
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(lat, lon, 1)
                val city = addresses?.firstOrNull()?.locality ?: "Unknown Location"

                // 2. Get Weather Data
                val urlString = "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&current_weather=true"
                val url = URL(urlString)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"

                val jsonStr = connection.inputStream.bufferedReader().use { it.readText() }
                val jsonObj = JSONObject(jsonStr)
                val current = jsonObj.getJSONObject("current_weather")
                val temp = current.getDouble("temperature")

                WeatherData(temperature = temp, locationName = city)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    // --- MISSING FUNCTION 1: Get Lat/Lon from City Name ---
    suspend fun getCoordinates(context: Context, cityName: String): Pair<Double, Double>? {
        return withContext(Dispatchers.IO) {
            try {
                val geocoder = Geocoder(context, Locale.getDefault())
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocationName(cityName, 1)
                if (!addresses.isNullOrEmpty()) {
                    val address = addresses[0]
                    Pair(address.latitude, address.longitude)
                } else {
                    null
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    // --- MISSING FUNCTION 2: Search for City Suggestions ---
    suspend fun searchCityNames(context: Context, query: String): List<String> {
        return withContext(Dispatchers.IO) {
            try {
                val geocoder = Geocoder(context, Locale.getDefault())
                @Suppress("DEPRECATION")
                // Fetch up to 5 results
                val addresses = geocoder.getFromLocationName(query, 5)

                addresses?.mapNotNull { address ->
                    val city = address.locality ?: address.featureName
                    val country = address.countryName
                    val admin = address.adminArea // State or Province

                    if (city != null && country != null) {
                        if (admin != null) "$city, $admin, $country" else "$city, $country"
                    } else {
                        city ?: address.featureName
                    }
                }?.distinct() ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
        }
    }
}