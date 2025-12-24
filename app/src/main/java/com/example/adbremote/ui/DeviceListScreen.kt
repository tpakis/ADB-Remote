package com.example.adbremote.ui

import androidx.compose.foundation.clickable
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
import com.example.adbremote.viewmodel.AdbUiState

@Composable
fun DeviceListScreen(
    uiState: AdbUiState,
    onAddDevice: (String) -> Unit,
    onRemoveDevice: (AdbDevice) -> Unit,
    onSelectDevice: (AdbDevice) -> Unit,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onClearError: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddDialog by remember { mutableStateOf(false) }

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
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
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

        // Device list header
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
            IconButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add device")
            }
        }

        // Device list
        if (uiState.devices.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No devices added\nTap + to add a device",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
                        onRemove = { onRemoveDevice(device) }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddDeviceDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { input ->
                onAddDevice(input)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun DeviceCard(
    device: AdbDevice,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect),
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
                Text(
                    text = device.name,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "${device.host}:${device.port}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
    onAdd: (String) -> Unit
) {
    var deviceInput by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Device") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

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
                           "• Host emulator: emulator-5554@host or emulator-5556@host\n" +
                           "• Same emulator: emulator-5554 (connects to itself)\n" +
                           "• IP address: 192.168.1.100:5555\n" +
                           "• IP default port: 192.168.1.100",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Important:\n" +
                           "• Use @host when connecting from an emulator to another emulator on the host machine (10.0.2.2)\n" +
                           "• Without @host uses 127.0.0.1 (only works for self-connection)\n" +
                           "• Physical devices: Use actual IP address with 'adb tcpip 5555' enabled first",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (deviceInput.isNotBlank()) {
                        onAdd(deviceInput.trim())
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
