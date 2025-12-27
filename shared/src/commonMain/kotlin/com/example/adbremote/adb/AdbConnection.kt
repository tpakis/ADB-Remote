package com.example.adbremote.adb

import com.example.adbremote.platform.PlatformLogger
import com.example.adbremote.platform.PlatformSocket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Manages an ADB connection to a remote device
 */
class AdbConnection(
    private val host: String,
    private val port: Int = 5555,
    private val keyManager: AdbKeyManager? = null
) {
    private var socket: PlatformSocket? = null
    private var connected = false
    private var localId = 1

    companion object {
        private const val TAG = "AdbConnection"
        private const val CONNECT_TIMEOUT = 5000
        // Banner with features - matches desktop adb format more closely
        private const val SYSTEM_IDENTITY = "host::features=shell_v2,cmd,stat_v2,ls_v2,fixed_push_mkdir,apex,abb,fixed_push_symlink_timestamp,abb_exec,remount_shell,track_app,sendrecv_v2,sendrecv_v2_brotli,sendrecv_v2_lz4,sendrecv_v2_zstd,sendrecv_v2_dry_run_send,openscreen_mdns\u0000"
    }

    suspend fun connect(): Result<Unit> = withContext(Dispatchers.Default) {
        try {
            PlatformLogger.i(TAG, "=== Starting ADB Connection to $host:$port ===")
            PlatformLogger.i(TAG, "Protocol version: 0x${AdbProtocol.A_VERSION.toString(16)}")
            PlatformLogger.i(TAG, "Max payload: ${AdbProtocol.MAX_PAYLOAD}")

            val newSocket = PlatformSocket()
            newSocket.connect(host, port, CONNECT_TIMEOUT)
            socket = newSocket
            PlatformLogger.i(TAG, "TCP socket connected")

            // Send CNXN message
            val systemIdentity = SYSTEM_IDENTITY.encodeToByteArray()
            PlatformLogger.i(TAG, "Sending CNXN with banner (${systemIdentity.size} bytes)")

            val cnxnMessage = AdbProtocol.createMessage(
                AdbProtocol.A_CNXN,
                AdbProtocol.A_VERSION,
                AdbProtocol.MAX_PAYLOAD,
                systemIdentity
            )
            PlatformLogger.i(TAG, "CNXN message total size: ${cnxnMessage.size} bytes")

            newSocket.writeAndFlush(cnxnMessage)
            PlatformLogger.i(TAG, "CNXN sent, waiting for response...")

            // Read response
            val response = AdbProtocol.readMessage(newSocket)
                ?: return@withContext Result.failure(Exception("No response from device"))

            PlatformLogger.i(TAG, "Received response: ${AdbProtocol.commandToString(response.command)}, arg0=${response.arg0}, arg1=${response.arg1}")

            when (response.command) {
                AdbProtocol.A_CNXN -> {
                    PlatformLogger.i(TAG, "Connection established WITHOUT authentication")
                    val deviceBanner = response.data.decodeToString()
                    PlatformLogger.i(TAG, "Device banner: $deviceBanner")
                    connected = true
                    Result.success(Unit)
                }
                AdbProtocol.A_AUTH -> {
                    PlatformLogger.i(TAG, "Authentication required")
                    handleAuthentication(newSocket, response)
                }
                else -> {
                    PlatformLogger.e(TAG, "Unexpected response: ${AdbProtocol.commandToString(response.command)}")
                    Result.failure(Exception("Unexpected response: ${AdbProtocol.commandToString(response.command)}"))
                }
            }
        } catch (e: Exception) {
            PlatformLogger.e(TAG, "Connection failed", e)
            Result.failure(e)
        }
    }

    suspend fun executeCommand(command: String): Result<String> = withContext(Dispatchers.Default) {
        if (!connected || socket == null) {
            return@withContext Result.failure(Exception("Not connected"))
        }

        try {
            val currentSocket = socket ?: return@withContext Result.failure(Exception("Socket is null"))

            // Check if there are any lingering messages and drain them
            while (currentSocket.available() > 0) {
                val lingering = AdbProtocol.readMessage(currentSocket)
                PlatformLogger.w(TAG, "Draining lingering message: ${AdbProtocol.commandToString(lingering?.command ?: 0)}")
            }

            // Open a shell service
            val destination = "shell:$command\u0000".encodeToByteArray()
            val currentLocalId = localId++

            PlatformLogger.d(TAG, "Opening stream with localId=$currentLocalId for command: $command")

            val openMessage = AdbProtocol.createMessage(
                AdbProtocol.A_OPEN,
                currentLocalId,
                0,
                destination
            )

            currentSocket.writeAndFlush(openMessage)

            // Wait for OKAY response
            val openResponse = AdbProtocol.readMessage(currentSocket)

            PlatformLogger.d(TAG, "Received response to OPEN: ${AdbProtocol.commandToString(openResponse?.command ?: 0)}")

            if (openResponse?.command != AdbProtocol.A_OKAY) {
                return@withContext Result.failure(Exception(
                    "Failed to open shell: ${AdbProtocol.commandToString(openResponse?.command ?: 0)}"
                ))
            }

            val remoteId = openResponse.arg0
            val resultBuilder = StringBuilder()

            // Read command output
            while (true) {
                val message = AdbProtocol.readMessage(currentSocket) ?: break

                when (message.command) {
                    AdbProtocol.A_WRTE -> {
                        resultBuilder.append(message.data.decodeToString())

                        // Send OKAY to acknowledge
                        val okayMessage = AdbProtocol.createMessage(
                            AdbProtocol.A_OKAY,
                            currentLocalId,
                            remoteId
                        )
                        currentSocket.writeAndFlush(okayMessage)
                    }
                    AdbProtocol.A_CLSE -> {
                        // Stream closed by server, acknowledge it
                        val closeMessage = AdbProtocol.createMessage(
                            AdbProtocol.A_CLSE,
                            currentLocalId,
                            remoteId
                        )
                        currentSocket.writeAndFlush(closeMessage)

                        PlatformLogger.d(TAG, "Stream $currentLocalId closed")
                        break
                    }
                    else -> {
                        PlatformLogger.w(TAG, "Unexpected message: ${AdbProtocol.commandToString(message.command)}")
                    }
                }
            }

            Result.success(resultBuilder.toString())
        } catch (e: Exception) {
            PlatformLogger.e(TAG, "Command execution failed", e)
            Result.failure(e)
        }
    }

    private suspend fun handleAuthentication(
        socket: PlatformSocket,
        initialAuthResponse: AdbProtocol.AdbMessage
    ): Result<Unit> = withContext(Dispatchers.Default) {
        try {
            if (keyManager == null) {
                return@withContext Result.failure(Exception(
                    "Authentication required but no key manager provided."
                ))
            }

            var authResponse = initialAuthResponse

            if (authResponse.command != AdbProtocol.A_AUTH || authResponse.arg0 != AdbProtocol.ADB_AUTH_TOKEN) {
                PlatformLogger.e(TAG, "Expected AUTH TOKEN")
                return@withContext Result.failure(Exception("Expected AUTH TOKEN message"))
            }

            val token = authResponse.data
            PlatformLogger.i(TAG, "=== Starting ADB Authentication ===")
            PlatformLogger.i(TAG, "Token received: ${token.size} bytes")

            // Sign the token with our private key
            val signature = keyManager.signToken(token)
            if (signature == null) {
                PlatformLogger.e(TAG, "Failed to sign token")
                return@withContext Result.failure(Exception("Failed to sign authentication token"))
            }

            PlatformLogger.i(TAG, "Signature generated: ${signature.size} bytes")
            val signatureMessage = AdbProtocol.createMessage(
                AdbProtocol.A_AUTH,
                AdbProtocol.ADB_AUTH_SIGNATURE,
                0,
                signature
            )
            socket.writeAndFlush(signatureMessage)
            PlatformLogger.i(TAG, "AUTH SIGNATURE sent")

            // Read response
            authResponse = AdbProtocol.readMessage(socket)
                ?: return@withContext Result.failure(Exception("No response after sending signature"))

            PlatformLogger.i(TAG, "Response: ${AdbProtocol.commandToString(authResponse.command)}")

            when (authResponse.command) {
                AdbProtocol.A_CNXN -> {
                    PlatformLogger.i(TAG, "★ SUCCESS: Signature recognized!")
                    connected = true
                    return@withContext Result.success(Unit)
                }
                AdbProtocol.A_AUTH -> {
                    // Server doesn't have our public key, send it
                    PlatformLogger.w(TAG, "Signature not recognized, sending public key")

                    val publicKey = keyManager.getPublicKeyForAdb()
                    PlatformLogger.i(TAG, "Public key size: ${publicKey.size} bytes")
                    val publicKeyMessage = AdbProtocol.createMessage(
                        AdbProtocol.A_AUTH,
                        AdbProtocol.ADB_AUTH_RSAPUBLICKEY,
                        0,
                        publicKey
                    )
                    socket.writeAndFlush(publicKeyMessage)
                    PlatformLogger.i(TAG, "AUTH RSAPUBLICKEY sent, waiting for user...")

                    // Read final response
                    val finalResponse = AdbProtocol.readMessage(socket)
                        ?: return@withContext Result.failure(Exception("No response after sending public key"))

                    PlatformLogger.i(TAG, "Final response: ${AdbProtocol.commandToString(finalResponse.command)}")

                    if (finalResponse.command == AdbProtocol.A_CNXN) {
                        PlatformLogger.i(TAG, "✓ Authentication successful")
                        connected = true
                        return@withContext Result.success(Unit)
                    } else {
                        PlatformLogger.e(TAG, "✗ Authentication rejected")
                        return@withContext Result.failure(Exception(
                            "Authentication rejected by device."
                        ))
                    }
                }
                else -> {
                    PlatformLogger.e(TAG, "Unexpected response: ${AdbProtocol.commandToString(authResponse.command)}")
                    return@withContext Result.failure(Exception(
                        "Unexpected response during authentication"
                    ))
                }
            }
        } catch (e: Exception) {
            PlatformLogger.e(TAG, "Authentication failed", e)
            return@withContext Result.failure(e)
        }
    }

    fun disconnect() {
        try {
            socket?.close()
        } catch (e: Exception) {
            PlatformLogger.e(TAG, "Error closing socket", e)
        } finally {
            socket = null
            connected = false
        }
    }

    /**
     * Check if the connection is actually alive.
     */
    fun isConnected(): Boolean {
        if (!connected) return false

        val currentSocket = socket ?: return false

        if (currentSocket.isClosed() || !currentSocket.isConnected()) {
            PlatformLogger.w(TAG, "Socket is closed, resetting connection state")
            connected = false
            return false
        }

        // Try to detect dead socket
        if (!currentSocket.checkConnectionAlive()) {
            PlatformLogger.w(TAG, "Socket appears dead, resetting connection state")
            connected = false
            socket = null
            return false
        }

        return true
    }

    /**
     * Check if socket is usable without sending data
     */
    fun checkConnection(): Boolean {
        if (!connected) return false

        val currentSocket = socket ?: return false

        if (currentSocket.isClosed() || !currentSocket.isConnected()) {
            PlatformLogger.w(TAG, "Socket is closed, marking as disconnected")
            connected = false
            socket = null
            return false
        }

        return true
    }
}
