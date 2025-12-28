package com.example.adbremote.platform

/**
 * Android stub implementation for SystemAdb.
 * System ADB integration is only available on Desktop.
 */
actual class SystemAdb actual constructor() {

    actual fun isAvailable(): Boolean = false

    actual suspend fun getConnectedDevices(): List<SystemAdbDevice> = emptyList()

    actual suspend fun executeCommand(serial: String, command: String): Result<String> =
        Result.failure(Exception("System ADB is not available on Android"))

    actual suspend fun executeCommandBinary(serial: String, command: String): Result<ByteArray> =
        Result.failure(Exception("System ADB is not available on Android"))

    actual suspend fun installApk(serial: String, apkPath: String): Result<String> =
        Result.failure(Exception("System ADB is not available on Android"))
}
