package com.powersense.screens.tabs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.powersense.R
import com.powersense.data.RelayDevice
import com.powersense.ui.theme.PowerSenseGreen
import com.powersense.ui.theme.PowerSenseOrange
import com.powersense.ui.theme.PowerSensePurple
import com.powersense.utils.FeedbackUtils
import com.powersense.viewmodels.RelayViewModel
import com.powersense.viewmodels.ThemeViewModel
import com.powersense.viewmodels.TimerUiState
import java.util.concurrent.TimeUnit
import androidx.compose.foundation.isSystemInDarkTheme
import com.powersense.viewmodels.ThemeOption

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimulationScreen(
    relayViewModel: RelayViewModel = viewModel(),
    themeViewModel: ThemeViewModel = viewModel()
) {
    val switches by relayViewModel.relays.collectAsState()
    val isLoading by relayViewModel.isLoading.collectAsState()
    val timerStates by relayViewModel.timerStates.collectAsState()

    // ERROR HANDLING
    val errorMessage by relayViewModel.errorMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    val currentTheme by themeViewModel.themeState.collectAsState()
    val isHapticsEnabled by themeViewModel.isHapticsEnabled.collectAsState()
    val context = LocalContext.current

    // Dialog States
    var showDialog by remember { mutableStateOf(false) }
    var editingDevice by remember { mutableStateOf<RelayDevice?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var deviceToDelete by remember { mutableStateOf<RelayDevice?>(null) }
    var showTimerDialog by remember { mutableStateOf(false) }
    var timerDevice by remember { mutableStateOf<RelayDevice?>(null) }

    // Show Snackbar when error occurs
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            relayViewModel.clearError() // Reset after showing
        }
    }

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
        // ATTACH SNACKBAR HOST HERE
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },

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
                    timerState = timerStates[device.id],
                    onToggle = {
                        FeedbackUtils.triggerFeedback(context, !device.isOn, isHapticsEnabled)
                        relayViewModel.toggleRelay(device)
                    },
                    onFavoriteToggle = { relayViewModel.toggleFavorite(device) },
                    onEdit = {
                        editingDevice = device
                        showDialog = true
                    },
                    onDelete = {
                        deviceToDelete = device
                        showDeleteDialog = true
                    },
                    onTimerClick = {
                        timerDevice = device
                        showTimerDialog = true
                    },
                    onStartTimer = { relayViewModel.startTimer(device.id) },
                    onStopTimer = { relayViewModel.stopTimer(device.id) },
                    onResetTimer = { relayViewModel.resetTimer(device.id) },
                    onClearTimer = { relayViewModel.clearTimer(device.id) }
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

        // --- DIALOGS ---

        if (showDialog) {
            SwitchConfigDialog(
                deviceToEdit = editingDevice,
                onDismiss = { showDialog = false },
                onSave = { name, desc, url, threshold, unit ->
                    if (editingDevice == null) {
                        relayViewModel.addRelay(name, desc, url, threshold, unit)
                    } else {
                        relayViewModel.updateRelay(editingDevice!!.id, name, desc, url, threshold, unit)
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

        if (showTimerDialog && timerDevice != null) {
            TimerDialog(
                device = timerDevice!!,
                onDismiss = { showTimerDialog = false; timerDevice = null },
                onConfigureTimer = { value, unit, autoStart ->
                    relayViewModel.setTimer(timerDevice!!, value, unit)
                    if (autoStart) {
                        relayViewModel.startTimer(timerDevice!!.id)
                    }
                    showTimerDialog = false
                    timerDevice = null
                }
            )
        }
    }
}

// --- HELPER DIALOGS & CARDS ---

@Composable
fun TimerDialog(
    device: RelayDevice,
    onDismiss: () -> Unit,
    onConfigureTimer: (Int, String, Boolean) -> Unit
) {
    var durationText by remember { mutableStateOf("") }
    var selectedUnit by remember { mutableStateOf("Minutes") }
    var unitExpanded by remember { mutableStateOf(false) }
    val units = listOf("Seconds", "Minutes", "Hours", "Days")

    val targetAction = if (device.isOn) "OFF" else "ON"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Timer, contentDescription = null, tint = PowerSensePurple)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Set Timer")
            }
        },
        text = {
            Column {
                Text("Turn ${device.name} $targetAction after:", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(16.dp))

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = durationText,
                        onValueChange = { if (it.all { char -> char.isDigit() }) durationText = it },
                        label = { Text("Duration") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    Box {
                        OutlinedButton(onClick = { unitExpanded = true }) {
                            Text(selectedUnit)
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                        DropdownMenu(expanded = unitExpanded, onDismissRequest = { unitExpanded = false }) {
                            units.forEach { unit ->
                                DropdownMenuItem(text = { Text(unit) }, onClick = { selectedUnit = unit; unitExpanded = false })
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = {
                    val value = durationText.toIntOrNull()
                    if (value != null && value > 0) onConfigureTimer(value, selectedUnit, false)
                }, enabled = durationText.isNotEmpty()) { Text("OK") }

                Button(onClick = {
                    val value = durationText.toIntOrNull()
                    if (value != null && value > 0) onConfigureTimer(value, selectedUnit, true)
                }, enabled = durationText.isNotEmpty(), colors = ButtonDefaults.buttonColors(containerColor = PowerSensePurple)) { Text("Start") }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun SwitchConfigDialog(
    deviceToEdit: RelayDevice?,
    onDismiss: () -> Unit,
    onSave: (String, String, String, Double?, String) -> Unit
) {
    var name by remember { mutableStateOf(deviceToEdit?.name ?: "") }
    var description by remember { mutableStateOf(deviceToEdit?.description ?: "") }
    var connectionUrl by remember { mutableStateOf(deviceToEdit?.connectionUrl ?: "") }
    var thresholdStr by remember { mutableStateOf(deviceToEdit?.threshold?.toString() ?: "") }
    var selectedUnit by remember { mutableStateOf(deviceToEdit?.thresholdUnit ?: "A") }
    var unitExpanded by remember { mutableStateOf(false) }

    val isEditMode = deviceToEdit != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = if (isEditMode) "Edit Switch" else "Add New Switch") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Switch Name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = connectionUrl, onValueChange = { if (it.all { char -> char.isDigit() }) connectionUrl = it }, label = { Text("Physical Relay ID") }, placeholder = { Text("e.g. 1 or 2") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(value = thresholdStr, onValueChange = { thresholdStr = it }, label = { Text("Max Limit") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), placeholder = { Text("Optional") })
                    Box {
                        OutlinedButton(onClick = { unitExpanded = true }) { Text(selectedUnit); Icon(Icons.Default.ArrowDropDown, contentDescription = null) }
                        DropdownMenu(expanded = unitExpanded, onDismissRequest = { unitExpanded = false }) {
                            DropdownMenuItem(text = { Text("Amps (A)") }, onClick = { selectedUnit = "A"; unitExpanded = false })
                            DropdownMenuItem(text = { Text("Watts (W)") }, onClick = { selectedUnit = "W"; unitExpanded = false })
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { val thresholdVal = thresholdStr.toDoubleOrNull(); onSave(name, description, connectionUrl, thresholdVal, selectedUnit) }, colors = ButtonDefaults.buttonColors(containerColor = PowerSensePurple)) { Text(if (isEditMode) "Save" else "Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun SwitchCard(
    item: RelayDevice,
    timerState: TimerUiState?,
    onToggle: () -> Unit,
    onFavoriteToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onTimerClick: () -> Unit,
    onStartTimer: () -> Unit,
    onStopTimer: () -> Unit,
    onResetTimer: () -> Unit,
    onClearTimer: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(text = item.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                IconButton(onClick = onFavoriteToggle) {
                    Icon(imageVector = if (item.isFavorite) Icons.Default.Star else Icons.Default.StarBorder, contentDescription = "Favorite", tint = if (item.isFavorite) Color(0xFFFFC107) else MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = item.isOn, onCheckedChange = { onToggle() }, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = PowerSenseGreen))
            }
            Text(if (item.isOn) "ON" else "OFF", color = if (item.isOn) PowerSenseGreen else Color.Gray, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(item.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

            if (item.threshold != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text("Alert Limit: ${item.threshold}${item.thresholdUnit}", style = MaterialTheme.typography.labelSmall, color = PowerSenseOrange)
            }

            if (timerState != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Timer, contentDescription = null, tint = PowerSensePurple, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = formatMillis(timerState.remainingMillis), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(onClick = onClearTimer, modifier = Modifier.size(20.dp)) { Icon(Icons.Default.Close, contentDescription = "Close Timer", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    if (timerState.isRunning) {
                        Button(onClick = onStopTimer, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer), contentPadding = PaddingValues(horizontal = 24.dp)) {
                            Icon(Icons.Default.Stop, contentDescription = null); Spacer(modifier = Modifier.width(8.dp)); Text("Stop")
                        }
                    } else {
                        Button(onClick = onStartTimer, colors = ButtonDefaults.buttonColors(containerColor = PowerSenseGreen), contentPadding = PaddingValues(horizontal = 24.dp)) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null); Spacer(modifier = Modifier.width(8.dp)); Text("Start")
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    OutlinedButton(onClick = onResetTimer) { Icon(Icons.Default.Refresh, contentDescription = null) }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onTimerClick) { Icon(Icons.Outlined.Timer, "Set Timer", modifier = Modifier.size(18.dp)); Text(" Timer") }
                TextButton(onClick = onEdit) { Icon(Icons.Outlined.Edit, "Edit", modifier = Modifier.size(18.dp)); Text(" Edit") }
                TextButton(onClick = onDelete) { Icon(Icons.Outlined.Delete, "Delete", tint = Color.Red, modifier = Modifier.size(18.dp)); Text(" Delete", color = Color.Red) }
            }
        }
    }
}

fun formatMillis(millis: Long): String {
    val days = TimeUnit.MILLISECONDS.toDays(millis)
    val hours = TimeUnit.MILLISECONDS.toHours(millis) % 24
    val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
    val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60
    return if (days > 0) String.format("%dd %02d:%02d:%02d", days, hours, minutes, seconds) else String.format("%02d:%02d:%02d", hours, minutes, seconds)
}