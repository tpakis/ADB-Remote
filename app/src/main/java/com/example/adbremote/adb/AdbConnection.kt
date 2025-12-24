package com.example.adbremote.adb

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket

/**
 * Manages an ADB connection to a remote device
 */
class AdbConnection(
    private val host: String,
    private val port: Int = 5555,
    private val keyManager: AdbKeyManager? = null
) {
    private var socket: Socket? = null
    private var isConnected = false
    private var localId = 1

    companion object {
        private const val TAG = "AdbConnection"
        private const val CONNECT_TIMEOUT = 5000
        // Banner with features - matches desktop adb format more closely
        // Format: "host::features=<feature_list>\0"
        private const val SYSTEM_IDENTITY = "host::features=shell_v2,cmd,stat_v2,ls_v2,fixed_push_mkdir,apex,abb,fixed_push_symlink_timestamp,abb_exec,remount_shell,track_app,sendrecv_v2,sendrecv_v2_brotli,sendrecv_v2_lz4,sendrecv_v2_zstd,sendrecv_v2_dry_run_send,openscreen_mdns\u0000"
    }

    suspend fun connect(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.i(TAG, "=== Starting ADB Connection to $host:$port ===")
            Log.i(TAG, "Protocol version: 0x${AdbProtocol.A_VERSION.toString(16)}")
            Log.i(TAG, "Max payload: ${AdbProtocol.MAX_PAYLOAD}")

            val newSocket = Socket()
            newSocket.connect(InetSocketAddress(host, port), CONNECT_TIMEOUT)
            socket = newSocket
            Log.i(TAG, "TCP socket connected")

            // Send CNXN message
            val systemIdentity = SYSTEM_IDENTITY.toByteArray(Charsets.UTF_8)
            Log.i(TAG, "Sending CNXN with banner (${systemIdentity.size} bytes): ${SYSTEM_IDENTITY.take(50)}...")

            val cnxnMessage = AdbProtocol.createMessage(
                AdbProtocol.A_CNXN,
                AdbProtocol.A_VERSION,
                AdbProtocol.MAX_PAYLOAD,
                systemIdentity
            )
            Log.i(TAG, "CNXN message total size: ${cnxnMessage.size} bytes")

            newSocket.getOutputStream().write(cnxnMessage)
            newSocket.getOutputStream().flush()
            Log.i(TAG, "CNXN sent, waiting for response...")

            // Read response
            val response = AdbProtocol.readMessage(newSocket.getInputStream())
                ?: return@withContext Result.failure(Exception("No response from device"))

            Log.i(TAG, "Received response: ${AdbProtocol.commandToString(response.command)}, arg0=${response.arg0}, arg1=${response.arg1}, data_size=${response.data.size}")

            when (response.command) {
                AdbProtocol.A_CNXN -> {
                    Log.i(TAG, "Connection established WITHOUT authentication (device trusts us)")
                    val deviceBanner = String(response.data, Charsets.UTF_8)
                    Log.i(TAG, "Device banner: $deviceBanner")
                    isConnected = true
                    Result.success(Unit)
                }
                AdbProtocol.A_AUTH -> {
                    Log.i(TAG, "Authentication required (arg0=${response.arg0}, expected TOKEN=${AdbProtocol.ADB_AUTH_TOKEN})")
                    handleAuthentication(newSocket, response)
                }
                else -> {
                    Log.e(TAG, "Unexpected response command: ${AdbProtocol.commandToString(response.command)}")
                    Result.failure(Exception("Unexpected response: ${AdbProtocol.commandToString(response.command)}"))
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

            // Check if there are any lingering messages and drain them
            // This can happen if previous stream had leftover CLSE messages
            while (input.available() > 0) {
                val lingering = AdbProtocol.readMessage(input)
                Log.w(TAG, "Draining lingering message before opening stream: ${AdbProtocol.commandToString(lingering?.command ?: 0)}")
            }

            // Open a shell service
            val destination = "shell:$command\u0000".toByteArray()
            val currentLocalId = localId++

            Log.d(TAG, "Opening stream with localId=$currentLocalId for command: $command")

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

            Log.d(TAG, "Received response to OPEN: ${AdbProtocol.commandToString(openResponse?.command ?: 0)}, " +
                      "arg0=${openResponse?.arg0}, arg1=${openResponse?.arg1}")

            if (openResponse?.command != AdbProtocol.A_OKAY) {
                return@withContext Result.failure(IOException(
                    "Failed to open shell: ${AdbProtocol.commandToString(openResponse?.command ?: 0)} " +
                    "(expected OKAY for localId=$currentLocalId)"
                ))
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
                        // Stream closed by server, acknowledge it
                        val closeMessage = AdbProtocol.createMessage(
                            AdbProtocol.A_CLSE,
                            currentLocalId,
                            remoteId
                        )
                        output.write(closeMessage)
                        output.flush()

                        Log.d(TAG, "Received CLSE from server for stream $currentLocalId, sent acknowledgment")

                        // Note: In ADB protocol, there's no separate acknowledgment to our CLSE
                        // The stream is now closed on both sides
                        // Just break and let the next command start fresh
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

    private suspend fun handleAuthentication(
        socket: Socket,
        initialAuthResponse: AdbProtocol.AdbMessage
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (keyManager == null) {
                return@withContext Result.failure(Exception(
                    "Authentication required but no key manager provided. " +
                    "Please disable authentication on the target device by running 'adb tcpip 5555' from a USB connection first."
                ))
            }

            val output = socket.getOutputStream()
            val input = socket.getInputStream()

            // First AUTH message from server contains the token (already read in connect())
            var authResponse = initialAuthResponse

            if (authResponse.command != AdbProtocol.A_AUTH || authResponse.arg0 != AdbProtocol.ADB_AUTH_TOKEN) {
                Log.e(TAG, "Expected AUTH TOKEN but got: cmd=${AdbProtocol.commandToString(authResponse.command)}, arg0=${authResponse.arg0}")
                return@withContext Result.failure(Exception("Expected AUTH TOKEN message"))
            }

            val token = authResponse.data
            Log.i(TAG, "=== Starting ADB Authentication ===")
            Log.i(TAG, "Token received: ${token.size} bytes")
            Log.i(TAG, "Token hex: ${token.joinToString("") { "%02x".format(it) }}")

            // Sign the token with our private key
            Log.i(TAG, "Signing token with our private key...")
            val signature = keyManager.signToken(token)
            if (signature == null) {
                Log.e(TAG, "Failed to sign token - keyManager.signToken returned null")
                return@withContext Result.failure(Exception("Failed to sign authentication token"))
            }

            Log.i(TAG, "Signature generated: ${signature.size} bytes")
            Log.i(TAG, "Sending AUTH SIGNATURE message...")
            val signatureMessage = AdbProtocol.createMessage(
                AdbProtocol.A_AUTH,
                AdbProtocol.ADB_AUTH_SIGNATURE,
                0,
                signature
            )
            output.write(signatureMessage)
            output.flush()
            Log.i(TAG, "AUTH SIGNATURE sent, waiting for response...")

            // Read response
            authResponse = AdbProtocol.readMessage(input)
                ?: return@withContext Result.failure(Exception("No response after sending signature"))

            Log.i(TAG, "Response after signature: ${AdbProtocol.commandToString(authResponse.command)}, arg0=${authResponse.arg0}, arg1=${authResponse.arg1}")

            when (authResponse.command) {
                AdbProtocol.A_CNXN -> {
                    Log.i(TAG, "★★★ SUCCESS: Device recognized our signature! ★★★")
                    Log.i(TAG, "'Always allow' is working - device has our key stored")
                    val deviceBanner = String(authResponse.data, Charsets.UTF_8)
                    Log.i(TAG, "Device banner: $deviceBanner")
                    isConnected = true
                    return@withContext Result.success(Unit)
                }
                AdbProtocol.A_AUTH -> {
                    // Server doesn't have our public key, send it
                    Log.w(TAG, "★ Signature NOT recognized - sending public key (user will see auth dialog)")
                    Log.i(TAG, "This happens when: 1) First connection, 2) Key was cleared, 3) Key format mismatch")

                    val publicKey = keyManager.getPublicKeyForAdb()
                    Log.i(TAG, "Public key size: ${publicKey.size} bytes")
                    Log.i(TAG, "Sending AUTH RSAPUBLICKEY message...")
                    val publicKeyMessage = AdbProtocol.createMessage(
                        AdbProtocol.A_AUTH,
                        AdbProtocol.ADB_AUTH_RSAPUBLICKEY,
                        0,
                        publicKey
                    )
                    output.write(publicKeyMessage)
                    output.flush()
                    Log.i(TAG, "AUTH RSAPUBLICKEY sent, waiting for user to accept...")

                    // Read final response
                    val finalResponse = AdbProtocol.readMessage(input)
                        ?: return@withContext Result.failure(Exception("No response after sending public key"))

                    Log.i(TAG, "Final response: ${AdbProtocol.commandToString(finalResponse.command)}")

                    if (finalResponse.command == AdbProtocol.A_CNXN) {
                        Log.i(TAG, "✓ Authentication successful with public key")
                        Log.i(TAG, "If 'Always allow' was selected, next connection should use signature auth")
                        val deviceBanner = String(finalResponse.data, Charsets.UTF_8)
                        Log.i(TAG, "Device banner: $deviceBanner")
                        isConnected = true
                        return@withContext Result.success(Unit)
                    } else {
                        Log.e(TAG, "✗ Authentication rejected - user denied or timeout")
                        return@withContext Result.failure(Exception(
                            "Authentication rejected by device. Please accept the connection on the target device."
                        ))
                    }
                }
                else -> {
                    Log.e(TAG, "Unexpected response: ${AdbProtocol.commandToString(authResponse.command)}")
                    return@withContext Result.failure(Exception(
                        "Unexpected response during authentication: ${AdbProtocol.commandToString(authResponse.command)}"
                    ))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Authentication failed with exception", e)
            return@withContext Result.failure(e)
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

    /**
     * Check if the connection is actually alive, not just the flag.
     * This detects sockets that have been closed by the OS (e.g., when app was backgrounded).
     */
    fun isConnected(): Boolean {
        if (!isConnected) return false

        val currentSocket = socket ?: return false

        // Check if socket is still truly connected
        if (currentSocket.isClosed || !currentSocket.isConnected) {
            Log.w(TAG, "Socket is closed or not connected, resetting connection state")
            isConnected = false
            return false
        }

        // Try to detect dead socket by checking if we can still use it
        // A socket that was closed by the remote end or by the OS will fail this check
        try {
            // sendUrgentData throws if socket is dead
            currentSocket.sendUrgentData(0)
        } catch (e: Exception) {
            Log.w(TAG, "Socket appears dead (urgent data test failed), resetting connection state", e)
            isConnected = false
            socket = null
            return false
        }

        return true
    }

    /**
     * Check if socket is usable without sending data (less intrusive check)
     */
    fun checkConnection(): Boolean {
        if (!isConnected) return false

        val currentSocket = socket ?: return false

        if (currentSocket.isClosed || !currentSocket.isConnected) {
            Log.w(TAG, "Socket is closed, marking as disconnected")
            isConnected = false
            socket = null
            return false
        }

        return true
    }
}
