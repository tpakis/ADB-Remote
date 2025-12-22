package com.example.adbremote.adb

import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

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

    data class AdbMessage(
        val command: Int,
        val arg0: Int,
        val arg1: Int,
        val data: ByteArray = ByteArray(0)
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
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
        val buffer = ByteBuffer.allocate(24 + data.size).order(ByteOrder.LITTLE_ENDIAN)

        buffer.putInt(command)
        buffer.putInt(arg0)
        buffer.putInt(arg1)
        buffer.putInt(data.size)
        buffer.putInt(checksum(data))
        buffer.putInt(command xor 0xffffffff.toInt())
        buffer.put(data)

        return buffer.array()
    }

    fun readMessage(input: InputStream): AdbMessage? {
        val header = ByteArray(24)
        var totalRead = 0

        while (totalRead < 24) {
            val read = input.read(header, totalRead, 24 - totalRead)
            if (read == -1) return null
            totalRead += read
        }

        val buffer = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN)
        val command = buffer.int
        val arg0 = buffer.int
        val arg1 = buffer.int
        val dataLength = buffer.int
        val dataChecksum = buffer.int
        val magic = buffer.int

        // Verify magic
        if (magic != (command xor 0xffffffff.toInt())) {
            throw IllegalStateException("Invalid magic in ADB message")
        }

        val data = if (dataLength > 0) {
            val dataArray = ByteArray(dataLength)
            totalRead = 0
            while (totalRead < dataLength) {
                val read = input.read(dataArray, totalRead, dataLength - totalRead)
                if (read == -1) throw IllegalStateException("Unexpected end of stream")
                totalRead += read
            }

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
