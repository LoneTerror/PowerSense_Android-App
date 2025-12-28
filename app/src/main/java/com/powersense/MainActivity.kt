package com.powersense

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
//import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.powersense.navigation.AppNavigation
import com.powersense.ui.theme.PowerSenseTheme
import com.powersense.viewmodels.ThemeViewModel // <-- IMPORT

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //enableEdgeToEdge()
        setContent {
            // Get the ViewModel and collect the theme state
            val themeViewModel: ThemeViewModel = viewModel()
            val themeState by themeViewModel.themeState.collectAsState()

            // Pass the collected state to your theme
            PowerSenseTheme(
                themeOption = themeState
            ) {
                AppNavigation()
            }
        }
    }
}