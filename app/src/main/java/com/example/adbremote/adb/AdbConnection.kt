package com.example.adbremote.adb

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket

/**
 * Manages an ADB connection to a remote device
 */
class AdbConnection(
    private val host: String,
    private val port: Int = 5555
) {
    private var socket: Socket? = null
    private var isConnected = false
    private var localId = 1

    companion object {
        private const val TAG = "AdbConnection"
        private const val CONNECT_TIMEOUT = 5000
        private const val SYSTEM_IDENTITY = "host::\x00"
    }

    suspend fun connect(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Connecting to $host:$port")

            val newSocket = Socket()
            newSocket.connect(InetSocketAddress(host, port), CONNECT_TIMEOUT)
            socket = newSocket

            // Send CNXN message
            val systemIdentity = SYSTEM_IDENTITY.toByteArray()
            val cnxnMessage = AdbProtocol.createMessage(
                AdbProtocol.A_CNXN,
                AdbProtocol.A_VERSION,
                AdbProtocol.MAX_PAYLOAD,
                systemIdentity
            )

            newSocket.getOutputStream().write(cnxnMessage)
            newSocket.getOutputStream().flush()

            // Read response
            val response = AdbProtocol.readMessage(newSocket.getInputStream())

            when (response?.command) {
                AdbProtocol.A_CNXN -> {
                    Log.d(TAG, "Connection established")
                    isConnected = true
                    Result.success(Unit)
                }
                AdbProtocol.A_AUTH -> {
                    Log.w(TAG, "Authentication required - not yet implemented")
                    Result.failure(Exception("Device requires authentication. Please disable authentication on the target device by running 'adb tcpip 5555' from a USB connection first."))
                }
                else -> {
                    Result.failure(Exception("Unexpected response: ${AdbProtocol.commandToString(response?.command ?: 0)}"))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Connection failed", e)
            Result.failure(e)
        }
    }

    suspend fun executeCommand(command: String): Result<String> = withContext(Dispatchers.IO) {
        if (!isConnected || socket == null) {
            return@withContext Result.failure(IOException("Not connected"))
        }

        try {
            val currentSocket = socket ?: return@withContext Result.failure(IOException("Socket is null"))
            val output = currentSocket.getOutputStream()
            val input = currentSocket.getInputStream()

            // Open a shell service
            val destination = "shell:$command\x00".toByteArray()
            val currentLocalId = localId++

            val openMessage = AdbProtocol.createMessage(
                AdbProtocol.A_OPEN,
                currentLocalId,
                0,
                destination
            )

            output.write(openMessage)
            output.flush()

            // Wait for OKAY response
            val openResponse = AdbProtocol.readMessage(input)
            if (openResponse?.command != AdbProtocol.A_OKAY) {
                return@withContext Result.failure(IOException("Failed to open shell: ${AdbProtocol.commandToString(openResponse?.command ?: 0)}"))
            }

            val remoteId = openResponse.arg0
            val resultBuilder = StringBuilder()

            // Read command output
            while (true) {
                val message = AdbProtocol.readMessage(input) ?: break

                when (message.command) {
                    AdbProtocol.A_WRTE -> {
                        // Data from device
                        resultBuilder.append(String(message.data))

                        // Send OKAY to acknowledge
                        val okayMessage = AdbProtocol.createMessage(
                            AdbProtocol.A_OKAY,
                            currentLocalId,
                            remoteId
                        )
                        output.write(okayMessage)
                        output.flush()
                    }
                    AdbProtocol.A_CLSE -> {
                        // Stream closed
                        val closeMessage = AdbProtocol.createMessage(
                            AdbProtocol.A_CLSE,
                            currentLocalId,
                            remoteId
                        )
                        output.write(closeMessage)
                        output.flush()
                        break
                    }
                    else -> {
                        Log.w(TAG, "Unexpected message: ${AdbProtocol.commandToString(message.command)}")
                    }
                }
            }

            Result.success(resultBuilder.toString())
        } catch (e: Exception) {
            Log.e(TAG, "Command execution failed", e)
            Result.failure(e)
        }
    }

    fun disconnect() {
        try {
            socket?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error closing socket", e)
        } finally {
            socket = null
            isConnected = false
        }
    }

    fun isConnected(): Boolean = isConnected
}
