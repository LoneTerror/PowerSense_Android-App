package com.powersense.screens

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.powersense.ui.theme.CozyHeaderBackground

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacySettingsScreen(
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Privacy Policy",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.background else CozyHeaderBackground
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "Last Updated: Dec 1, 2025",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            PrivacySection(
                title = "1. Introduction",
                content = "Welcome to PowerSense. We value your privacy and are committed to protecting your personal data. This privacy policy explains how we collect, use, and safeguard your information when you use our smart energy monitoring application."
            )

            PrivacySection(
                title = "2. Data We Collect",
                content = "We collect the following types of information:\n\n" +
                        "• Account Information: Name, email address, and profile picture provided during sign-up.\n" +
                        "• Device Data: Status and power consumption metrics of connected appliances (e.g., Voltage, Current, Power).\n" +
                        "• Usage Logs: Timestamps of when devices are toggled on or off."
            )

            PrivacySection(
                title = "3. How We Use Your Data",
                content = "Your data is used to:\n\n" +
                        "• Provide real-time monitoring and control of your home appliances.\n" +
                        "• Calculate energy costs and generate usage history charts.\n" +
                        "• Sync device states across your connected devices."
            )

            PrivacySection(
                title = "4. Data Security",
                content = "We implement security measures to ensure your data is safe. All communication between the app and our backend servers is encrypted using SSL/TLS protocols."
            )

            PrivacySection(
                title = "5. Third-Party Services",
                content = "We use Firebase for authentication and database services to ensure reliable and secure data storage. We do not compromise or sell your personal data to third parties."
            )

            PrivacySection(
                title = "6. Contact Us",
                content = "If you have any questions about this Privacy Policy, please contact us at nothinghere21@gmail.com"
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun PrivacySection(title: String, content: String) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = content,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 22.sp
        )
    }
}