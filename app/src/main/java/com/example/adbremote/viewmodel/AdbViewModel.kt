package com.example.adbremote.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.adbremote.adb.AdbConnection
import com.example.adbremote.adb.AdbKeyManager
import com.example.adbremote.model.AdbDevice
import com.example.adbremote.model.CommandResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AdbUiState(
    val devices: List<AdbDevice> = emptyList(),
    val selectedDevice: AdbDevice? = null,
    val commandHistory: List<CommandResult> = emptyList(),
    val isConnecting: Boolean = false,
    val isExecuting: Boolean = false,
    val errorMessage: String? = null
)

class AdbViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(AdbUiState())
    val uiState: StateFlow<AdbUiState> = _uiState.asStateFlow()

    private var currentConnection: AdbConnection? = null
    private val keyManager: AdbKeyManager

    init {
        // Initialize the key manager for ADB authentication
        keyManager = AdbKeyManager(application.applicationContext)
        keyManager.initialize()
    }

    companion object {
        private const val TAG = "AdbViewModel"
    }

    fun addDevice(host: String, port: Int = 5555) {
        val name = "$host:$port"
        val device = AdbDevice(name = name, host = host, port = port)

        _uiState.update { currentState ->
            val existingDevice = currentState.devices.find { it.host == host && it.port == port }
            if (existingDevice != null) {
                currentState.copy(errorMessage = "Device already added")
            } else {
                currentState.copy(
                    devices = currentState.devices + device,
                    errorMessage = null
                )
            }
        }
    }

    fun removeDevice(device: AdbDevice) {
        if (_uiState.value.selectedDevice == device) {
            disconnect()
        }

        _uiState.update { currentState ->
            currentState.copy(
                devices = currentState.devices - device
            )
        }
    }

    fun selectDevice(device: AdbDevice) {
        if (_uiState.value.selectedDevice?.host != device.host ||
            _uiState.value.selectedDevice?.port != device.port) {
            disconnect()
        }

        _uiState.update { it.copy(selectedDevice = device, commandHistory = emptyList()) }
    }

    fun connect() {
        val device = _uiState.value.selectedDevice ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isConnecting = true, errorMessage = null) }

            try {
                val connection = AdbConnection(device.host, device.port, keyManager)
                val result = connection.connect()

                result.fold(
                    onSuccess = {
                        currentConnection = connection
                        _uiState.update { currentState ->
                            currentState.copy(
                                isConnecting = false,
                                selectedDevice = device.copy(isConnected = true),
                                devices = currentState.devices.map {
                                    if (it.host == device.host && it.port == device.port) {
                                        it.copy(isConnected = true)
                                    } else {
                                        it
                                    }
                                }
                            )
                        }
                        Log.d(TAG, "Connected to ${device.name}")
                    },
                    onFailure = { error ->
                        _uiState.update { currentState ->
                            currentState.copy(
                                isConnecting = false,
                                errorMessage = "Connection failed: ${error.message}"
                            )
                        }
                        Log.e(TAG, "Connection failed", error)
                    }
                )
            } catch (e: Exception) {
                _uiState.update { it.copy(isConnecting = false, errorMessage = e.message) }
                Log.e(TAG, "Connection error", e)
            }
        }
    }

    fun disconnect() {
        currentConnection?.disconnect()
        currentConnection = null

        _uiState.update { currentState ->
            val device = currentState.selectedDevice
            currentState.copy(
                selectedDevice = device?.copy(isConnected = false),
                devices = currentState.devices.map {
                    if (it.host == device?.host && it.port == device.port) {
                        it.copy(isConnected = false)
                    } else {
                        it
                    }
                }
            )
        }
    }

    fun executeCommand(command: String) {
        val connection = currentConnection
        if (connection == null || !connection.isConnected()) {
            _uiState.update { it.copy(errorMessage = "Not connected to device") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isExecuting = true, errorMessage = null) }

            try {
                val result = connection.executeCommand(command)

                result.fold(
                    onSuccess = { output ->
                        _uiState.update { currentState ->
                            currentState.copy(
                                isExecuting = false,
                                commandHistory = currentState.commandHistory + CommandResult(
                                    command = command,
                                    output = output.ifEmpty { "(no output)" }
                                )
                            )
                        }
                    },
                    onFailure = { error ->
                        _uiState.update { currentState ->
                            currentState.copy(
                                isExecuting = false,
                                commandHistory = currentState.commandHistory + CommandResult(
                                    command = command,
                                    output = error.message ?: "Unknown error",
                                    isError = true
                                )
                            )
                        }
                    }
                )
            } catch (e: Exception) {
                _uiState.update { currentState ->
                    currentState.copy(
                        isExecuting = false,
                        commandHistory = currentState.commandHistory + CommandResult(
                            command = command,
                            output = e.message ?: "Unknown error",
                            isError = true
                        )
                    )
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun clearHistory() {
        _uiState.update { it.copy(commandHistory = emptyList()) }
    }

    override fun onCleared() {
        super.onCleared()
        disconnect()
    }
}
