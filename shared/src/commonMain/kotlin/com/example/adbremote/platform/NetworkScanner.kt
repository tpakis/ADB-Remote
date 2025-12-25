package com.example.adbremote.platform

import kotlinx.coroutines.flow.Flow

/**
 * Data class representing a discovered device on the network.
 */
data class DiscoveredDevice(
    val ipAddress: String,
    val port: Int,
    val hostname: String? = null
)

/**
 * Platform-specific network scanner to find ADB-enabled devices.
 */
expect class NetworkScanner() {
    /**
     * Scan the local network for devices with ADB port open.
     * Emits discovered devices as they are found.
     *
     * @param port The port to scan for (default 5555 for ADB over TCP)
     * @param timeoutMs Connection timeout per host in milliseconds
     * @return Flow of discovered devices
     */
    fun scanNetwork(port: Int = 5555, timeoutMs: Int = 200): Flow<DiscoveredDevice>

    /**
     * Get the local IP address to determine the network range to scan.
     */
    suspend fun getLocalIpAddress(): String?

    /**
     * Cancel any ongoing scan.
     */
    fun cancelScan()
}
