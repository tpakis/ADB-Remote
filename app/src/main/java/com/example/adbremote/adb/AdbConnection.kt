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
        private const val SYSTEM_IDENTITY = "host::\u0000"
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
                ?: return@withContext Result.failure(Exception("No response from device"))

            when (response.command) {
                AdbProtocol.A_CNXN -> {
                    Log.d(TAG, "Connection established without authentication")
                    isConnected = true
                    Result.success(Unit)
                }
                AdbProtocol.A_AUTH -> {
                    Log.d(TAG, "Authentication required, attempting to authenticate")
                    handleAuthentication(newSocket, response)
                }
                else -> {
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
                return@withContext Result.failure(Exception("Expected AUTH TOKEN message"))
            }

            val token = authResponse.data
            Log.i(TAG, "=== Starting ADB Authentication ===")
            Log.i(TAG, "Received authentication token (${token.size} bytes)")

            // Sign the token with our private key
            val signature = keyManager.signToken(token)
            if (signature == null) {
                return@withContext Result.failure(Exception("Failed to sign authentication token"))
            }

            Log.i(TAG, "Sending signed signature (${signature.size} bytes) - if device has our key, this should work")
            val signatureMessage = AdbProtocol.createMessage(
                AdbProtocol.A_AUTH,
                AdbProtocol.ADB_AUTH_SIGNATURE,
                0,
                signature
            )
            output.write(signatureMessage)
            output.flush()

            // Read response
            authResponse = AdbProtocol.readMessage(input)
                ?: return@withContext Result.failure(Exception("No response after sending signature"))

            when (authResponse.command) {
                AdbProtocol.A_CNXN -> {
                    Log.i(TAG, "✓ Authentication successful with signature - device recognized our key!")
                    Log.i(TAG, "This means 'Always allow' is working correctly")
                    isConnected = true
                    return@withContext Result.success(Unit)
                }
                AdbProtocol.A_AUTH -> {
                    // Server doesn't have our public key, send it
                    Log.i(TAG, "✗ Signature not recognized - device doesn't have our key yet")
                    Log.i(TAG, "Sending public key for authorization (user will see dialog)")

                    val publicKey = keyManager.getPublicKeyForAdb()
                    Log.i(TAG, "Public key size: ${publicKey.size} bytes")
                    val publicKeyMessage = AdbProtocol.createMessage(
                        AdbProtocol.A_AUTH,
                        AdbProtocol.ADB_AUTH_RSAPUBLICKEY,
                        0,
                        publicKey
                    )
                    output.write(publicKeyMessage)
                    output.flush()

                    // Read final response
                    val finalResponse = AdbProtocol.readMessage(input)
                        ?: return@withContext Result.failure(Exception("No response after sending public key"))

                    if (finalResponse.command == AdbProtocol.A_CNXN) {
                        Log.i(TAG, "✓ Authentication successful with public key")
                        Log.i(TAG, "If you selected 'Always allow', next connection should use signature auth")
                        isConnected = true
                        return@withContext Result.success(Unit)
                    } else {
                        Log.e(TAG, "✗ Authentication rejected by device")
                        return@withContext Result.failure(Exception(
                            "Authentication rejected by device. Please accept the connection on the target device."
                        ))
                    }
                }
                else -> {
                    return@withContext Result.failure(Exception(
                        "Unexpected response during authentication: ${AdbProtocol.commandToString(authResponse.command)}"
                    ))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Authentication failed", e)
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

    fun isConnected(): Boolean = isConnected
}
