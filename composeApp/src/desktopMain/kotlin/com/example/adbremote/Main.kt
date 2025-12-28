package com.example.adbremote

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.example.adbremote.platform.PlatformClipboardFactory
import com.example.adbremote.platform.PlatformContext
import com.example.adbremote.platform.PlatformCrypto
import com.example.adbremote.platform.PlatformFilePickerFactory
import com.example.adbremote.platform.PlatformFileSaverFactory
import com.example.adbremote.platform.PlatformStorageFactory
import com.example.adbremote.platform.SystemAdbDevice
import com.example.adbremote.platform.initializePlatform
import com.example.adbremote.ui.AdbRemoteApp
import com.example.adbremote.ui.theme.ADBRemoteTheme
import com.example.adbremote.viewmodel.AdbController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

fun main() = application {
    // Initialize platform
    initializePlatform(PlatformContext())

    val windowState = rememberWindowState(width = 650.dp, height = 1400.dp)

    Window(
        onCloseRequest = ::exitApplication,
        state = windowState,
        title = "ADB Remote"
    ) {
        val scope = remember { CoroutineScope(SupervisorJob() + Dispatchers.Main) }

        val controller = remember {
            val storage = PlatformStorageFactory.create("adb_remote_prefs")
            val keyStorage = PlatformStorageFactory.create("adb_keys")
            val crypto = PlatformCrypto()
            AdbController(scope, storage, keyStorage, crypto)
        }

        val platformContext = remember { PlatformContext() }
        val fileSaver = remember { PlatformFileSaverFactory.create(platformContext) }
        val filePicker = remember { PlatformFilePickerFactory.create(platformContext) }
        val clipboard = remember { PlatformClipboardFactory.create(platformContext) }

        // System ADB detection state
        var systemAdbDevices by remember { mutableStateOf<List<SystemAdbDevice>>(emptyList()) }
        var showSystemAdbDialog by remember { mutableStateOf(false) }
        var systemAdbChecked by remember { mutableStateOf(false) }

        // Check for system ADB devices on startup
        LaunchedEffect(Unit) {
            if (controller.isSystemAdbAvailable()) {
                val devices = controller.getSystemAdbDevices()
                if (devices.isNotEmpty()) {
                    systemAdbDevices = devices
                    showSystemAdbDialog = true
                }
            }
            systemAdbChecked = true
        }

        DisposableEffect(Unit) {
            onDispose {
                controller.cleanup()
            }
        }

        ADBRemoteTheme {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                // Show system ADB dialog if devices detected
                if (showSystemAdbDialog && systemAdbDevices.isNotEmpty()) {
                    SystemAdbDialog(
                        devices = systemAdbDevices,
                        onSelectDevice = { device ->
                            controller.selectSystemAdbDevice(device)
                            showSystemAdbDialog = false
                        },
                        onDismiss = {
                            showSystemAdbDialog = false
                        }
                    )
                }

                // Main app content
                AdbRemoteApp(
                    controller = controller,
                    fileSaver = fileSaver,
                    filePicker = filePicker,
                    clipboard = clipboard,
                    systemAdbDeviceCount = systemAdbDevices.size,
                    onChangeSystemAdbDevice = {
                        showSystemAdbDialog = true
                    }
                )
            }
        }
    }
}

/**
 * Dialog shown when system ADB devices are detected on startup.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SystemAdbDialog(
    devices: List<SystemAdbDevice>,
    onSelectDevice: (SystemAdbDevice) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("System ADB Devices Detected") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "The following devices are connected via your system's ADB server. " +
                           "Select one to use it directly, or dismiss to use manual TCP/IP connection.",
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(8.dp))

                devices.forEach { device ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectDevice(device) },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Text(
                                text = device.model ?: device.serial,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = device.serial,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (device.product != null) {
                                Text(
                                    text = "Product: ${device.product}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Use Manual Connection")
            }
        }
    )
}
