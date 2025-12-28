package com.example.adbremote.model

import kotlinx.serialization.Serializable

@Serializable
data class AdbDevice(
    val name: String,
    val displayName: String? = null,
    val host: String,
    val port: Int = 5555,
    val isConnected: Boolean = false,
    val isEmulator: Boolean = false
) {
    companion object {
        /**
         * Create a device from an emulator serial name (e.g., "emulator-5554")
         * Emulator serial format: emulator-XXXX where XXXX is the console port
         * ADB port is console port + 1
         *
         * Note: When connecting from an emulator to the host machine's emulators,
         * use the special syntax "emulator-5554@host" which will use 10.0.2.2
         */
        fun fromEmulatorSerial(serial: String, useHost: Boolean = false, displayName: String? = null): AdbDevice? {
            val emulatorPattern = Regex("emulator-(\\d+)(?:@host)?")
            val match = emulatorPattern.matchEntire(serial.trim())

            return if (match != null) {
                val consolePort = match.groupValues[1].toIntOrNull()
                val shouldUseHost = useHost || serial.contains("@host")

                if (consolePort != null) {
                    val adbPort = consolePort + 1
                    val host = if (shouldUseHost) "10.0.2.2" else "127.0.0.1"

                    AdbDevice(
                        name = if (shouldUseHost) "$serial (via host)" else serial,
                        displayName = displayName?.takeIf { it.isNotBlank() },
                        host = host,
                        port = adbPort,
                        isEmulator = true
                    )
                } else {
                    null
                }
            } else {
                null
            }
        }

        /**
         * Create a device from host:port string
         */
        fun fromHostPort(input: String, defaultPort: Int = 5555, displayName: String? = null): AdbDevice? {
            val parts = input.trim().split(":")
            return when (parts.size) {
                1 -> {
                    val host = parts[0].trim()
                    if (host.isNotEmpty()) {
                        AdbDevice(
                            name = "$host:$defaultPort",
                            displayName = displayName?.takeIf { it.isNotBlank() },
                            host = host,
                            port = defaultPort
                        )
                    } else {
                        null
                    }
                }
                2 -> {
                    val host = parts[0].trim()
                    val port = parts[1].trim().toIntOrNull() ?: defaultPort
                    if (host.isNotEmpty()) {
                        AdbDevice(
                            name = "$host:$port",
                            displayName = displayName?.takeIf { it.isNotBlank() },
                            host = host,
                            port = port
                        )
                    } else {
                        null
                    }
                }
                else -> null
            }
        }

        /**
         * Parse input and create appropriate device (emulator or IP-based)
         */
        fun fromInput(input: String, defaultPort: Int = 5555, displayName: String? = null): AdbDevice? {
            val trimmed = input.trim()

            // Check if it's an emulator serial
            if (trimmed.startsWith("emulator-")) {
                return fromEmulatorSerial(trimmed, displayName = displayName)
            }

            // Otherwise treat as host:port
            return fromHostPort(trimmed, defaultPort, displayName)
        }
    }
}

@Serializable
data class CommandResult(
    val command: String,
    val output: String,
    val isError: Boolean = false,
    val timestamp: Long = currentTimeMillis()
)

/**
 * Platform-independent way to get current time in milliseconds.
 */
expect fun currentTimeMillis(): Long
