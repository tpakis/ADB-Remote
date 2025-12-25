package com.example.adbremote.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.adbremote.ui.rcu.Tag
import com.example.adbremote.ui.remote.RemoteScreen
import com.example.adbremote.viewmodel.AdbController
import com.example.adbremote.viewmodel.AdbUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdbRemoteApp(
    controller: AdbController,
    modifier: Modifier = Modifier
) {
    val uiState by controller.uiState.collectAsState()
    var selectedTab by remember { mutableStateOf(0) }

    Scaffold(
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
                    onClearError = controller::clearError,
                    onStartScan = controller::startScan,
                    onStopScan = controller::stopScan,
                    onAddDiscoveredDevice = controller::addDiscoveredDevice
                )
                1 -> CommandScreen(
                    uiState = uiState,
                    onExecuteCommand = controller::executeCommand,
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
