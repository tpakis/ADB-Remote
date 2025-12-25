package com.example.adbremote.viewmodel

import com.example.adbremote.model.AdbDevice
import com.example.adbremote.model.CommandResult

/**
 * UI state for the ADB Remote app.
 * This is shared across all platforms.
 */
data class AdbUiState(
    val devices: List<AdbDevice> = emptyList(),
    val selectedDevice: AdbDevice? = null,
    val commandHistory: List<CommandResult> = emptyList(),
    val recentCommands: List<String> = emptyList(),
    val isConnecting: Boolean = false,
    val isExecuting: Boolean = false,
    val errorMessage: String? = null
)
