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
    }

    /**
     * Load the list of devices from persistent storage
     */
    suspend fun loadDevices(): List<AdbDevice> = withContext(Dispatchers.IO) {
        val devicesJson = prefs.getString(KEY_DEVICES, null) ?: return@withContext emptyList()

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

            devices
        } catch (e: Exception) {
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
     * Clear all saved data
     */
    suspend fun clearAll() = withContext(Dispatchers.IO) {
        prefs.edit().clear().apply()
    }
}
