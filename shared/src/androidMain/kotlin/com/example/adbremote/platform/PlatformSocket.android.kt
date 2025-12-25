package com.example.adbremote.platform

import java.net.InetSocketAddress
import java.net.Socket

actual class PlatformSocket actual constructor() {
    private var socket: Socket? = null
    private var inputStream: java.io.InputStream? = null
    private var outputStream: java.io.OutputStream? = null

    actual suspend fun connect(host: String, port: Int, timeoutMs: Int) {
        val newSocket = Socket()
        newSocket.connect(InetSocketAddress(host, port), timeoutMs)
        socket = newSocket
        inputStream = newSocket.getInputStream()
        outputStream = newSocket.getOutputStream()
    }

    actual fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        return inputStream?.read(buffer, offset, length) ?: -1
    }

    actual fun readFully(buffer: ByteArray, offset: Int, length: Int) {
        var totalRead = 0
        while (totalRead < length) {
            val read = inputStream?.read(buffer, offset + totalRead, length - totalRead) ?: -1
            if (read == -1) throw java.io.IOException("Unexpected end of stream")
            totalRead += read
        }
    }

    actual fun write(data: ByteArray) {
        outputStream?.write(data)
    }

    actual fun writeAndFlush(data: ByteArray) {
        outputStream?.write(data)
        outputStream?.flush()
    }

    actual fun close() {
        try {
            inputStream?.close()
        } catch (_: Exception) {}
        try {
            outputStream?.close()
        } catch (_: Exception) {}
        try {
            socket?.close()
        } catch (_: Exception) {}
        socket = null
        inputStream = null
        outputStream = null
    }

    actual fun isConnected(): Boolean {
        return socket?.isConnected == true
    }

    actual fun isClosed(): Boolean {
        return socket?.isClosed == true
    }

    actual fun checkConnectionAlive(): Boolean {
        val s = socket ?: return false
        return try {
            s.sendUrgentData(0)
            true
        } catch (e: Exception) {
            false
        }
    }

    actual fun available(): Int {
        return inputStream?.available() ?: 0
    }

    actual fun setSoTimeout(timeoutMs: Int) {
        socket?.soTimeout = timeoutMs
    }
}
