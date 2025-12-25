package com.example.adbremote.platform

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import platform.Foundation.NSLog
import platform.posix.*
import kotlinx.cinterop.*
import kotlin.coroutines.coroutineContext

actual class NetworkScanner actual constructor() {
    private var isCancelled = false

    @OptIn(ExperimentalForeignApi::class)
    actual fun scanNetwork(port: Int, timeoutMs: Int): Flow<DiscoveredDevice> = flow {
        isCancelled = false

        val localIp = getLocalIpAddress() ?: return@flow
        val subnet = localIp.substringBeforeLast(".")

        NSLog("NetworkScanner: Scanning subnet: $subnet.* for port $port")

        // Scan IPs 1-254 in the subnet
        for (i in 1..254) {
            if (isCancelled || !coroutineContext.isActive) {
                NSLog("NetworkScanner: Scan cancelled")
                break
            }

            val ip = "$subnet.$i"

            // Skip our own IP
            if (ip == localIp) continue

            if (checkPort(ip, port, timeoutMs)) {
                NSLog("NetworkScanner: Found device at $ip:$port")
                emit(DiscoveredDevice(ipAddress = ip, port = port))
            }
        }

        NSLog("NetworkScanner: Scan complete")
    }.flowOn(Dispatchers.Default)

    @OptIn(ExperimentalForeignApi::class)
    private fun checkPort(host: String, port: Int, timeoutMs: Int): Boolean {
        memScoped {
            val sockfd = socket(AF_INET, SOCK_STREAM, 0)
            if (sockfd < 0) return false

            try {
                // Set non-blocking
                val flags = fcntl(sockfd, F_GETFL, 0)
                fcntl(sockfd, F_SETFL, flags or O_NONBLOCK)

                val addr = alloc<sockaddr_in>()
                addr.sin_family = AF_INET.toUShort()
                addr.sin_port = htons(port.toUShort())

                inet_pton(AF_INET, host, addr.sin_addr.ptr)

                val result = connect(sockfd, addr.ptr.reinterpret(), sizeOf<sockaddr_in>().toUInt())

                if (result < 0 && errno != EINPROGRESS) {
                    return false
                }

                // Wait for connection with timeout
                val fdSet = alloc<fd_set>()
                posix_FD_ZERO(fdSet.ptr)
                posix_FD_SET(sockfd, fdSet.ptr)

                val timeout = alloc<timeval>()
                timeout.tv_sec = (timeoutMs / 1000).toLong()
                timeout.tv_usec = ((timeoutMs % 1000) * 1000).toLong()

                val selectResult = select(sockfd + 1, null, fdSet.ptr, null, timeout.ptr)

                if (selectResult > 0 && posix_FD_ISSET(sockfd, fdSet.ptr) != 0) {
                    // Check if connection actually succeeded
                    val error = alloc<IntVar>()
                    val len = alloc<UIntVar>()
                    len.value = sizeOf<IntVar>().toUInt()
                    getsockopt(sockfd, SOL_SOCKET, SO_ERROR, error.ptr, len.ptr)
                    return error.value == 0
                }

                return false
            } finally {
                close(sockfd)
            }
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    actual suspend fun getLocalIpAddress(): String? = withContext(Dispatchers.Default) {
        memScoped {
            val ifaddrs = alloc<CPointerVar<ifaddrs>>()

            if (getifaddrs(ifaddrs.ptr) != 0) {
                return@withContext null
            }

            var result: String? = null
            var current = ifaddrs.value

            while (current != null) {
                val ifa = current.pointed
                val family = ifa.ifa_addr?.pointed?.sa_family?.toInt()

                if (family == AF_INET) {
                    val name = ifa.ifa_name?.toKString()
                    // Prefer en0 (WiFi) or similar interfaces
                    if (name != null && (name.startsWith("en") || name.startsWith("wlan"))) {
                        val addr = ifa.ifa_addr?.reinterpret<sockaddr_in>()?.pointed
                        if (addr != null) {
                            val buffer = ByteArray(INET_ADDRSTRLEN)
                            buffer.usePinned { pinned ->
                                inet_ntop(AF_INET, addr.sin_addr.ptr, pinned.addressOf(0), INET_ADDRSTRLEN.toUInt())
                            }
                            val ip = buffer.toKString()
                            if (ip.startsWith("192.168.") || ip.startsWith("10.") || ip.startsWith("172.")) {
                                result = ip
                                break
                            }
                        }
                    }
                }

                current = ifa.ifa_next
            }

            freeifaddrs(ifaddrs.value)
            result
        }
    }

    actual fun cancelScan() {
        isCancelled = true
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun posix_FD_ZERO(set: CPointer<fd_set>) {
    memset(set, 0, sizeOf<fd_set>().toULong())
}

@OptIn(ExperimentalForeignApi::class)
private fun posix_FD_SET(fd: Int, set: CPointer<fd_set>) {
    val fdSet = set.pointed
    val index = fd / 32
    val bit = fd % 32
    fdSet.fds_bits[index] = fdSet.fds_bits[index] or (1 shl bit)
}

@OptIn(ExperimentalForeignApi::class)
private fun posix_FD_ISSET(fd: Int, set: CPointer<fd_set>): Int {
    val fdSet = set.pointed
    val index = fd / 32
    val bit = fd % 32
    return if ((fdSet.fds_bits[index] and (1 shl bit)) != 0) 1 else 0
}
