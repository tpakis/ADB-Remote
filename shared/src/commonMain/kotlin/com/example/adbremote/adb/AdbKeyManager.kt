package com.example.adbremote.adb

import com.example.adbremote.platform.*

/**
 * Manages RSA keys for ADB authentication.
 * Uses platform-specific crypto and storage implementations.
 */
class AdbKeyManager(
    private val storage: PlatformStorage,
    private val crypto: PlatformCrypto
) {
    companion object {
        private const val TAG = "AdbKeyManager"
        private const val PRIVATE_KEY_KEY = "adb_private_key"
        private const val PUBLIC_KEY_KEY = "adb_public_key"
        private const val ADB_PUBLIC_KEY_NAME = "android-adb-remote"
    }

    private var keyPair: PlatformKeyPair? = null

    /**
     * Initialize keys - load existing or generate new ones
     */
    fun initialize() {
        PlatformLogger.i(TAG, "Initializing ADB keys...")

        if (!loadKeys()) {
            PlatformLogger.i(TAG, "No existing keys found, generating new RSA key pair")
            generateKeys()
            saveKeys()
            PlatformLogger.i(TAG, "New keys generated and saved")
        } else {
            PlatformLogger.i(TAG, "Successfully loaded existing RSA keys")
        }

        // Log key fingerprint for debugging
        keyPair?.let { kp ->
            val fingerprint = getKeyFingerprint(kp.publicKey)
            PlatformLogger.i(TAG, "Public key fingerprint: $fingerprint")
        }
    }

    /**
     * Get MD5 fingerprint of public key for debugging
     */
    private fun getKeyFingerprint(key: PlatformPublicKey): String {
        return try {
            val encoded = keyPair?.getPublicKeyEncoded() ?: return "error"
            val digest = crypto.sha1(encoded) // Using SHA1 for shorter fingerprint
            digest.take(16).joinToString(":") { "%02x".format(it) }
        } catch (e: Exception) {
            "error"
        }
    }

    /**
     * Sign the authentication token from the ADB server.
     *
     * ADB uses RSA_sign(NID_sha1, token, ...) which:
     * 1. Wraps the token (which IS a SHA-1 hash) in a DigestInfo structure
     * 2. Applies PKCS#1 v1.5 Type 1 padding
     * 3. Performs raw RSA private key operation
     */
    fun signToken(token: ByteArray): ByteArray? {
        return try {
            val kp = keyPair ?: run {
                PlatformLogger.e(TAG, "Key pair is null!")
                return null
            }

            PlatformLogger.i(TAG, "Signing token (${token.size} bytes)")
            PlatformLogger.i(TAG, "Token: ${token.joinToString("") { "%02x".format(it) }}")

            val signed = crypto.signWithSha1DigestInfo(token, kp.privateKey)

            PlatformLogger.i(TAG, "Generated signature: ${signed.size} bytes")
            PlatformLogger.i(TAG, "Signature: ${signed.take(16).joinToString("") { "%02x".format(it) }}...")

            signed
        } catch (e: Exception) {
            PlatformLogger.e(TAG, "Failed to sign token", e)
            null
        }
    }

    /**
     * Get the public key in Android-native RSAPublicKey format.
     *
     * This is a 524-byte binary structure that Android's adbd can directly use:
     * struct RSAPublicKey {
     *     uint32_t len;        // Length of n in uint32_t (64 for 2048-bit)
     *     uint32_t n0inv;      // -(1/n[0]) mod 2^32
     *     uint32_t n[64];      // Modulus (little-endian uint32_t array)
     *     uint32_t rr[64];     // R^2 mod n (Montgomery constant, little-endian)
     *     uint32_t e;          // Public exponent
     * }
     *
     * Then base64 encoded with " user@host\0" appended.
     */
    fun getPublicKeyForAdb(): ByteArray {
        val kp = keyPair ?: throw IllegalStateException("Key pair not initialized")

        val modulusBytes = crypto.getPublicKeyModulus(kp.publicKey)
        val exponent = crypto.getPublicKeyExponent(kp.publicKey)

        PlatformLogger.i(TAG, "Building Android-native RSA public key:")
        PlatformLogger.i(TAG, "  Modulus size: ${modulusBytes.size} bytes")
        PlatformLogger.i(TAG, "  Exponent: $exponent")

        // For 2048-bit key, len = 64 (number of uint32_t values)
        val len = (modulusBytes.size * 8 + 31) / 32

        // Convert modulus to BigInteger-like operations for Montgomery calculations
        val modulus = bytesToBigInt(modulusBytes)

        // Calculate n0inv = -(1/n[0]) mod 2^32
        val n0 = modulus.and(0xFFFFFFFFL.toBigInt())
        val n0inv = n0.modInverse(1L.toBigInt().shl(32)).negate().and(0xFFFFFFFFL.toBigInt())

        // Calculate R^2 mod n (Montgomery constant)
        val r = 1L.toBigInt().shl(32 * len)
        val rr = r.multiply(r).mod(modulus)

        // Build the struct (524 bytes for 2048-bit key)
        val buffer = ByteBuffer.allocate(4 + 4 + len * 4 + len * 4 + 4)

        // len (uint32_t)
        buffer.putInt(len)

        // n0inv (uint32_t)
        buffer.putInt(n0inv.toInt())

        // n[64] - modulus as little-endian uint32_t array
        val modulusLE = bigIntToLittleEndianUint32Array(modulus, len)
        buffer.put(modulusLE)

        // rr[64] - R^2 mod n as little-endian uint32_t array
        val rrLE = bigIntToLittleEndianUint32Array(rr, len)
        buffer.put(rrLE)

        // e (uint32_t) - public exponent
        buffer.putInt(exponent)

        // Base64 encode and add identifier
        val structBytes = buffer.array()
        val base64Key = structBytes.encodeBase64()
        val adbKey = "$base64Key $ADB_PUBLIC_KEY_NAME\u0000"

        PlatformLogger.i(TAG, "Android struct size: ${structBytes.size} bytes")
        PlatformLogger.i(TAG, "Base64 encoded: ${base64Key.length} chars")

        return adbKey.encodeToByteArray()
    }

    private fun generateKeys() {
        keyPair = crypto.generateRsaKeyPair()
        PlatformLogger.d(TAG, "Generated new RSA key pair")
    }

    private fun saveKeys() {
        try {
            keyPair?.let { kp ->
                storage.saveBytes(PRIVATE_KEY_KEY, kp.getPrivateKeyEncoded())
                storage.saveBytes(PUBLIC_KEY_KEY, kp.getPublicKeyEncoded())
            }
            PlatformLogger.d(TAG, "Saved RSA keys to storage")
        } catch (e: Exception) {
            PlatformLogger.e(TAG, "Failed to save keys", e)
        }
    }

    private fun loadKeys(): Boolean {
        return try {
            val privateKeyBytes = storage.getBytes(PRIVATE_KEY_KEY)
            val publicKeyBytes = storage.getBytes(PUBLIC_KEY_KEY)

            if (privateKeyBytes == null || publicKeyBytes == null) {
                return false
            }

            keyPair = crypto.loadKeyPair(privateKeyBytes, publicKeyBytes)
            true
        } catch (e: Exception) {
            PlatformLogger.e(TAG, "Failed to load keys", e)
            false
        }
    }
}

// Simple BigInteger implementation for the Montgomery calculations
// This is a minimal implementation just for the public key format generation

private class BigInt private constructor(private val magnitude: LongArray, private val negative: Boolean = false) {
    companion object {
        val ZERO = BigInt(longArrayOf(0))
        val ONE = BigInt(longArrayOf(1))

        fun fromLong(value: Long): BigInt {
            return if (value < 0) {
                BigInt(longArrayOf(-value), true)
            } else {
                BigInt(longArrayOf(value))
            }
        }

        fun fromBytes(bytes: ByteArray): BigInt {
            if (bytes.isEmpty()) return ZERO

            // Convert big-endian bytes to magnitude
            val words = mutableListOf<Long>()
            var i = bytes.size
            while (i > 0) {
                var word = 0L
                val start = maxOf(0, i - 4)
                for (j in start until i) {
                    word = (word shl 8) or (bytes[j].toLong() and 0xFF)
                }
                words.add(word)
                i -= 4
            }

            // Remove leading zeros
            while (words.size > 1 && words.last() == 0L) {
                words.removeLast()
            }

            return BigInt(words.toLongArray())
        }
    }

    fun and(other: BigInt): BigInt {
        val result = LongArray(minOf(magnitude.size, other.magnitude.size))
        for (i in result.indices) {
            result[i] = magnitude[i] and other.magnitude[i]
        }
        return BigInt(result)
    }

    fun shl(n: Int): BigInt {
        if (n == 0) return this
        val wordShift = n / 32
        val bitShift = n % 32

        val newSize = magnitude.size + wordShift + 1
        val result = LongArray(newSize)

        var carry = 0L
        for (i in magnitude.indices) {
            val shifted = (magnitude[i] shl bitShift) or carry
            result[i + wordShift] = shifted and 0xFFFFFFFFL
            carry = shifted shr 32
        }
        if (carry != 0L) {
            result[magnitude.size + wordShift] = carry
        }

        // Trim leading zeros
        var len = result.size
        while (len > 1 && result[len - 1] == 0L) len--
        return BigInt(result.copyOf(len), negative)
    }

    fun multiply(other: BigInt): BigInt {
        val result = LongArray(magnitude.size + other.magnitude.size)

        for (i in magnitude.indices) {
            var carry = 0L
            for (j in other.magnitude.indices) {
                val product = magnitude[i] * other.magnitude[j] + result[i + j] + carry
                result[i + j] = product and 0xFFFFFFFFL
                carry = product shr 32
            }
            result[i + other.magnitude.size] = carry
        }

        // Trim leading zeros
        var len = result.size
        while (len > 1 && result[len - 1] == 0L) len--
        return BigInt(result.copyOf(len), negative xor other.negative)
    }

    fun mod(other: BigInt): BigInt {
        // Simple modular reduction - not efficient but works
        var remainder = this
        while (remainder >= other) {
            remainder = remainder.subtract(other)
        }
        return remainder
    }

    fun modInverse(mod: BigInt): BigInt {
        // Extended Euclidean algorithm
        var t = ZERO
        var newT = ONE
        var r = mod
        var newR = this

        while (newR != ZERO) {
            val quotient = r.divide(newR)
            val tempT = t
            t = newT
            newT = tempT.subtract(quotient.multiply(newT))

            val tempR = r
            r = newR
            newR = tempR.subtract(quotient.multiply(newR))
        }

        if (t.negative) {
            t = t.add(mod)
        }
        return t
    }

    fun negate(): BigInt = BigInt(magnitude, !negative)

    fun add(other: BigInt): BigInt {
        if (negative != other.negative) {
            return if (negative) other.subtract(this.negate()) else this.subtract(other.negate())
        }

        val result = LongArray(maxOf(magnitude.size, other.magnitude.size) + 1)
        var carry = 0L

        for (i in result.indices) {
            val a = if (i < magnitude.size) magnitude[i] else 0L
            val b = if (i < other.magnitude.size) other.magnitude[i] else 0L
            val sum = a + b + carry
            result[i] = sum and 0xFFFFFFFFL
            carry = sum shr 32
        }

        var len = result.size
        while (len > 1 && result[len - 1] == 0L) len--
        return BigInt(result.copyOf(len), negative)
    }

    fun subtract(other: BigInt): BigInt {
        if (negative != other.negative) {
            return this.add(other.negate())
        }

        val cmp = compareAbsolute(other)
        if (cmp == 0) return ZERO
        if (cmp < 0) return other.subtract(this).negate()

        val result = LongArray(magnitude.size)
        var borrow = 0L

        for (i in magnitude.indices) {
            val a = magnitude[i]
            val b = if (i < other.magnitude.size) other.magnitude[i] else 0L
            val diff = a - b - borrow
            if (diff < 0) {
                result[i] = (diff + 0x100000000L) and 0xFFFFFFFFL
                borrow = 1
            } else {
                result[i] = diff
                borrow = 0
            }
        }

        var len = result.size
        while (len > 1 && result[len - 1] == 0L) len--
        return BigInt(result.copyOf(len), negative)
    }

    fun divide(other: BigInt): BigInt {
        // Simple long division
        if (compareAbsolute(other) < 0) return ZERO

        var remainder = this
        var quotient = ZERO

        // Find the highest bit position difference
        val shift = this.bitLength() - other.bitLength()
        var divisor = other.shl(shift)
        var bit = ONE.shl(shift)

        for (i in 0..shift) {
            if (remainder >= divisor) {
                remainder = remainder.subtract(divisor)
                quotient = quotient.add(bit)
            }
            divisor = divisor.shr(1)
            bit = bit.shr(1)
        }

        return BigInt(quotient.magnitude, negative xor other.negative)
    }

    private fun shr(n: Int): BigInt {
        if (n == 0) return this
        val wordShift = n / 32
        val bitShift = n % 32

        if (wordShift >= magnitude.size) return ZERO

        val newSize = magnitude.size - wordShift
        val result = LongArray(newSize)

        for (i in result.indices) {
            result[i] = magnitude[i + wordShift] shr bitShift
            if (bitShift > 0 && i + wordShift + 1 < magnitude.size) {
                result[i] = result[i] or ((magnitude[i + wordShift + 1] shl (32 - bitShift)) and 0xFFFFFFFFL)
            }
        }

        var len = result.size
        while (len > 1 && result[len - 1] == 0L) len--
        return BigInt(result.copyOf(len), negative)
    }

    private fun bitLength(): Int {
        if (magnitude.isEmpty() || (magnitude.size == 1 && magnitude[0] == 0L)) return 0
        val highWord = magnitude.last()
        var bits = (magnitude.size - 1) * 32
        var hw = highWord
        while (hw != 0L) {
            bits++
            hw = hw shr 1
        }
        return bits
    }

    private fun compareAbsolute(other: BigInt): Int {
        if (magnitude.size != other.magnitude.size) {
            return magnitude.size.compareTo(other.magnitude.size)
        }
        for (i in magnitude.indices.reversed()) {
            if (magnitude[i] != other.magnitude[i]) {
                return magnitude[i].compareTo(other.magnitude[i])
            }
        }
        return 0
    }

    operator fun compareTo(other: BigInt): Int {
        if (negative != other.negative) {
            return if (negative) -1 else 1
        }
        val cmp = compareAbsolute(other)
        return if (negative) -cmp else cmp
    }

    override fun equals(other: Any?): Boolean {
        if (other !is BigInt) return false
        if (negative != other.negative) return false
        return magnitude.contentEquals(other.magnitude)
    }

    override fun hashCode(): Int = magnitude.contentHashCode()

    fun toInt(): Int = if (negative) -(magnitude[0].toInt()) else magnitude[0].toInt()

    fun toLittleEndianBytes(length: Int): ByteArray {
        val result = ByteArray(length)
        for (i in magnitude.indices) {
            val word = magnitude[i]
            val offset = i * 4
            if (offset < length) result[offset] = (word and 0xFF).toByte()
            if (offset + 1 < length) result[offset + 1] = ((word shr 8) and 0xFF).toByte()
            if (offset + 2 < length) result[offset + 2] = ((word shr 16) and 0xFF).toByte()
            if (offset + 3 < length) result[offset + 3] = ((word shr 24) and 0xFF).toByte()
        }
        return result
    }

    operator fun compareTo(other: Int): Int = compareTo(fromLong(other.toLong()))
}

private fun Long.toBigInt() = BigInt.fromLong(this)
private fun bytesToBigInt(bytes: ByteArray) = BigInt.fromBytes(bytes)
private fun bigIntToLittleEndianUint32Array(value: BigInt, len: Int): ByteArray = value.toLittleEndianBytes(len * 4)

// Base64 encoding without external dependencies
private fun ByteArray.encodeBase64(): String {
    val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"
    val result = StringBuilder()
    var i = 0
    while (i < size) {
        val b0 = this[i].toInt() and 0xFF
        val b1 = if (i + 1 < size) this[i + 1].toInt() and 0xFF else 0
        val b2 = if (i + 2 < size) this[i + 2].toInt() and 0xFF else 0

        result.append(chars[b0 shr 2])
        result.append(chars[((b0 and 0x03) shl 4) or (b1 shr 4)])
        result.append(if (i + 1 < size) chars[((b1 and 0x0F) shl 2) or (b2 shr 6)] else '=')
        result.append(if (i + 2 < size) chars[b2 and 0x3F] else '=')

        i += 3
    }
    return result.toString()
}
