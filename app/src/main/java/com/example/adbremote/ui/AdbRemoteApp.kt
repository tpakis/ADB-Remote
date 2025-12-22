package com.example.adbremote.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.adbremote.viewmodel.AdbViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdbRemoteApp(
    viewModel: AdbViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }

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
                    icon = { Icon(Icons.Default.Devices, contentDescription = null) },
                    label = { Text("Devices") },
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Code, contentDescription = null) },
                    label = { Text("Commands") },
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    enabled = uiState.selectedDevice?.isConnected == true
                )
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when (selectedTab) {
                0 -> DeviceListScreen(
                    uiState = uiState,
                    onAddDevice = viewModel::addDevice,
                    onRemoveDevice = viewModel::removeDevice,
                    onSelectDevice = viewModel::selectDevice,
                    onConnect = viewModel::connect,
                    onDisconnect = viewModel::disconnect,
                    onClearError = viewModel::clearError
                )
                1 -> CommandScreen(
                    uiState = uiState,
                    onExecuteCommand = viewModel::executeCommand,
                    onClearHistory = viewModel::clearHistory
                )
            }
        }
    }
}
