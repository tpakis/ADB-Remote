package com.example.adbremote.platform

/**
 * Simple byte buffer for constructing and parsing ADB protocol messages.
 * Uses little-endian byte order (as required by ADB protocol).
 */
class ByteBuffer private constructor(
    private val data: ByteArray,
    private var position: Int = 0,
    private val limit: Int = data.size
) {
    companion object {
        /**
         * Allocate a new buffer with the given capacity.
         */
        fun allocate(capacity: Int): ByteBuffer {
            return ByteBuffer(ByteArray(capacity))
        }

        /**
         * Wrap an existing byte array.
         */
        fun wrap(array: ByteArray): ByteBuffer {
            return ByteBuffer(array)
        }
    }

    /**
     * Put a single byte at the current position.
     */
    fun put(value: Byte): ByteBuffer {
        data[position++] = value
        return this
    }

    /**
     * Put a byte array at the current position.
     */
    fun put(src: ByteArray): ByteBuffer {
        src.copyInto(data, position)
        position += src.size
        return this
    }

    /**
     * Put a byte array with offset and length at the current position.
     */
    fun put(src: ByteArray, offset: Int, length: Int): ByteBuffer {
        src.copyInto(data, position, offset, offset + length)
        position += length
        return this
    }

    /**
     * Put a 32-bit integer in little-endian order.
     */
    fun putInt(value: Int): ByteBuffer {
        data[position++] = (value and 0xFF).toByte()
        data[position++] = ((value shr 8) and 0xFF).toByte()
        data[position++] = ((value shr 16) and 0xFF).toByte()
        data[position++] = ((value shr 24) and 0xFF).toByte()
        return this
    }

    /**
     * Get a single byte at the current position.
     */
    fun get(): Byte {
        return data[position++]
    }

    /**
     * Get bytes into the destination array.
     */
    fun get(dst: ByteArray): ByteBuffer {
        data.copyInto(dst, 0, position, position + dst.size)
        position += dst.size
        return this
    }

    /**
     * Get bytes into the destination array with offset and length.
     */
    fun get(dst: ByteArray, offset: Int, length: Int): ByteBuffer {
        data.copyInto(dst, offset, position, position + length)
        position += length
        return this
    }

    /**
     * Get a 32-bit integer in little-endian order.
     */
    fun getInt(): Int {
        val b0 = data[position++].toInt() and 0xFF
        val b1 = data[position++].toInt() and 0xFF
        val b2 = data[position++].toInt() and 0xFF
        val b3 = data[position++].toInt() and 0xFF
        return b0 or (b1 shl 8) or (b2 shl 16) or (b3 shl 24)
    }

    /**
     * Get the underlying byte array.
     */
    fun array(): ByteArray = data

    /**
     * Get the current position.
     */
    fun position(): Int = position

    /**
     * Set the current position.
     */
    fun position(newPosition: Int): ByteBuffer {
        position = newPosition
        return this
    }

    /**
     * Get the limit.
     */
    fun limit(): Int = limit

    /**
     * Get remaining bytes from position to limit.
     */
    fun remaining(): Int = limit - position

    /**
     * Check if there are remaining bytes.
     */
    fun hasRemaining(): Boolean = position < limit

    /**
     * Rewind to the beginning.
     */
    fun rewind(): ByteBuffer {
        position = 0
        return this
    }

    /**
     * Flip the buffer for reading after writing.
     */
    fun flip(): ByteBuffer {
        position = 0
        return this
    }
}
