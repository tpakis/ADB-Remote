package com.example.adbremote.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.adbremote.model.AdbDevice
import com.example.adbremote.platform.DiscoveredDevice
import com.example.adbremote.viewmodel.AdbUiState

@Composable
fun DeviceListScreen(
    uiState: AdbUiState,
    onAddDevice: (String, String?) -> Unit,
    onRemoveDevice: (AdbDevice) -> Unit,
    onSelectDevice: (AdbDevice) -> Unit,
    onEditDevice: (AdbDevice, AdbDevice) -> Unit,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onCancelConnection: () -> Unit,
    onClearError: () -> Unit,
    onStartScan: () -> Unit = {},
    onStopScan: () -> Unit = {},
    onAddDiscoveredDevice: (DiscoveredDevice) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var deviceToEdit by remember { mutableStateOf<AdbDevice?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Error message
        uiState.errorMessage?.let { error ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onClearError) {
                        Icon(Icons.Default.Close, contentDescription = "Dismiss")
                    }
                }
            }
        }

        // Connection status
        uiState.selectedDevice?.let { device ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (device.isConnected)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = device.name,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = if (device.isConnected) "Connected" else "Not connected",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        if (uiState.isConnecting) {
                            OutlinedButton(onClick = onCancelConnection) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Cancel")
                            }
                        } else if (device.isConnected) {
                            Button(onClick = onDisconnect) {
                                Text("Disconnect")
                            }
                        } else {
                            Button(onClick = onConnect) {
                                Text("Connect")
                            }
                        }
                    }
                }
            }
        }

        // Device list header with scan button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Devices",
                style = MaterialTheme.typography.titleLarge
            )
            Row {
                // Scan button
                if (uiState.isScanning) {
                    TextButton(onClick = onStopScan) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Stop")
                    }
                } else {
                    TextButton(onClick = onStartScan) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Scan")
                    }
                }
                // Add button
                IconButton(onClick = { showAddDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add device")
                }
            }
        }

        // Discovered devices section
        if (uiState.discoveredDevices.isNotEmpty() || uiState.isScanning) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Discovered Devices",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        if (uiState.isScanning) {
                            Text(
                                text = "Scanning...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }

                    if (uiState.discoveredDevices.isEmpty() && uiState.isScanning) {
                        Text(
                            text = "Searching for ADB devices on local network...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    } else {
                        uiState.discoveredDevices.forEach { device ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = device.ipAddress,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                    Text(
                                        text = "Port ${device.port}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                                    )
                                }
                                // Check if device already added
                                val alreadyAdded = uiState.devices.any {
                                    it.host == device.ipAddress && it.port == device.port
                                }
                                if (alreadyAdded) {
                                    Text(
                                        text = "Added",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                                    )
                                } else {
                                    TextButton(onClick = { onAddDiscoveredDevice(device) }) {
                                        Icon(
                                            Icons.Default.Add,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Add")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Device list
        if (uiState.devices.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "No devices added",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Tap Scan to find devices or + to add manually",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.devices) { device ->
                    DeviceCard(
                        device = device,
                        isSelected = uiState.selectedDevice == device,
                        onSelect = { onSelectDevice(device) },
                        onRemove = { onRemoveDevice(device) },
                        onEdit = { deviceToEdit = device }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddDeviceDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { address, name ->
                onAddDevice(address, name)
                showAddDialog = false
            }
        )
    }

    deviceToEdit?.let { device ->
        EditDeviceDialog(
            device = device,
            onDismiss = { deviceToEdit = null },
            onSave = { updatedDevice ->
                onEditDevice(device, updatedDevice)
                deviceToEdit = null
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DeviceCard(
    device: AdbDevice,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onRemove: () -> Unit,
    onEdit: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onSelect,
                onLongClick = onEdit
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.secondaryContainer
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                val displayName = device.displayName
                if (!displayName.isNullOrBlank()) {
                    // Show name as title, host:port as subtitle
                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "${device.host}:${device.port}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    // Show only host:port as title
                    Text(
                        text = "${device.host}:${device.port}",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            Row {
                if (device.isConnected) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Connected",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
                IconButton(onClick = onRemove) {
                    Icon(Icons.Default.Delete, contentDescription = "Remove")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDeviceDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String?) -> Unit
) {
    var deviceInput by remember { mutableStateOf("") }
    var deviceName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Device") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Optional device name
                OutlinedTextField(
                    value = deviceName,
                    onValueChange = { deviceName = it },
                    label = { Text("Device Name (optional)") },
                    placeholder = { Text("e.g., Living Room TV") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Quick add buttons for host emulators
                Text(
                    text = "Connect to Host Emulator (from this emulator/device)",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { deviceInput = "emulator-5554@host" },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Emu-5554", style = MaterialTheme.typography.bodySmall)
                    }
                    OutlinedButton(
                        onClick = { deviceInput = "emulator-5556@host" },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Emu-5556", style = MaterialTheme.typography.bodySmall)
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 4.dp))

                // Manual input
                Text(
                    text = "Or Enter Manually",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                OutlinedTextField(
                    value = deviceInput,
                    onValueChange = { deviceInput = it },
                    label = { Text("Device Address") },
                    placeholder = { Text("emulator-5554 or 192.168.1.100:5555") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Help text
                Text(
                    text = "Examples:\n" +
                           "• Host emulator: emulator-5554@host\n" +
                           "• IP address: 192.168.1.100:5555",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (deviceInput.isNotBlank()) {
                        onAdd(deviceInput.trim(), deviceName.trim().takeIf { it.isNotBlank() })
                    }
                },
                enabled = deviceInput.isNotBlank()
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditDeviceDialog(
    device: AdbDevice,
    onDismiss: () -> Unit,
    onSave: (AdbDevice) -> Unit
) {
    var deviceName by remember { mutableStateOf(device.displayName ?: "") }
    var host by remember { mutableStateOf(device.host) }
    var portText by remember { mutableStateOf(device.port.toString()) }

    val isValid = host.isNotBlank() && portText.toIntOrNull() != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Device") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = deviceName,
                    onValueChange = { deviceName = it },
                    label = { Text("Device Name (optional)") },
                    placeholder = { Text("e.g., Living Room TV") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = host,
                    onValueChange = { host = it },
                    label = { Text("Host / IP Address") },
                    placeholder = { Text("192.168.1.100") },
                    singleLine = true,
                    isError = host.isBlank(),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = portText,
                    onValueChange = { portText = it },
                    label = { Text("Port") },
                    placeholder = { Text("5555") },
                    singleLine = true,
                    isError = portText.toIntOrNull() == null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Long-press a device to edit it",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val port = portText.toIntOrNull() ?: device.port
                    val updatedDevice = device.copy(
                        displayName = deviceName.trim().takeIf { it.isNotBlank() },
                        host = host.trim(),
                        port = port,
                        name = "${host.trim()}:$port"
                    )
                    onSave(updatedDevice)
                },
                enabled = isValid
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
