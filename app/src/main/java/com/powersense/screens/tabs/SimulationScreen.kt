package com.powersense.screens.tabs

import androidx.compose.foundation.BorderStroke // Import
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.powersense.R
import com.powersense.data.RelayDevice
import com.powersense.ui.theme.PowerSenseGreen
import com.powersense.ui.theme.PowerSensePurple
import com.powersense.viewmodels.RelayViewModel
import com.powersense.viewmodels.ThemeOption
import com.powersense.viewmodels.ThemeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimulationScreen(
    relayViewModel: RelayViewModel = viewModel(),
    themeViewModel: ThemeViewModel = viewModel()
) {
    val switches by relayViewModel.relays.collectAsState()
    val isLoading by relayViewModel.isLoading.collectAsState()
    val currentTheme by themeViewModel.themeState.collectAsState()

    var showDialog by remember { mutableStateOf(false) }
    var editingDevice by remember { mutableStateOf<RelayDevice?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var deviceToDelete by remember { mutableStateOf<RelayDevice?>(null) }

    // --- HEADER COLOR LOGIC ---
    val systemDark = isSystemInDarkTheme()
    val isAppInDarkTheme = when (currentTheme) {
        ThemeOption.Light -> false
        ThemeOption.Dark -> true
        ThemeOption.System -> systemDark
    }

    val aestheticDarkHeader = Color(0xFF4E463F)
    val headerContainerColor = if (isAppInDarkTheme) MaterialTheme.colorScheme.background else aestheticDarkHeader
    val headerContentColor = Color.White

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_powersense_logo),
                            contentDescription = "Logo",
                            modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp))
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Switch Configuration",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = headerContentColor
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = headerContainerColor,
                    titleContentColor = headerContentColor,
                    navigationIconContentColor = headerContentColor,
                    actionIconContentColor = headerContentColor
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingDevice = null
                    showDialog = true
                },
                containerColor = PowerSensePurple,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Switch")
            }
        },
        floatingActionButtonPosition = FabPosition.End,
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(switches) { device ->
                SwitchCard(
                    item = device,
                    onToggle = { relayViewModel.toggleRelay(device) },
                    onFavoriteToggle = { relayViewModel.toggleFavorite(device) },
                    onEdit = {
                        editingDevice = device
                        showDialog = true
                    },
                    onDelete = {
                        deviceToDelete = device
                        showDeleteDialog = true
                    }
                )
            }

            if (switches.isEmpty() && !isLoading) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                        Text("No switches found. Add one!", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }

        if (showDialog) {
            SwitchConfigDialog(
                deviceToEdit = editingDevice,
                onDismiss = { showDialog = false },
                onSave = { name, desc, url ->
                    if (editingDevice == null) {
                        relayViewModel.addRelay(name, desc, url)
                    } else {
                        relayViewModel.updateRelay(editingDevice!!.id, name, desc, url)
                    }
                    showDialog = false
                }
            )
        }

        if (showDeleteDialog && deviceToDelete != null) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false; deviceToDelete = null },
                title = { Text("Delete Switch?") },
                text = { Text("Are you sure you want to delete '${deviceToDelete?.name}'?") },
                confirmButton = {
                    Button(onClick = { deviceToDelete?.let { relayViewModel.deleteRelay(it.id) }; showDeleteDialog = false; deviceToDelete = null }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) { Text("Delete", color = Color.White) }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false; deviceToDelete = null }) { Text("Cancel") }
                }
            )
        }
    }
}

// ... (SwitchConfigDialog unchanged) ...
@Composable
fun SwitchConfigDialog(
    deviceToEdit: RelayDevice?,
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit
) {
    var name by remember { mutableStateOf(deviceToEdit?.name ?: "") }
    var description by remember { mutableStateOf(deviceToEdit?.description ?: "") }
    var connectionUrl by remember { mutableStateOf(deviceToEdit?.connectionUrl ?: "") }

    val isEditMode = deviceToEdit != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = if (isEditMode) "Edit Switch" else "Add New Switch") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text("Switch Name") }, modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description, onValueChange = { description = it },
                    label = { Text("Description") }, modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = connectionUrl, onValueChange = { connectionUrl = it },
                    label = { Text("Connection URL") },
                    placeholder = { Text("http://192.168.x.x/toggle") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(name, description, connectionUrl) },
                colors = ButtonDefaults.buttonColors(containerColor = PowerSensePurple)
            ) {
                Text(if (isEditMode) "Save" else "Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun SwitchCard(
    item: RelayDevice,
    onToggle: () -> Unit,
    onFavoriteToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp), // Added Elevation
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)) // Added Border
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )

                IconButton(onClick = onFavoriteToggle) {
                    Icon(
                        imageVector = if (item.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = "Favorite",
                        tint = if (item.isFavorite) Color(0xFFFFC107) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Switch(
                    checked = item.isOn,
                    onCheckedChange = { onToggle() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = PowerSenseGreen
                    )
                )
            }

            Text(if (item.isOn) "ON" else "OFF", color = if (item.isOn) PowerSenseGreen else Color.Gray, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(item.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onEdit) { Icon(Icons.Outlined.Edit, "Edit", modifier = Modifier.size(18.dp)); Text(" Edit") }
                TextButton(onClick = onDelete) { Icon(Icons.Outlined.Delete, "Delete", tint = Color.Red, modifier = Modifier.size(18.dp)); Text(" Delete", color = Color.Red) }
            }
        }
    }
}