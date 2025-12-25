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
            val digest = crypto.sha1(encoded)
            digest.take(16).joinToString(":") { "%02x".format(it) }
        } catch (e: Exception) {
            "error"
        }
    }

    /**
     * Sign the authentication token from the ADB server.
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
     */
    fun getPublicKeyForAdb(): ByteArray {
        val kp = keyPair ?: throw IllegalStateException("Key pair not initialized")

        val modulusBytes = crypto.getPublicKeyModulus(kp.publicKey)
        val exponent = crypto.getPublicKeyExponent(kp.publicKey)

        PlatformLogger.i(TAG, "Building Android-native RSA public key:")
        PlatformLogger.i(TAG, "  Modulus size: ${modulusBytes.size} bytes")
        PlatformLogger.i(TAG, "  Exponent: $exponent")

        // For 2048-bit key, len = 64 (number of uint32_t values)
        val len = 64

        // Calculate Montgomery constants using platform-agnostic helper
        val montgomeryData = calculateMontgomeryConstants(modulusBytes, len)

        // Build the struct (524 bytes for 2048-bit key)
        val buffer = ByteBuffer.allocate(4 + 4 + len * 4 + len * 4 + 4)

        // len (uint32_t)
        buffer.putInt(len)

        // n0inv (uint32_t)
        buffer.putInt(montgomeryData.n0inv)

        // n[64] - modulus as little-endian uint32_t array
        buffer.put(montgomeryData.modulusLE)

        // rr[64] - R^2 mod n as little-endian uint32_t array
        buffer.put(montgomeryData.rrLE)

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

/**
 * Data class holding Montgomery constants for RSA public key format
 */
data class MontgomeryData(
    val n0inv: Int,
    val modulusLE: ByteArray,
    val rrLE: ByteArray
)

/**
 * Calculate Montgomery constants for the Android RSA public key format.
 * This is platform-agnostic - uses simple arithmetic.
 */
expect fun calculateMontgomeryConstants(modulusBytes: ByteArray, len: Int): MontgomeryData

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
