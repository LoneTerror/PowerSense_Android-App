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

// Define an Enum for our theme options
enum class ThemeOption(val value: String) {
    Light("Light"),
    Dark("Dark"),
    System("System")
}

// Create the DataStore instance
private val Application.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class ThemeViewModel(application: Application) : AndroidViewModel(application) {

    private val dataStore = application.dataStore

    // Define the key for saving the theme
    companion object {
        private val THEME_KEY = stringPreferencesKey("theme_option")
    }

    // Read the theme from DataStore and expose it as a StateFlow
    // This will emit the new value whenever it changes
    val themeState = dataStore.data
        .map { preferences ->
            // Get the string value from DataStore, default to "System"
            val themeString = preferences[THEME_KEY] ?: ThemeOption.System.value
            // Convert the string to our ThemeOption enum
            ThemeOption.entries.firstOrNull { it.value == themeString } ?: ThemeOption.System
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ThemeOption.System // Default value while loading
        )

    // Function to set (write) the new theme
    fun setTheme(theme: ThemeOption) {
        viewModelScope.launch {
            dataStore.edit { preferences ->
                preferences[THEME_KEY] = theme.value
            }
        }
    }
}