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
     * Sign the authentication token from the ADB server
     *
     * CRITICAL: ADB sends a pre-hashed 20-byte token and expects RAW RSA signature
     * with PKCS#1 v1.5 padding. Do NOT hash it again.
     */
    fun signToken(token: ByteArray): ByteArray? {
        return try {
            val key = privateKey ?: return null

            Log.i(TAG, "Signing token (${token.size} bytes) with raw RSA-PKCS#1")
            Log.i(TAG, "Token: ${token.joinToString("") { "%02x".format(it) }}")

            // Use Cipher for raw RSA with PKCS#1 padding
            // ENCRYPT_MODE with private key = signing operation
            val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
            cipher.init(Cipher.ENCRYPT_MODE, key)
            val signed = cipher.doFinal(token)

            Log.i(TAG, "Generated signature: ${signed.size} bytes")
            Log.i(TAG, "Signature preview: ${signed.take(16).joinToString("") { "%02x".format(it) }}...")

            // Verify the signature locally to ensure it's correct
            try {
                val verifyCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
                verifyCipher.init(Cipher.DECRYPT_MODE, publicKey)
                val verified = verifyCipher.doFinal(signed)

                val matches = verified.contentEquals(token)
                Log.i(TAG, "Local signature verification: ${if (matches) "✓ PASS" else "✗ FAIL"}")

                if (!matches) {
                    Log.e(TAG, "WARNING: Our signature doesn't verify with our own public key!")
                    Log.e(TAG, "Expected: ${token.joinToString("") { "%02x".format(it) }}")
                    Log.e(TAG, "Got:      ${verified.joinToString("") { "%02x".format(it) }}")
                }
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
     * Get the public key in ADB format (OpenSSH RSA public key format)
     * Format: base64(ssh_rsa_key_structure) + " " + name + "\0"
     *
     * SSH RSA public key structure (wire format):
     * - 4 bytes: length of "ssh-rsa" string (big-endian)
     * - "ssh-rsa" string (7 bytes)
     * - 4 bytes: length of exponent (big-endian)
     * - exponent bytes (big-endian, typically 3 bytes for 65537)
     * - 4 bytes: length of modulus (big-endian)
     * - modulus bytes (big-endian, 256 bytes for 2048-bit key)
     */
    fun getPublicKeyForAdb(): ByteArray {
        val key = publicKey as? RSAPublicKey ?: throw IllegalStateException("Public key not initialized")

        // Get RSA parameters
        val modulus = key.modulus
        val exponent = key.publicExponent

        // Convert to byte arrays (big-endian, no sign bit)
        val modulusBytes = stripLeadingZeros(modulus.toByteArray())
        val exponentBytes = stripLeadingZeros(exponent.toByteArray())

        Log.i(TAG, "Building SSH public key blob:")
        Log.i(TAG, "  Modulus: ${modulusBytes.size} bytes")
        Log.i(TAG, "  Exponent: ${exponentBytes.size} bytes (value: ${exponent})")

        val keyType = "ssh-rsa"
        val keyTypeBytes = keyType.toByteArray(Charsets.UTF_8)

        // Calculate total size: 4 + 7 + 4 + exp_len + 4 + mod_len
        val totalSize = 4 + keyTypeBytes.size + 4 + exponentBytes.size + 4 + modulusBytes.size
        val buffer = ByteBuffer.allocate(totalSize)
        buffer.order(ByteOrder.BIG_ENDIAN)

        // Write key type (ssh-rsa)
        buffer.putInt(keyTypeBytes.size)
        buffer.put(keyTypeBytes)

        // Write exponent (e)
        buffer.putInt(exponentBytes.size)
        buffer.put(exponentBytes)

        // Write modulus (n)
        buffer.putInt(modulusBytes.size)
        buffer.put(modulusBytes)

        // Encode as base64 and add identifier
        val sshKey = buffer.array()
        val base64Key = Base64.encodeToString(sshKey, Base64.NO_WRAP)
        val adbKey = "$base64Key $ADB_PUBLIC_KEY_NAME\u0000"

        Log.i(TAG, "SSH blob total size: ${sshKey.size} bytes")
        Log.i(TAG, "Base64 encoded: ${base64Key.length} chars")
        Log.i(TAG, "Final ADB key: ${adbKey.length} bytes (including name and null terminator)")

        // Log a preview of the key format
        val preview = if (base64Key.length > 32) "${base64Key.substring(0, 32)}..." else base64Key
        Log.i(TAG, "Public key preview: $preview")

        return adbKey.toByteArray()
    }

    /**
     * Strip leading zero bytes that BigInteger adds for positive numbers
     */
    private fun stripLeadingZeros(bytes: ByteArray): ByteArray {
        var firstNonZero = 0
        while (firstNonZero < bytes.size && bytes[firstNonZero] == 0.toByte()) {
            firstNonZero++
        }
        return if (firstNonZero > 0 && firstNonZero < bytes.size) {
            bytes.copyOfRange(firstNonZero, bytes.size)
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
