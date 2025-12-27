package com.example.adbremote.adb

import com.example.adbremote.platform.ByteBuffer
import com.example.adbremote.platform.PlatformSocket

/**
 * ADB Protocol constants and message handling
 * Based on the ADB protocol specification
 */
object AdbProtocol {
    // ADB Protocol Commands
    const val A_SYNC = 0x434e5953
    const val A_CNXN = 0x4e584e43
    const val A_AUTH = 0x48545541
    const val A_OPEN = 0x4e45504f
    const val A_OKAY = 0x59414b4f
    const val A_CLSE = 0x45534c43
    const val A_WRTE = 0x45545257

    // ADB Protocol Version
    const val A_VERSION = 0x01000000

    // Maximum data payload
    const val MAX_PAYLOAD = 4096

    // ADB Auth types
    const val ADB_AUTH_TOKEN = 1
    const val ADB_AUTH_SIGNATURE = 2
    const val ADB_AUTH_RSAPUBLICKEY = 3

    // ADB Sync Protocol Commands (as integers from ASCII)
    const val SYNC_SEND = 0x444E4553  // "SEND" in little-endian
    const val SYNC_DATA = 0x41544144  // "DATA" in little-endian
    const val SYNC_DONE = 0x454E4F44  // "DONE" in little-endian
    const val SYNC_OKAY = 0x59414B4F  // "OKAY" in little-endian
    const val SYNC_FAIL = 0x4C494146  // "FAIL" in little-endian

    // Max sync data chunk size
    const val SYNC_DATA_MAX = 65536

    data class AdbMessage(
        val command: Int,
        val arg0: Int,
        val arg1: Int,
        val data: ByteArray = ByteArray(0)
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false
            other as AdbMessage
            return command == other.command && arg0 == other.arg0 &&
                   arg1 == other.arg1 && data.contentEquals(other.data)
        }

        override fun hashCode(): Int {
            var result = command
            result = 31 * result + arg0
            result = 31 * result + arg1
            result = 31 * result + data.contentHashCode()
            return result
        }
    }

    fun createMessage(command: Int, arg0: Int, arg1: Int, data: ByteArray = ByteArray(0)): ByteArray {
        val buffer = ByteBuffer.allocate(24 + data.size)

        buffer.putInt(command)
        buffer.putInt(arg0)
        buffer.putInt(arg1)
        buffer.putInt(data.size)
        buffer.putInt(checksum(data))
        buffer.putInt(command xor 0xffffffff.toInt())
        buffer.put(data)

        return buffer.array()
    }

    fun readMessage(socket: PlatformSocket): AdbMessage? {
        val header = ByteArray(24)
        try {
            socket.readFully(header, 0, 24)
        } catch (e: Exception) {
            return null
        }

        val buffer = ByteBuffer.wrap(header)
        val command = buffer.getInt()
        val arg0 = buffer.getInt()
        val arg1 = buffer.getInt()
        val dataLength = buffer.getInt()
        val dataChecksum = buffer.getInt()
        val magic = buffer.getInt()

        // Verify magic
        if (magic != (command xor 0xffffffff.toInt())) {
            throw IllegalStateException("Invalid magic in ADB message")
        }

        val data = if (dataLength > 0) {
            val dataArray = ByteArray(dataLength)
            socket.readFully(dataArray, 0, dataLength)

            // Verify checksum
            if (checksum(dataArray) != dataChecksum) {
                throw IllegalStateException("Checksum mismatch")
            }
            dataArray
        } else {
            ByteArray(0)
        }

        return AdbMessage(command, arg0, arg1, data)
    }

    private fun checksum(data: ByteArray): Int {
        var sum = 0
        for (byte in data) {
            sum += byte.toInt() and 0xFF
        }
        return sum
    }

    fun commandToString(command: Int): String = when (command) {
        A_SYNC -> "SYNC"
        A_CNXN -> "CNXN"
        A_AUTH -> "AUTH"
        A_OPEN -> "OPEN"
        A_OKAY -> "OKAY"
        A_CLSE -> "CLSE"
        A_WRTE -> "WRTE"
        else -> "UNKNOWN(0x${command.toString(16)})"
    }
}
