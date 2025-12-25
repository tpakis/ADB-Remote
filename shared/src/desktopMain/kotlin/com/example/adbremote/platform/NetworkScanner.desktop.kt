package com.example.adbremote.platform

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.net.Inet4Address
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.Socket
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.coroutineContext

actual class NetworkScanner actual constructor() {
    private val isCancelled = AtomicBoolean(false)

    actual fun scanNetwork(port: Int, timeoutMs: Int): Flow<DiscoveredDevice> = flow {
        isCancelled.set(false)

        val localIp = getLocalIpAddress() ?: return@flow
        val subnet = localIp.substringBeforeLast(".")

        println("NetworkScanner: Scanning subnet: $subnet.* for port $port")

        // Scan IPs 1-254 in the subnet
        for (i in 1..254) {
            if (isCancelled.get() || !coroutineContext.isActive) {
                println("NetworkScanner: Scan cancelled")
                break
            }

            val ip = "$subnet.$i"

            // Skip our own IP
            if (ip == localIp) continue

            try {
                val socket = Socket()
                socket.connect(InetSocketAddress(ip, port), timeoutMs)
                socket.close()

                // Found a device with ADB port open
                println("NetworkScanner: Found device at $ip:$port")
                emit(DiscoveredDevice(ipAddress = ip, port = port))
            } catch (e: Exception) {
                // Connection failed - device not available or no ADB
            }
        }

        println("NetworkScanner: Scan complete")
    }.flowOn(Dispatchers.IO)

    actual suspend fun getLocalIpAddress(): String? = withContext(Dispatchers.IO) {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()

                // Skip loopback and down interfaces
                if (networkInterface.isLoopback || !networkInterface.isUp) continue

                val addresses = networkInterface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val address = addresses.nextElement()

                    // Only interested in IPv4 addresses
                    if (address is Inet4Address && !address.isLoopbackAddress) {
                        val ip = address.hostAddress
                        // Prefer typical private network addresses
                        if (ip != null && (ip.startsWith("192.168.") || ip.startsWith("10.") || ip.startsWith("172."))) {
                            println("NetworkScanner: Local IP: $ip")
                            return@withContext ip
                        }
                    }
                }
            }
        } catch (e: Exception) {
            println("NetworkScanner: Failed to get local IP: ${e.message}")
        }
        null
    }

    actual fun cancelScan() {
        isCancelled.set(true)
    }
}
