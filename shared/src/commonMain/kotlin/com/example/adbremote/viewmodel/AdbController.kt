package com.example.adbremote.viewmodel

import com.example.adbremote.adb.AdbConnection
import com.example.adbremote.adb.AdbKeyManager
import com.example.adbremote.data.DeviceRepository
import com.example.adbremote.model.AdbDevice
import com.example.adbremote.model.CommandResult
import com.example.adbremote.platform.DiscoveredDevice
import com.example.adbremote.platform.NetworkScanner
import com.example.adbremote.platform.PlatformCrypto
import com.example.adbremote.platform.PlatformLogger
import com.example.adbremote.platform.PlatformStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Shared business logic controller for the ADB Remote app.
 * Platform-specific ViewModels delegate to this controller.
 */
class AdbController(
    private val scope: CoroutineScope,
    storage: PlatformStorage,
    keyStorage: PlatformStorage,
    crypto: PlatformCrypto
) {
    private val _uiState = MutableStateFlow(AdbUiState())
    val uiState: StateFlow<AdbUiState> = _uiState.asStateFlow()

    private var currentConnection: AdbConnection? = null
    private val keyManager: AdbKeyManager
    private val deviceRepository: DeviceRepository
    private val networkScanner = NetworkScanner()
    private var scanJob: Job? = null

    companion object {
        private const val TAG = "AdbController"
    }

    init {
        // Initialize key manager
        keyManager = AdbKeyManager(keyStorage, crypto)
        keyManager.initialize()

        // Initialize device repository
        deviceRepository = DeviceRepository(storage)

        // Load saved devices
        loadDevices()
    }

    private fun loadDevices() {
        scope.launch {
            try {
                val savedDevices = deviceRepository.loadDevices()
                val recentCommands = deviceRepository.loadRecentCommands()

                // Try to find last connected device
                val lastDevice = deviceRepository.getLastConnectedDevice()
                val selectedDevice = if (lastDevice != null) {
                    val (host, port) = lastDevice
                    val device = savedDevices.find { it.host == host && it.port == port }
                    if (device != null) {
                        PlatformLogger.d(TAG, "Auto-selected last connected device: ${device.name}")
                        device
                    } else {
                        null
                    }
                } else {
                    null
                }

                // Single atomic state update
                _uiState.update {
                    it.copy(
                        devices = savedDevices,
                        recentCommands = recentCommands,
                        selectedDevice = selectedDevice
                    )
                }
            } catch (e: Exception) {
                PlatformLogger.e(TAG, "Failed to load devices", e)
            }
        }
    }

    private fun saveDevices() {
        scope.launch {
            try {
                deviceRepository.saveDevices(_uiState.value.devices)
            } catch (e: Exception) {
                PlatformLogger.e(TAG, "Failed to save devices", e)
            }
        }
    }

    fun addDevice(input: String) {
        val device = AdbDevice.fromInput(input)

        if (device == null) {
            _uiState.update { it.copy(errorMessage = "Invalid device format. Use 'emulator-5554' or '192.168.1.100:5555'") }
            return
        }

        // Check if device already exists
        val existingDevice = _uiState.value.devices.find { it.host == device.host && it.port == device.port }
        if (existingDevice != null) {
            _uiState.update { it.copy(errorMessage = "Device already added") }
            return
        }

        // Add device to state
        _uiState.update { currentState ->
            currentState.copy(
                devices = currentState.devices + device,
                errorMessage = null
            )
        }

        // Save after state is updated
        saveDevices()
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
        saveDevices()
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

        scope.launch {
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
                        // Save last connected device
                        scope.launch {
                            deviceRepository.saveLastConnectedDevice(device)
                        }
                        PlatformLogger.d(TAG, "Connected to ${device.name}")
                    },
                    onFailure = { error ->
                        _uiState.update { currentState ->
                            currentState.copy(
                                isConnecting = false,
                                errorMessage = "Connection failed: ${error.message}"
                            )
                        }
                        PlatformLogger.e(TAG, "Connection failed", error)
                    }
                )
            } catch (e: Exception) {
                _uiState.update { it.copy(isConnecting = false, errorMessage = e.message) }
                PlatformLogger.e(TAG, "Connection error", e)
            }
        }
    }

    fun disconnect() {
        currentConnection?.disconnect()
        currentConnection = null
        updateDisconnectedState()
    }

    private fun handleConnectionLost() {
        currentConnection = null
        updateDisconnectedState()
    }

    private fun updateDisconnectedState() {
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
        // Strip "adb shell" prefix if user accidentally included it
        val sanitizedCommand = command.trim()
            .removePrefix("adb shell ")
            .removePrefix("adb shell")
            .trim()

        val connection = currentConnection
        if (connection == null || !connection.isConnected()) {
            PlatformLogger.w(TAG, "Connection is dead, updating UI state")
            handleConnectionLost()
            _uiState.update { it.copy(errorMessage = "Connection lost. Please reconnect.") }
            return
        }

        scope.launch {
            _uiState.update { it.copy(isExecuting = true, errorMessage = null) }

            try {
                val result = connection.executeCommand(sanitizedCommand)

                result.fold(
                    onSuccess = { output ->
                        // Save successful command to recent commands
                        scope.launch {
                            deviceRepository.addRecentCommand(sanitizedCommand)
                            val updatedRecentCommands = deviceRepository.loadRecentCommands()
                            _uiState.update { it.copy(recentCommands = updatedRecentCommands) }
                        }

                        _uiState.update { currentState ->
                            currentState.copy(
                                isExecuting = false,
                                commandHistory = currentState.commandHistory + CommandResult(
                                    command = sanitizedCommand,
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
                                    command = sanitizedCommand,
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
                            command = sanitizedCommand,
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

    fun cleanup() {
        disconnect()
        stopScan()
    }

    /**
     * Start scanning the local network for ADB-enabled devices.
     */
    fun startScan() {
        // Cancel any existing scan
        stopScan()

        _uiState.update { it.copy(isScanning = true, discoveredDevices = emptyList()) }

        scanJob = scope.launch {
            networkScanner.scanNetwork()
                .catch { e ->
                    PlatformLogger.e(TAG, "Scan error", e)
                }
                .onCompletion {
                    _uiState.update { it.copy(isScanning = false) }
                }
                .collect { device ->
                    _uiState.update { currentState ->
                        // Avoid duplicates
                        if (currentState.discoveredDevices.none { it.ipAddress == device.ipAddress }) {
                            currentState.copy(
                                discoveredDevices = currentState.discoveredDevices + device
                            )
                        } else {
                            currentState
                        }
                    }
                }
        }
    }

    /**
     * Stop any ongoing network scan.
     */
    fun stopScan() {
        networkScanner.cancelScan()
        scanJob?.cancel()
        scanJob = null
        _uiState.update { it.copy(isScanning = false) }
    }

    /**
     * Add a discovered device to the device list.
     */
    fun addDiscoveredDevice(device: DiscoveredDevice) {
        addDevice("${device.ipAddress}:${device.port}")
    }

    /**
     * Clear the list of discovered devices.
     */
    fun clearDiscoveredDevices() {
        _uiState.update { it.copy(discoveredDevices = emptyList()) }
    }
}
