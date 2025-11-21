package com.powersense.screens.tabs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.powersense.ui.theme.PowerSenseGreen
import com.powersense.ui.theme.PowerSensePurple
import com.powersense.ui.theme.PowerSenseTheme

// We'll create a data class to hold the state for each switch
data class VirtualSwitch(
    val id: Int,
    val name: String,
    val description: String,
    val isOn: Boolean
)

@Composable
fun SimulationScreen() {
    // This is a "remember" block. It holds the state of our switches.
    // In a real app, this list would come from your backend!
    var switches by remember {
        mutableStateOf(
            listOf(
                VirtualSwitch(1, "Living Room Lights", "Controls the main overhead lights.", true),
                VirtualSwitch(2, "Bedroom Fan", "Manages the ceiling fan.", false),
                VirtualSwitch(3, "Kitchen Coffee Maker", "Automates the morning brew.", true)
            )
        )
    }

    Scaffold(
        // This puts the "Add Switch" button at the bottom, just like your wireframe
        floatingActionButton = {
            Button(
                onClick = { /* TODO: Show dialog to add a new switch */ },
                colors = ButtonDefaults.buttonColors(containerColor = PowerSensePurple),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Text(text = "+ Add Switch", fontSize = 16.sp)
            }
        },
        floatingActionButtonPosition = FabPosition.Center, // <-- Center the button
        containerColor = MaterialTheme.colorScheme.background // <-- THEME AWARE
    ) { innerPadding ->

        // LazyColumn is an efficient, scrollable list.
        // It only renders the items that are visible on screen.
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp), // Add padding around the list
            verticalArrangement = Arrangement.spacedBy(12.dp) // Space between items
        ) {
            // This is the title at the top
            item {
                Text(
                    text = "Simulation",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground, // <-- THEME AWARE
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            // This loops over our 'switches' list and creates a SwitchCard for each one
            items(switches) { virtualSwitch ->
                SwitchCard(
                    item = virtualSwitch,
                    onToggle = {
                        // This code runs when a switch is clicked.
                        // It finds the switch that was clicked and flips its 'isOn' state.
                        switches = switches.map {
                            if (it.id == virtualSwitch.id) it.copy(isOn = !it.isOn) else it
                        }
                        // TODO: In the future, this would also make an API call to your backend!
                    },
                    onEdit = { /* TODO: Handle edit */ },
                    onDelete = { /* TODO: Handle delete */ }
                )
            }

            // Add a spacer at the bottom to make room for the "Add Switch" button
            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

/**
 * This is a custom Composable for a single switch item in the list.
 * This makes our code clean and reusable.
 */
@Composable
fun SwitchCard(
    item: VirtualSwitch,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), // <-- THEME AWARE
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Title (e.g., "Living Room Lights")
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface // <-- THEME AWARE
                )
                // The Switch toggle
                Switch(
                    checked = item.isOn,
                    onCheckedChange = { onToggle() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = PowerSenseGreen,
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = Color.Gray
                    )
                )
            }
            Spacer(modifier = Modifier.height(4.dp))

            // ON / OFF Text
            Text(
                text = if (item.isOn) "ON" else "OFF",
                color = if (item.isOn) PowerSenseGreen else Color.Gray,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Description text
            Text(
                text = item.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant // <-- THEME AWARE
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Edit / Delete Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onEdit) {
                    Icon(
                        Icons.Outlined.Edit,
                        contentDescription = "Edit",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant, // <-- THEME AWARE
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = " Edit",
                        color = MaterialTheme.colorScheme.onSurfaceVariant, // <-- THEME AWARE
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
                TextButton(onClick = onDelete) {
                    Icon(
                        Icons.Outlined.Delete,
                        contentDescription = "Delete",
                        tint = Color.Red,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(text = " Delete", color = Color.Red, modifier = Modifier.padding(start = 4.dp))
                }
            }
        }
    }
}

// Preview so you can see your screen in Android Studio's 'Split' view
@Preview(showBackground = true)
@Composable
fun SimulationScreenPreview() {
    PowerSenseTheme {
        SimulationScreen()
    }
}