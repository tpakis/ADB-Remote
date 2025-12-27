package com.example.adbremote.platform

import kotlinx.cinterop.*
import platform.darwin.*
import platform.posix.*

/**
 * iOS implementation of PlatformSocket using BSD sockets via Kotlin/Native.
 */
@OptIn(ExperimentalForeignApi::class)
actual class PlatformSocket actual constructor() {
    private var socketFd: Int = -1

    actual suspend fun connect(host: String, port: Int, timeoutMs: Int) {
        memScoped {
            // Create socket
            socketFd = socket(AF_INET, SOCK_STREAM, 0)
            if (socketFd < 0) {
                throw Exception("Failed to create socket: ${strerror(errno)?.toKString()}")
            }

            // Set up address
            val serverAddr = alloc<sockaddr_in>()
            serverAddr.sin_family = AF_INET.toUByte()
            serverAddr.sin_port = swapBytes(port.toUShort())

            // Convert host to address using inet_pton
            val result = inet_pton(AF_INET, host, serverAddr.sin_addr.ptr)
            if (result != 1) {
                // Try to resolve hostname
                val hostent = gethostbyname(host)
                if (hostent == null) {
                    close(socketFd)
                    socketFd = -1
                    throw Exception("Failed to resolve host: $host")
                }
                val addrList = hostent.pointed.h_addr_list
                if (addrList == null || addrList[0] == null) {
                    close(socketFd)
                    socketFd = -1
                    throw Exception("No addresses found for host: $host")
                }
                memcpy(serverAddr.sin_addr.ptr, addrList[0], sizeOf<in_addr>().toULong())
            }

            // Set socket to non-blocking for timeout support
            val flags = fcntl(socketFd, F_GETFL, 0)
            fcntl(socketFd, F_SETFL, flags or O_NONBLOCK)

            // Connect
            val connectResult = connect(socketFd, serverAddr.ptr.reinterpret(), sizeOf<sockaddr_in>().toUInt())

            if (connectResult < 0 && errno != EINPROGRESS) {
                close(socketFd)
                socketFd = -1
                throw Exception("Failed to connect: ${strerror(errno)?.toKString()}")
            }

            // Wait for connection with timeout using select
            val fdSet = alloc<fd_set>()
            posix_FD_ZERO(fdSet.ptr)
            posix_FD_SET(socketFd, fdSet.ptr)

            val timeout = alloc<timeval>()
            timeout.tv_sec = (timeoutMs / 1000).toLong()
            timeout.tv_usec = ((timeoutMs % 1000) * 1000)

            val selectResult = select(socketFd + 1, null, fdSet.ptr, null, timeout.ptr)

            if (selectResult <= 0) {
                close(socketFd)
                socketFd = -1
                throw Exception("Connection timeout")
            }

            // Check for errors
            val error = alloc<IntVar>()
            val len = alloc<UIntVar>()
            len.value = sizeOf<IntVar>().toUInt()
            getsockopt(socketFd, SOL_SOCKET, SO_ERROR, error.ptr, len.ptr)

            if (error.value != 0) {
                close(socketFd)
                socketFd = -1
                throw Exception("Connection failed: ${strerror(error.value)?.toKString()}")
            }

            // Set socket back to blocking
            fcntl(socketFd, F_SETFL, flags)
        }
    }

    // Network byte order conversion (big-endian)
    private fun swapBytes(value: UShort): UShort {
        return (((value.toInt() and 0xFF) shl 8) or ((value.toInt() shr 8) and 0xFF)).toUShort()
    }

    actual fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (socketFd < 0) return -1

        return buffer.usePinned { pinned ->
            val result = recv(socketFd, pinned.addressOf(offset), length.convert(), 0)
            result.toInt()
        }
    }

    actual fun readFully(buffer: ByteArray, offset: Int, length: Int) {
        var totalRead = 0
        while (totalRead < length) {
            val read = read(buffer, offset + totalRead, length - totalRead)
            if (read <= 0) throw Exception("Unexpected end of stream")
            totalRead += read
        }
    }

    actual fun write(data: ByteArray) {
        if (socketFd < 0) return

        data.usePinned { pinned ->
            var totalSent = 0
            while (totalSent < data.size) {
                val sent = send(socketFd, pinned.addressOf(totalSent), (data.size - totalSent).convert(), 0)
                if (sent <= 0) throw Exception("Failed to write: ${strerror(errno)?.toKString()}")
                totalSent += sent.toInt()
            }
        }
    }

    actual fun writeAndFlush(data: ByteArray) {
        write(data)
        // TCP doesn't have a separate flush - data is sent immediately
    }

    actual fun close() {
        if (socketFd >= 0) {
            close(socketFd)
            socketFd = -1
        }
    }

    actual fun isConnected(): Boolean {
        return socketFd >= 0
    }

    actual fun isClosed(): Boolean {
        return socketFd < 0
    }

    actual fun checkConnectionAlive(): Boolean {
        if (socketFd < 0) return false

        // Check if socket is still alive by peeking
        memScoped {
            val buffer = alloc<platform.posix.int8_tVar>()
            val result = recv(socketFd, buffer.ptr, 1u, MSG_PEEK or MSG_DONTWAIT)
            // If result is 0, connection was closed
            // If result is -1 with EAGAIN/EWOULDBLOCK, connection is alive but no data
            // If result is > 0, connection is alive
            return when {
                result > 0 -> true
                result == 0L -> false
                else -> errno == EAGAIN || errno == EWOULDBLOCK
            }
        }
    }

    actual fun available(): Int {
        if (socketFd < 0) return 0

        memScoped {
            val available = alloc<IntVar>()
            ioctl(socketFd, FIONREAD.toULong(), available.ptr)
            return available.value
        }
    }

    actual fun setSoTimeout(timeoutMs: Int) {
        if (socketFd < 0) return

        memScoped {
            val timeout = alloc<timeval>()
            timeout.tv_sec = (timeoutMs / 1000).convert()
            timeout.tv_usec = ((timeoutMs % 1000) * 1000).convert()
            setsockopt(socketFd, SOL_SOCKET, SO_RCVTIMEO, timeout.ptr, sizeOf<timeval>().convert())
        }
    }
}

// Helper function for FD_ZERO
@OptIn(ExperimentalForeignApi::class)
private fun posix_FD_ZERO(set: CPointer<fd_set>) {
    memset(set, 0, sizeOf<fd_set>().convert())
}

// Helper function for FD_SET
@OptIn(ExperimentalForeignApi::class)
private fun posix_FD_SET(fd: Int, set: CPointer<fd_set>) {
    val fdSet = set.pointed
    val index = fd / 32
    val bit = fd % 32
    fdSet.fds_bits[index] = fdSet.fds_bits[index] or (1 shl bit)
}
