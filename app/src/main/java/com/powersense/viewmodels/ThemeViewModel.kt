package com.powersense.viewmodels
import android.app.Application
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class ThemeOption(val value: String) {
    Light("Light"),
    Dark("Dark"),
    System("System")
}

// Enum for Consumption Summary Interval
enum class SummaryInterval(val hours: Int, val label: String) {
    SixHours(6, "Every 6 Hours"),
    TwelveHours(12, "Every 12 Hours"),
    TwentyFourHours(24, "Every 24 Hours")
}

private val Application.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class ThemeViewModel(application: Application) : AndroidViewModel(application) {

    private val dataStore = application.dataStore

    companion object {
        private val THEME_KEY = stringPreferencesKey("theme_option")
        private val SUMMARY_INTERVAL_KEY = stringPreferencesKey("summary_interval") // New Key
        private val SUMMARY_ENABLED_KEY = stringPreferencesKey("summary_enabled") // New Key
    }

    val themeState = dataStore.data
        .map { preferences ->
            val themeString = preferences[THEME_KEY] ?: ThemeOption.System.value
            ThemeOption.entries.firstOrNull { it.value == themeString } ?: ThemeOption.System
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ThemeOption.System
        )

    // New State for Summary Interval
    val summaryIntervalState = dataStore.data
        .map { preferences ->
            val intervalString = preferences[SUMMARY_INTERVAL_KEY] ?: SummaryInterval.TwentyFourHours.name
            SummaryInterval.entries.firstOrNull { it.name == intervalString } ?: SummaryInterval.TwentyFourHours
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SummaryInterval.TwentyFourHours
        )

    // New State for Summary Enabled Toggle
    val isSummaryEnabled = dataStore.data
        .map { preferences ->
            preferences[SUMMARY_ENABLED_KEY]?.toBooleanStrictOrNull() ?: true // Default true
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true
        )

    fun setTheme(theme: ThemeOption) {
        viewModelScope.launch {
            dataStore.edit { preferences ->
                preferences[THEME_KEY] = theme.value
            }
        }
    }

    // New setters
    fun setSummaryInterval(interval: SummaryInterval) {
        viewModelScope.launch {
            dataStore.edit { preferences ->
                preferences[SUMMARY_INTERVAL_KEY] = interval.name
            }
        }
    }

    fun setSummaryEnabled(enabled: Boolean) {
        viewModelScope.launch {
            dataStore.edit { preferences ->
                preferences[SUMMARY_ENABLED_KEY] = enabled.toString()
            }
        }
    }
}