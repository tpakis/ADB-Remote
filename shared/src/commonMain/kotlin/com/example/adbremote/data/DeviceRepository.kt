package com.example.adbremote.data

import com.example.adbremote.model.AdbDevice
import com.example.adbremote.platform.PlatformLogger
import com.example.adbremote.platform.PlatformStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Repository for persisting device configuration and connection history
 */
class DeviceRepository(private val storage: PlatformStorage) {
    companion object {
        private const val TAG = "DeviceRepository"
        private const val KEY_DEVICES = "devices"
        private const val KEY_LAST_DEVICE_HOST = "last_device_host"
        private const val KEY_LAST_DEVICE_PORT = "last_device_port"
        private const val KEY_RECENT_COMMANDS = "recent_commands"
        private const val MAX_RECENT_COMMANDS = 10
    }

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Save the list of devices to persistent storage
     */
    suspend fun saveDevices(devices: List<AdbDevice>) = withContext(Dispatchers.Default) {
        try {
            // Only save non-connection state
            val devicesToSave = devices.map { it.copy(isConnected = false) }
            val jsonString = json.encodeToString(devicesToSave)
            storage.saveString(KEY_DEVICES, jsonString)
            PlatformLogger.d(TAG, "Saved ${devices.size} devices: ${devices.map { it.name }}")
        } catch (e: Exception) {
            PlatformLogger.e(TAG, "Failed to save devices", e)
        }
    }

    /**
     * Load the list of devices from persistent storage
     */
    suspend fun loadDevices(): List<AdbDevice> = withContext(Dispatchers.Default) {
        val devicesJson = storage.getString(KEY_DEVICES)
        if (devicesJson == null) {
            PlatformLogger.d(TAG, "No saved devices found")
            return@withContext emptyList()
        }

        try {
            val devices: List<AdbDevice> = json.decodeFromString(devicesJson)
            PlatformLogger.d(TAG, "Loaded ${devices.size} devices: ${devices.map { it.name }}")
            devices
        } catch (e: Exception) {
            PlatformLogger.e(TAG, "Failed to load devices", e)
            emptyList()
        }
    }

    /**
     * Save the last connected device for quick reconnection
     */
    suspend fun saveLastConnectedDevice(device: AdbDevice) = withContext(Dispatchers.Default) {
        storage.saveString(KEY_LAST_DEVICE_HOST, device.host)
        storage.saveString(KEY_LAST_DEVICE_PORT, device.port.toString())
    }

    /**
     * Get the last connected device info (host and port)
     */
    suspend fun getLastConnectedDevice(): Pair<String, Int>? = withContext(Dispatchers.Default) {
        val host = storage.getString(KEY_LAST_DEVICE_HOST)
        val portStr = storage.getString(KEY_LAST_DEVICE_PORT)
        val port = portStr?.toIntOrNull()

        if (host != null && port != null) {
            Pair(host, port)
        } else {
            null
        }
    }

    /**
     * Save a successful command to recent commands list
     */
    suspend fun addRecentCommand(command: String) = withContext(Dispatchers.Default) {
        try {
            val recentCommands = loadRecentCommands().toMutableList()

            // Remove if already exists (to move it to front)
            recentCommands.remove(command)

            // Add to front
            recentCommands.add(0, command)

            // Keep only last 10
            val trimmedCommands = recentCommands.take(MAX_RECENT_COMMANDS)

            // Save
            val jsonString = json.encodeToString(trimmedCommands)
            storage.saveString(KEY_RECENT_COMMANDS, jsonString)
        } catch (e: Exception) {
            PlatformLogger.e(TAG, "Failed to save recent command", e)
        }
    }

    /**
     * Load recent commands list
     */
    suspend fun loadRecentCommands(): List<String> = withContext(Dispatchers.Default) {
        val commandsJson = storage.getString(KEY_RECENT_COMMANDS) ?: return@withContext emptyList()

        try {
            json.decodeFromString<List<String>>(commandsJson)
        } catch (e: Exception) {
            PlatformLogger.e(TAG, "Failed to load recent commands", e)
            emptyList()
        }
    }

    /**
     * Clear all saved data
     */
    suspend fun clearAll() = withContext(Dispatchers.Default) {
        storage.clear()
    }
}
