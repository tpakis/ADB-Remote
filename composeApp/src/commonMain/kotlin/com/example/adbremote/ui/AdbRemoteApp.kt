package com.example.adbremote.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.adbremote.platform.PlatformClipboard
import com.example.adbremote.platform.PlatformFilePicker
import com.example.adbremote.platform.PlatformFileSaver
import com.example.adbremote.platform.saveTextFileAwait
import com.example.adbremote.platform.saveBinaryFileAwait
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
    clipboard: PlatformClipboard,
    modifier: Modifier = Modifier,
    systemAdbDeviceCount: Int = 0,
    onChangeSystemAdbDevice: () -> Unit = {},
    onScanLocalAdb: (() -> Unit)? = null
) {
    val uiState by controller.uiState.collectAsState()
    var selectedTab by remember { mutableStateOf(0) }
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // State for save-to-file command
    var pendingSaveCommand by remember { mutableStateOf<Pair<String, String>?>(null) }
    var isInstalling by remember { mutableStateOf(false) }
    var isGeneratingBugreport by remember { mutableStateOf(false) }
    var pendingBugreportFileName by remember { mutableStateOf<String?>(null) }

    // Track file operation status for UI
    var fileOperationStatus by remember { mutableStateOf(FileOperationStatus()) }

    // Handle save-to-file command execution
    LaunchedEffect(pendingSaveCommand) {
        val (command, defaultFileName) = pendingSaveCommand ?: return@LaunchedEffect

        fileOperationStatus = FileOperationStatus(
            isInProgress = true,
            operationName = "Fetching logcat...",
            fileName = defaultFileName
        )

        // First execute the command
        val result = controller.executeCommandWithResult(command)

        if (result.isSuccess) {
            val output = result.getOrThrow()
            fileOperationStatus = FileOperationStatus(
                isInProgress = true,
                operationName = "Saving to file...",
                fileName = defaultFileName
            )
            // Save to file and await result
            val path = fileSaver.saveTextFileAwait(defaultFileName, output)
            fileOperationStatus = FileOperationStatus()
            if (path != null) {
                snackbarHostState.showSnackbar("Saved to: $path")
            } else {
                snackbarHostState.showSnackbar("Save cancelled or failed")
            }
        } else {
            val error = result.exceptionOrNull()
            fileOperationStatus = FileOperationStatus()
            snackbarHostState.showSnackbar("Command failed: ${error?.message}")
        }

        pendingSaveCommand = null
    }

    // Handle bugreport generation
    LaunchedEffect(pendingBugreportFileName) {
        val defaultFileName = pendingBugreportFileName ?: return@LaunchedEffect
        isGeneratingBugreport = true

        fileOperationStatus = FileOperationStatus(
            isInProgress = true,
            operationName = "Generating bug report...",
            fileName = defaultFileName
        )

        val result = controller.executeBugreport()

        if (result.isSuccess) {
            val zipData = result.getOrThrow()
            fileOperationStatus = FileOperationStatus(
                isInProgress = true,
                operationName = "Saving bug report...",
                fileName = defaultFileName
            )
            // Save the zip file and await result
            val path = fileSaver.saveBinaryFileAwait(defaultFileName, zipData, "application/zip")
            isGeneratingBugreport = false
            fileOperationStatus = FileOperationStatus()
            if (path != null) {
                snackbarHostState.showSnackbar("Bug report saved to: $path")
            } else {
                snackbarHostState.showSnackbar("Save cancelled or failed")
            }
        } else {
            val error = result.exceptionOrNull()
            isGeneratingBugreport = false
            fileOperationStatus = FileOperationStatus()
            snackbarHostState.showSnackbar("Bugreport failed: ${error?.message}")
        }

        pendingBugreportFileName = null
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
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Info, contentDescription = null) },
                    label = { Text("Help") },
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 }
                )
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when (selectedTab) {
                0 -> DeviceListScreen(
                    uiState = uiState,
                    onAddDevice = { address, name -> controller.addDevice(address, name) },
                    onRemoveDevice = controller::removeDevice,
                    onSelectDevice = controller::selectDevice,
                    onEditDevice = controller::updateDevice,
                    onConnect = controller::connect,
                    onDisconnect = controller::disconnect,
                    onCancelConnection = controller::cancelConnection,
                    onClearError = controller::clearError,
                    onStartScan = controller::startScan,
                    onStopScan = controller::stopScan,
                    onAddDiscoveredDevice = controller::addDiscoveredDevice,
                    systemAdbDeviceCount = systemAdbDeviceCount,
                    onChangeSystemAdbDevice = onChangeSystemAdbDevice,
                    onScanLocalAdb = onScanLocalAdb
                )
                1 -> CommandScreen(
                    uiState = uiState,
                    onExecuteCommand = controller::executeCommand,
                    onSaveToFileCommand = { command, defaultFileName ->
                        if (!fileOperationStatus.isInProgress) {
                            pendingSaveCommand = command to defaultFileName
                        }
                    },
                    onBugreport = { defaultFileName ->
                        if (!isGeneratingBugreport && !fileOperationStatus.isInProgress) {
                            pendingBugreportFileName = defaultFileName
                        }
                    },
                    isGeneratingBugreport = isGeneratingBugreport,
                    fileOperationStatus = fileOperationStatus,
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
                    onClearHistory = controller::clearHistory,
                    onCopyToClipboard = { text ->
                        clipboard.copyToClipboard(text)
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Copied to clipboard")
                        }
                    }
                )
                2 -> RcuScreen(
                    uiState = uiState,
                    onKeyPressed = { tag ->
                        controller.executeCommand(tag.toAdbCommand())
                    }
                )
                3 -> HelpScreen()
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
