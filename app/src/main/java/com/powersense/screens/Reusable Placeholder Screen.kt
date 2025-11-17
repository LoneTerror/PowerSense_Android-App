package com.powersense.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.powersense.ui.theme.PowerSenseTheme

/**
 * A simple placeholder screen to use for our tabs
 * until we build the real UI.
 */
@Composable
fun PlaceholderScreen(name: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(text = "$name Screen")
    }
}

@Preview
@Composable
fun PlaceholderPreview() {
    PowerSenseTheme {
        PlaceholderScreen(name = "Preview")
    }
}