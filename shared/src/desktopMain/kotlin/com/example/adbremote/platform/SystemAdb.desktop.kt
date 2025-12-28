package com.example.adbremote.platform

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

actual class SystemAdb actual constructor() {

    companion object {
        private const val TAG = "SystemAdb"
        private const val ADB_TIMEOUT_SECONDS = 30L
        private const val ADB_CHECK_TIMEOUT_SECONDS = 5L

        /**
         * Cached path to the adb executable, or null if not found.
         */
        private var cachedAdbPath: String? = null

        /**
         * Find the adb executable path.
         * Packaged macOS apps don't inherit shell PATH, so we search common locations.
         */
        private fun findAdbPath(): String? {
            // Return cached path if already found
            cachedAdbPath?.let { return it }

            val homeDir = System.getProperty("user.home")
            val androidHome = System.getenv("ANDROID_HOME")
            val androidSdkRoot = System.getenv("ANDROID_SDK_ROOT")

            // List of common adb locations to check
            val possiblePaths = buildList {
                // Environment variable paths (most reliable if set)
                androidHome?.let { add("$it/platform-tools/adb") }
                androidSdkRoot?.let { add("$it/platform-tools/adb") }

                // macOS default Android Studio SDK location
                add("$homeDir/Library/Android/sdk/platform-tools/adb")

                // Homebrew paths
                add("/opt/homebrew/bin/adb")  // Apple Silicon
                add("/usr/local/bin/adb")     // Intel Mac

                // Linux common paths
                add("$homeDir/Android/Sdk/platform-tools/adb")
                add("/usr/bin/adb")

                // Windows paths (for completeness)
                add("$homeDir/AppData/Local/Android/Sdk/platform-tools/adb.exe")
                System.getenv("LOCALAPPDATA")?.let {
                    add("$it/Android/Sdk/platform-tools/adb.exe")
                }
            }

            // Check each path
            for (path in possiblePaths) {
                val file = File(path)
                if (file.exists() && file.canExecute()) {
                    PlatformLogger.d(TAG, "Found adb at: $path")
                    cachedAdbPath = path
                    return path
                }
            }

            // Last resort: try bare "adb" command (works if in PATH)
            try {
                val process = ProcessBuilder("adb", "version")
                    .redirectErrorStream(true)
                    .start()
                val completed = process.waitFor(ADB_CHECK_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                if (completed && process.exitValue() == 0) {
                    PlatformLogger.d(TAG, "Found adb in PATH")
                    cachedAdbPath = "adb"
                    return "adb"
                }
            } catch (e: Exception) {
                // Ignore, adb not in PATH
            }

            PlatformLogger.w(TAG, "adb not found in any known location")
            return null
        }
    }

    /**
     * Check if the adb command is available.
     */
    actual fun isAvailable(): Boolean {
        return findAdbPath() != null
    }

    /**
     * Get list of devices connected via the system ADB server.
     * Parses output from "adb devices -l" command.
     *
     * Output format:
     * List of devices attached
     * emulator-5554          device product:sdk_gphone64_arm64 model:sdk_gphone64_arm64 device:emu64a transport_id:1
     * R3CT1234567            device usb:1-1 product:sargo model:Pixel_3a device:sargo transport_id:2
     */
    actual suspend fun getConnectedDevices(): List<SystemAdbDevice> = withContext(Dispatchers.IO) {
        try {
            val adbPath = findAdbPath() ?: return@withContext emptyList()
            val process = ProcessBuilder(adbPath, "devices", "-l")
                .redirectErrorStream(true)
                .start()

            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val lines = reader.readLines()
            reader.close()

            val completed = process.waitFor(ADB_CHECK_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            if (!completed) {
                process.destroyForcibly()
                PlatformLogger.w(TAG, "adb devices timed out")
                return@withContext emptyList()
            }

            // Parse the output, skip first line ("List of devices attached")
            val devices = mutableListOf<SystemAdbDevice>()
            for (line in lines.drop(1)) {
                val trimmed = line.trim()
                if (trimmed.isEmpty()) continue

                // Parse: serial state [additional info]
                // Example: "emulator-5554          device product:xxx model:xxx device:xxx"
                val parts = trimmed.split("\\s+".toRegex(), limit = 3)
                if (parts.size >= 2) {
                    val serial = parts[0]
                    val state = parts[1]

                    // Parse additional info (model, product, device)
                    val additionalInfo = if (parts.size > 2) parts[2] else ""
                    val model = extractField(additionalInfo, "model:")
                    val product = extractField(additionalInfo, "product:")
                    val device = extractField(additionalInfo, "device:")

                    devices.add(SystemAdbDevice(
                        serial = serial,
                        state = state,
                        model = model?.replace("_", " "),  // Make model more readable
                        product = product,
                        device = device
                    ))
                    PlatformLogger.d(TAG, "Found device: $serial ($state) - ${model ?: "unknown model"}")
                }
            }

            devices
        } catch (e: Exception) {
            PlatformLogger.e(TAG, "Failed to get connected devices", e)
            emptyList()
        }
    }

    /**
     * Execute a shell command on a specific device via system ADB.
     */
    actual suspend fun executeCommand(serial: String, command: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val adbPath = findAdbPath()
                ?: return@withContext Result.failure(Exception("adb not found"))
            PlatformLogger.d(TAG, "Executing on $serial: $command")

            val process = ProcessBuilder(adbPath, "-s", serial, "shell", command)
                .redirectErrorStream(true)
                .start()

            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val output = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                output.appendLine(line)
            }
            reader.close()

            val completed = process.waitFor(ADB_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            if (!completed) {
                process.destroyForcibly()
                return@withContext Result.failure(Exception("Command timed out after ${ADB_TIMEOUT_SECONDS}s"))
            }

            val exitCode = process.exitValue()
            val result = output.toString().trimEnd()

            if (exitCode == 0) {
                Result.success(result)
            } else {
                // ADB shell commands may return non-zero for command failures
                // but we still want to show the output
                Result.success(result)
            }
        } catch (e: Exception) {
            PlatformLogger.e(TAG, "Command execution failed", e)
            Result.failure(e)
        }
    }

    /**
     * Execute a command and return binary output.
     */
    actual suspend fun executeCommandBinary(serial: String, command: String): Result<ByteArray> = withContext(Dispatchers.IO) {
        try {
            val adbPath = findAdbPath()
                ?: return@withContext Result.failure(Exception("adb not found"))
            PlatformLogger.d(TAG, "Executing binary command on $serial: $command")

            val process = ProcessBuilder(adbPath, "-s", serial, "shell", command)
                .redirectErrorStream(false)
                .start()

            val inputStream = process.inputStream
            val bytes = inputStream.readBytes()
            inputStream.close()

            val completed = process.waitFor(ADB_TIMEOUT_SECONDS * 10, TimeUnit.SECONDS) // Longer timeout for binary
            if (!completed) {
                process.destroyForcibly()
                return@withContext Result.failure(Exception("Command timed out"))
            }

            Result.success(bytes)
        } catch (e: Exception) {
            PlatformLogger.e(TAG, "Binary command execution failed", e)
            Result.failure(e)
        }
    }

    /**
     * Install an APK file on the device using adb install command.
     */
    actual suspend fun installApk(serial: String, apkPath: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val adbPath = findAdbPath()
                ?: return@withContext Result.failure(Exception("adb not found"))
            PlatformLogger.d(TAG, "Installing APK on $serial: $apkPath")

            val process = ProcessBuilder(adbPath, "-s", serial, "install", "-r", "-t", apkPath)
                .redirectErrorStream(true)
                .start()

            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val output = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                output.appendLine(line)
            }
            reader.close()

            val completed = process.waitFor(ADB_TIMEOUT_SECONDS * 2, TimeUnit.SECONDS) // Longer timeout for install
            if (!completed) {
                process.destroyForcibly()
                return@withContext Result.failure(Exception("Install timed out"))
            }

            val result = output.toString().trimEnd()
            if (result.contains("Success")) {
                Result.success("APK installed successfully")
            } else {
                Result.failure(Exception("Install failed: $result"))
            }
        } catch (e: Exception) {
            PlatformLogger.e(TAG, "APK installation failed", e)
            Result.failure(e)
        }
    }

    /**
     * Extract a field value from the additional info string.
     * Example: "product:sargo model:Pixel_3a device:sargo" -> extractField(..., "model:") returns "Pixel_3a"
     */
    private fun extractField(info: String, fieldName: String): String? {
        val startIndex = info.indexOf(fieldName)
        if (startIndex == -1) return null

        val valueStart = startIndex + fieldName.length
        val valueEnd = info.indexOf(' ', valueStart).takeIf { it != -1 } ?: info.length
        return info.substring(valueStart, valueEnd).takeIf { it.isNotEmpty() }
    }
}
