package com.example.adbremote.platform

/**
 * Data class representing a device connected via the system's ADB server.
 */
data class SystemAdbDevice(
    val serial: String,      // e.g., "emulator-5554" or "R3CT1234567"
    val state: String,       // "device", "offline", "unauthorized"
    val model: String?,      // Device model if available (from "model:" field)
    val product: String?,    // Product name if available (from "product:" field)
    val device: String?      // Device codename if available (from "device:" field)
)

/**
 * Platform abstraction for interacting with the system's ADB server.
 * Only functional on Desktop where the adb command-line tool may be available.
 */
expect class SystemAdb() {
    /**
     * Check if the adb command is available in the system PATH.
     */
    fun isAvailable(): Boolean

    /**
     * Get list of devices connected via the system ADB server.
     * Runs "adb devices -l" and parses the output.
     */
    suspend fun getConnectedDevices(): List<SystemAdbDevice>

    /**
     * Execute a shell command on a specific device via system ADB.
     * Runs "adb -s <serial> shell <command>" and returns the output.
     */
    suspend fun executeCommand(serial: String, command: String): Result<String>

    /**
     * Execute a shell command and return binary output.
     * Used for commands like bugreport that return binary data.
     */
    suspend fun executeCommandBinary(serial: String, command: String): Result<ByteArray>

    /**
     * Install an APK file on the device.
     * Runs "adb -s <serial> install -r -t <apkPath>".
     */
    suspend fun installApk(serial: String, apkPath: String): Result<String>
}
