package com.example.adbremote.adb

import android.content.Context
import android.util.Base64
import android.util.Log
import java.io.File
import java.math.BigInteger
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.*
import java.security.interfaces.RSAPublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher

/**
 * Manages RSA keys for ADB authentication
 */
class AdbKeyManager(private val context: Context) {

    companion object {
        private const val TAG = "AdbKeyManager"
        private const val KEY_SIZE = 2048
        private const val PRIVATE_KEY_FILE = "adb_private_key"
        private const val PUBLIC_KEY_FILE = "adb_public_key"
        private const val ADB_PUBLIC_KEY_NAME = "android-adb-remote"
    }

    private var privateKey: PrivateKey? = null
    private var publicKey: PublicKey? = null

    /**
     * Initialize keys - load existing or generate new ones
     */
    fun initialize() {
        val privateKeyFile = File(context.filesDir, PRIVATE_KEY_FILE)
        val publicKeyFile = File(context.filesDir, PUBLIC_KEY_FILE)

        Log.i(TAG, "Initializing ADB keys...")
        Log.i(TAG, "Private key file exists: ${privateKeyFile.exists()}")
        Log.i(TAG, "Public key file exists: ${publicKeyFile.exists()}")

        if (!loadKeys()) {
            Log.i(TAG, "No existing keys found, generating new RSA key pair")
            generateKeys()
            saveKeys()
            Log.i(TAG, "New keys generated and saved")
        } else {
            Log.i(TAG, "Successfully loaded existing RSA keys")
        }

        // Log key fingerprint for debugging
        publicKey?.let { key ->
            val fingerprint = getKeyFingerprint(key)
            Log.i(TAG, "Public key fingerprint: $fingerprint")
            Log.i(TAG, "If signature auth fails, this fingerprint should match what's stored on target device")
        }
    }

    /**
     * Get MD5 fingerprint of public key for debugging
     */
    private fun getKeyFingerprint(key: PublicKey): String {
        return try {
            val md = java.security.MessageDigest.getInstance("MD5")
            val digest = md.digest(key.encoded)
            digest.joinToString(":") { "%02x".format(it) }
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
     *
     * The DigestInfo for SHA-1 is:
     * 30 21 30 09 06 05 2b 0e 03 02 1a 05 00 04 14 <20-byte-hash>
     */
    fun signToken(token: ByteArray): ByteArray? {
        return try {
            val key = privateKey ?: run {
                Log.e(TAG, "Private key is null!")
                return null
            }
            val rsaKey = key as java.security.interfaces.RSAPrivateKey

            // Log key fingerprint to verify we're using the same key
            val keyFingerprint = getKeyFingerprint(publicKey!!)
            Log.i(TAG, "Using key with fingerprint: $keyFingerprint")
            Log.i(TAG, "Signing token (${token.size} bytes)")
            Log.i(TAG, "Token: ${token.joinToString("") { "%02x".format(it) }}")

            // SHA-1 DigestInfo prefix (ASN.1 DER encoding)
            // SEQUENCE { SEQUENCE { OID sha1, NULL }, OCTET STRING <hash> }
            val digestInfoPrefix = byteArrayOf(
                0x30, 0x21,                         // SEQUENCE, length 33
                0x30, 0x09,                         // SEQUENCE, length 9
                0x06, 0x05,                         // OID, length 5
                0x2b, 0x0e, 0x03, 0x02, 0x1a,      // SHA-1 OID: 1.3.14.3.2.26
                0x05, 0x00,                         // NULL
                0x04, 0x14                          // OCTET STRING, length 20
            )

            // Build DigestInfo: prefix + token (the token IS the SHA-1 hash)
            val digestInfo = digestInfoPrefix + token
            Log.i(TAG, "DigestInfo: ${digestInfo.size} bytes (15 prefix + ${token.size} hash)")
            Log.i(TAG, "DigestInfo hex: ${digestInfo.joinToString("") { "%02x".format(it) }}")

            // Key size in bytes (256 for 2048-bit key)
            val keySize = (rsaKey.modulus.bitLength() + 7) / 8
            Log.i(TAG, "Key size: $keySize bytes")

            // Build PKCS#1 v1.5 Type 1 padded message
            // Format: 0x00 || 0x01 || 0xFF...0xFF || 0x00 || DigestInfo
            val paddingLength = keySize - digestInfo.size - 3

            if (paddingLength < 8) {
                Log.e(TAG, "DigestInfo too large for key size")
                return null
            }

            val paddedMessage = ByteArray(keySize)
            paddedMessage[0] = 0x00
            paddedMessage[1] = 0x01
            for (i in 0 until paddingLength) {
                paddedMessage[2 + i] = 0xFF.toByte()
            }
            paddedMessage[2 + paddingLength] = 0x00
            System.arraycopy(digestInfo, 0, paddedMessage, 3 + paddingLength, digestInfo.size)

            Log.i(TAG, "Padded message: $keySize bytes, padding: $paddingLength bytes")
            Log.i(TAG, "Padded start: ${paddedMessage.take(10).joinToString("") { "%02x".format(it) }}...")
            Log.i(TAG, "Padded end (DigestInfo): ...${paddedMessage.takeLast(40).joinToString("") { "%02x".format(it) }}")

            // Perform raw RSA operation (private key encrypt = sign)
            val cipher = Cipher.getInstance("RSA/ECB/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, key)
            val signed = cipher.doFinal(paddedMessage)

            Log.i(TAG, "Generated signature: ${signed.size} bytes")
            Log.i(TAG, "Signature: ${signed.take(16).joinToString("") { "%02x".format(it) }}...")

            // Verify locally by decrypting with public key
            try {
                val verifyCipher = Cipher.getInstance("RSA/ECB/NoPadding")
                verifyCipher.init(Cipher.DECRYPT_MODE, publicKey)
                val decrypted = verifyCipher.doFinal(signed)

                Log.i(TAG, "Decrypted start: ${decrypted.take(10).joinToString("") { "%02x".format(it) }}...")
                Log.i(TAG, "Decrypted end: ...${decrypted.takeLast(40).joinToString("") { "%02x".format(it) }}")

                val matches = paddedMessage.contentEquals(decrypted)
                Log.i(TAG, "Local verification: ${if (matches) "PASS" else "FAIL"}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to verify signature locally", e)
            }

            signed
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sign token", e)
            e.printStackTrace()
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
        val key = publicKey as? RSAPublicKey ?: throw IllegalStateException("Public key not initialized")

        val modulus = key.modulus
        val exponent = key.publicExponent

        Log.i(TAG, "Building Android-native RSA public key:")
        Log.i(TAG, "  Modulus bit length: ${modulus.bitLength()}")
        Log.i(TAG, "  Exponent: $exponent")

        // For 2048-bit key, len = 64 (number of uint32_t values)
        val len = (modulus.bitLength() + 31) / 32
        Log.i(TAG, "  Modulus length in uint32: $len")

        // Calculate n0inv = -(1/n[0]) mod 2^32
        // n[0] is the least significant 32 bits of the modulus
        val n0 = modulus.and(BigInteger.valueOf(0xFFFFFFFFL))
        val n0inv = n0.modInverse(BigInteger.ONE.shiftLeft(32)).negate().and(BigInteger.valueOf(0xFFFFFFFFL))
        Log.i(TAG, "  n0inv: ${n0inv.toLong().toUInt()}")

        // Calculate R^2 mod n (Montgomery constant)
        // R = 2^(32*len) = 2^2048 for 2048-bit key
        val r = BigInteger.ONE.shiftLeft(32 * len)
        val rr = r.multiply(r).mod(modulus)

        // Build the struct (524 bytes for 2048-bit key)
        // All values are little-endian
        val buffer = ByteBuffer.allocate(4 + 4 + len * 4 + len * 4 + 4)
        buffer.order(ByteOrder.LITTLE_ENDIAN)

        // len (uint32_t)
        buffer.putInt(len)

        // n0inv (uint32_t)
        buffer.putInt(n0inv.toInt())

        // n[64] - modulus as little-endian uint32_t array
        val modulusBytes = modulus.toByteArray()
        // Remove leading zero if present (BigInteger sign extension)
        val modulusBytesClean = if (modulusBytes[0] == 0.toByte() && modulusBytes.size > len * 4) {
            modulusBytes.copyOfRange(1, modulusBytes.size)
        } else {
            modulusBytes
        }
        // Pad to len*4 bytes if needed
        val modulusPadded = ByteArray(len * 4)
        // BigInteger is big-endian, we need to reverse and copy
        for (i in modulusBytesClean.indices) {
            modulusPadded[modulusBytesClean.size - 1 - i] = modulusBytesClean[i]
        }
        buffer.put(modulusPadded)

        // rr[64] - R^2 mod n as little-endian uint32_t array
        val rrBytes = rr.toByteArray()
        val rrBytesClean = if (rrBytes[0] == 0.toByte() && rrBytes.size > len * 4) {
            rrBytes.copyOfRange(1, rrBytes.size)
        } else {
            rrBytes
        }
        val rrPadded = ByteArray(len * 4)
        for (i in rrBytesClean.indices) {
            rrPadded[rrBytesClean.size - 1 - i] = rrBytesClean[i]
        }
        buffer.put(rrPadded)

        // e (uint32_t) - public exponent
        buffer.putInt(exponent.toInt())

        // Base64 encode and add identifier
        val structBytes = buffer.array()
        val base64Key = Base64.encodeToString(structBytes, Base64.NO_WRAP)
        val adbKey = "$base64Key $ADB_PUBLIC_KEY_NAME\u0000"

        Log.i(TAG, "Android struct size: ${structBytes.size} bytes")
        Log.i(TAG, "Base64 encoded: ${base64Key.length} chars")
        Log.i(TAG, "Final ADB key: ${adbKey.length} bytes")

        // Log preview
        val preview = if (base64Key.length > 40) "${base64Key.substring(0, 40)}..." else base64Key
        Log.i(TAG, "Public key preview: $preview")

        return adbKey.toByteArray(Charsets.UTF_8)
    }

    /**
     * Convert a BigInteger to SSH mpint (multiple precision integer) format.
     *
     * SSH mpint format:
     * - Big-endian two's complement representation
     * - If the number is positive and the high bit is set, prepend 0x00
     * - Strip unnecessary leading zeros (but keep one if needed for sign)
     *
     * BigInteger.toByteArray() already handles the sign-extension correctly,
     * so we just need to strip redundant leading zeros while preserving the sign bit.
     */
    private fun toSshMpint(bigInt: BigInteger): ByteArray {
        val bytes = bigInt.toByteArray()

        // For positive numbers, BigInteger.toByteArray() correctly adds a leading 0x00
        // when the high bit is set. We should NOT strip this zero.

        // Count truly redundant leading zeros (zeros that don't affect the sign)
        var redundantZeros = 0
        while (redundantZeros < bytes.size - 1 &&
               bytes[redundantZeros] == 0.toByte() &&
               (bytes[redundantZeros + 1].toInt() and 0x80) == 0) {
            // Only strip a zero if the next byte doesn't have high bit set
            redundantZeros++
        }

        return if (redundantZeros > 0) {
            bytes.copyOfRange(redundantZeros, bytes.size)
        } else {
            bytes
        }
    }


    private fun generateKeys() {
        try {
            val keyPairGenerator = KeyPairGenerator.getInstance("RSA")
            keyPairGenerator.initialize(KEY_SIZE)
            val keyPair = keyPairGenerator.generateKeyPair()

            privateKey = keyPair.private
            publicKey = keyPair.public

            Log.d(TAG, "Generated new RSA key pair")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to generate keys", e)
            throw e
        }
    }

    private fun saveKeys() {
        try {
            val privateKeyFile = File(context.filesDir, PRIVATE_KEY_FILE)
            val publicKeyFile = File(context.filesDir, PUBLIC_KEY_FILE)

            privateKey?.let {
                privateKeyFile.writeBytes(it.encoded)
            }

            publicKey?.let {
                publicKeyFile.writeBytes(it.encoded)
            }

            Log.d(TAG, "Saved RSA keys to storage")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save keys", e)
        }
    }

    private fun loadKeys(): Boolean {
        return try {
            val privateKeyFile = File(context.filesDir, PRIVATE_KEY_FILE)
            val publicKeyFile = File(context.filesDir, PUBLIC_KEY_FILE)

            if (!privateKeyFile.exists() || !publicKeyFile.exists()) {
                return false
            }

            val keyFactory = KeyFactory.getInstance("RSA")

            val privateKeyBytes = privateKeyFile.readBytes()
            val privateKeySpec = PKCS8EncodedKeySpec(privateKeyBytes)
            privateKey = keyFactory.generatePrivate(privateKeySpec)

            val publicKeyBytes = publicKeyFile.readBytes()
            val publicKeySpec = X509EncodedKeySpec(publicKeyBytes)
            publicKey = keyFactory.generatePublic(publicKeySpec)

            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load keys", e)
            false
        }
    }
}
