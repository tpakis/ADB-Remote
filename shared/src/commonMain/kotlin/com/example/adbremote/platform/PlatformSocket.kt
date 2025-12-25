package com.example.adbremote.platform

/**
 * Platform-specific TCP socket abstraction for ADB connections.
 */
expect class PlatformSocket() {
    /**
     * Connect to the specified host and port with timeout.
     * @throws Exception if connection fails
     */
    suspend fun connect(host: String, port: Int, timeoutMs: Int)

    /**
     * Read bytes into the buffer.
     * @return Number of bytes read, or -1 if end of stream
     */
    fun read(buffer: ByteArray, offset: Int, length: Int): Int

    /**
     * Read exactly the specified number of bytes.
     * @throws Exception if unable to read all bytes
     */
    fun readFully(buffer: ByteArray, offset: Int, length: Int)

    /**
     * Write bytes to the socket.
     */
    fun write(data: ByteArray)

    /**
     * Write bytes to the socket and flush.
     */
    fun writeAndFlush(data: ByteArray)

    /**
     * Close the socket connection.
     */
    fun close()

    /**
     * Check if the socket is currently connected.
     */
    fun isConnected(): Boolean

    /**
     * Check if the socket has been closed.
     */
    fun isClosed(): Boolean

    /**
     * Check if the connection is still alive (more reliable than isConnected).
     * May attempt to send urgent data to verify.
     */
    fun checkConnectionAlive(): Boolean

    /**
     * Get the number of bytes available to read without blocking.
     */
    fun available(): Int

    /**
     * Set socket timeout for read operations.
     */
    fun setSoTimeout(timeoutMs: Int)
}
