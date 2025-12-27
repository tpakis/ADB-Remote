package com.example.adbremote.platform

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import platform.Foundation.NSLog

/**
 * iOS implementation of NetworkScanner.
 * Note: Network scanning on iOS is limited due to sandbox restrictions.
 * This is a stub implementation - network scanning is not available on iOS.
 * Users can manually add devices by IP address.
 */
actual class NetworkScanner actual constructor() {
    private var isCancelled = false

    actual fun scanNetwork(port: Int, timeoutMs: Int): Flow<DiscoveredDevice> {
        NSLog("NetworkScanner: Network scanning is not available on iOS")
        return emptyFlow()
    }

    actual suspend fun getLocalIpAddress(): String? {
        // Network interface enumeration is complex on iOS with Kotlin/Native
        // Return null to indicate scanning is not available
        return null
    }

    actual fun cancelScan() {
        isCancelled = true
    }
}
