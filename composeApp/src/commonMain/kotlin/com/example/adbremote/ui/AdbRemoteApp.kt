package com.example.adbremote.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.adbremote.platform.PlatformFilePicker
import com.example.adbremote.platform.PlatformFileSaver
import com.example.adbremote.ui.rcu.Tag
import com.example.adbremote.ui.remote.RemoteScreen
import com.example.adbremote.viewmodel.AdbController
import com.example.adbremote.viewmodel.AdbUiState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdbRemoteApp(
    controller: AdbController,
    fileSaver: PlatformFileSaver,
    filePicker: PlatformFilePicker,
    modifier: Modifier = Modifier
) {
    val uiState by controller.uiState.collectAsState()
    var selectedTab by remember { mutableStateOf(0) }
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // State for save-to-file command
    var pendingSaveCommand by remember { mutableStateOf<Pair<String, String>?>(null) }
    var isSaving by remember { mutableStateOf(false) }
    var isInstalling by remember { mutableStateOf(false) }

    // Handle save-to-file command execution
    LaunchedEffect(pendingSaveCommand) {
        val (command, defaultFileName) = pendingSaveCommand ?: return@LaunchedEffect
        isSaving = true

        // First execute the command
        val result = controller.executeCommandWithResult(command)

        result.fold(
            onSuccess = { output ->
                // Then save to file using platform file saver
                fileSaver.saveTextFile(defaultFileName, output) { path ->
                    coroutineScope.launch {
                        if (path != null) {
                            snackbarHostState.showSnackbar("Saved to: $path")
                        } else {
                            snackbarHostState.showSnackbar("Save cancelled or failed")
                        }
                    }
                }
            },
            onFailure = { error ->
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Command failed: ${error.message}")
                }
            }
        )

        isSaving = false
        pendingSaveCommand = null
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("ADB Remote") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, contentDescription = null) },
                    label = { Text("Devices") },
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Build, contentDescription = null) },
                    label = { Text("Commands") },
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    enabled = uiState.selectedDevice?.isConnected == true
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    label = { Text("RCU") },
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    enabled = uiState.selectedDevice?.isConnected == true
                )
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when (selectedTab) {
                0 -> DeviceListScreen(
                    uiState = uiState,
                    onAddDevice = controller::addDevice,
                    onRemoveDevice = controller::removeDevice,
                    onSelectDevice = controller::selectDevice,
                    onConnect = controller::connect,
                    onDisconnect = controller::disconnect,
                    onCancelConnection = controller::cancelConnection,
                    onClearError = controller::clearError,
                    onStartScan = controller::startScan,
                    onStopScan = controller::stopScan,
                    onAddDiscoveredDevice = controller::addDiscoveredDevice
                )
                1 -> CommandScreen(
                    uiState = uiState,
                    onExecuteCommand = controller::executeCommand,
                    onSaveToFileCommand = { command, defaultFileName ->
                        pendingSaveCommand = command to defaultFileName
                    },
                    onInstallApk = {
                        if (isInstalling) return@CommandScreen
                        filePicker.pickFile("application/vnd.android.package-archive") { path ->
                            if (path != null) {
                                isInstalling = true
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Pushing and installing APK...")
                                    val result = controller.installApk(path)
                                    result.fold(
                                        onSuccess = { message ->
                                            snackbarHostState.showSnackbar(message)
                                        },
                                        onFailure = { error ->
                                            snackbarHostState.showSnackbar("Install failed: ${error.message}")
                                        }
                                    )
                                    isInstalling = false
                                }
                            }
                        }
                    },
                    isInstalling = isInstalling,
                    onClearHistory = controller::clearHistory
                )
                2 -> RcuScreen(
                    uiState = uiState,
                    onKeyPressed = { tag ->
                        controller.executeCommand(tag.toAdbCommand())
                    }
                )
            }
        }
    }
}

@Composable
fun RcuScreen(
    uiState: AdbUiState,
    onKeyPressed: (Tag) -> Unit
) {
    RemoteScreen(callback = onKeyPressed)
}
