package com.example.adbremote.data

import android.content.Context
import android.content.SharedPreferences
import com.example.adbremote.model.AdbDevice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * Repository for persisting device configuration and connection history
 */
class DeviceRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "adb_remote_prefs"
        private const val KEY_DEVICES = "devices"
        private const val KEY_LAST_DEVICE_HOST = "last_device_host"
        private const val KEY_LAST_DEVICE_PORT = "last_device_port"
        private const val KEY_RECENT_COMMANDS = "recent_commands"
        private const val MAX_RECENT_COMMANDS = 10
    }

    /**
     * Save the list of devices to persistent storage
     */
    suspend fun saveDevices(devices: List<AdbDevice>) = withContext(Dispatchers.IO) {
        val jsonArray = JSONArray()
        devices.forEach { device ->
            val jsonObject = JSONObject().apply {
                put("name", device.name)
                put("host", device.host)
                put("port", device.port)
                put("isEmulator", device.isEmulator)
            }
            jsonArray.put(jsonObject)
        }

        prefs.edit().putString(KEY_DEVICES, jsonArray.toString()).apply()
        android.util.Log.d("DeviceRepository", "Saved ${devices.size} devices: ${devices.map { it.name }}")
    }

    /**
     * Load the list of devices from persistent storage
     */
    suspend fun loadDevices(): List<AdbDevice> = withContext(Dispatchers.IO) {
        val devicesJson = prefs.getString(KEY_DEVICES, null)
        if (devicesJson == null) {
            android.util.Log.d("DeviceRepository", "No saved devices found")
            return@withContext emptyList()
        }

        try {
            val jsonArray = JSONArray(devicesJson)
            val devices = mutableListOf<AdbDevice>()

            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                val device = AdbDevice(
                    name = jsonObject.getString("name"),
                    host = jsonObject.getString("host"),
                    port = jsonObject.getInt("port"),
                    isConnected = false,
                    isEmulator = jsonObject.optBoolean("isEmulator", false)
                )
                devices.add(device)
            }

            android.util.Log.d("DeviceRepository", "Loaded ${devices.size} devices: ${devices.map { it.name }}")
            devices
        } catch (e: Exception) {
            android.util.Log.e("DeviceRepository", "Failed to load devices", e)
            emptyList()
        }
    }

    /**
     * Save the last connected device for quick reconnection
     */
    suspend fun saveLastConnectedDevice(device: AdbDevice) = withContext(Dispatchers.IO) {
        prefs.edit()
            .putString(KEY_LAST_DEVICE_HOST, device.host)
            .putInt(KEY_LAST_DEVICE_PORT, device.port)
            .apply()
    }

    /**
     * Get the last connected device info (host and port)
     */
    suspend fun getLastConnectedDevice(): Pair<String, Int>? = withContext(Dispatchers.IO) {
        val host = prefs.getString(KEY_LAST_DEVICE_HOST, null)
        val port = prefs.getInt(KEY_LAST_DEVICE_PORT, -1)

        if (host != null && port != -1) {
            Pair(host, port)
        } else {
            null
        }
    }

    /**
     * Save a successful command to recent commands list
     * Keeps the last 10 unique commands, most recent first
     */
    suspend fun addRecentCommand(command: String) = withContext(Dispatchers.IO) {
        val recentCommands = loadRecentCommands().toMutableList()

        // Remove if already exists (to move it to front)
        recentCommands.remove(command)

        // Add to front
        recentCommands.add(0, command)

        // Keep only last 10
        val trimmedCommands = recentCommands.take(MAX_RECENT_COMMANDS)

        // Save
        val jsonArray = JSONArray()
        trimmedCommands.forEach { jsonArray.put(it) }
        prefs.edit().putString(KEY_RECENT_COMMANDS, jsonArray.toString()).apply()
    }

    /**
     * Load recent commands list
     */
    suspend fun loadRecentCommands(): List<String> = withContext(Dispatchers.IO) {
        val commandsJson = prefs.getString(KEY_RECENT_COMMANDS, null) ?: return@withContext emptyList()

        try {
            val jsonArray = JSONArray(commandsJson)
            val commands = mutableListOf<String>()

            for (i in 0 until jsonArray.length()) {
                commands.add(jsonArray.getString(i))
            }

            commands
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Clear all saved data
     */
    suspend fun clearAll() = withContext(Dispatchers.IO) {
        prefs.edit().clear().apply()
    }
}
