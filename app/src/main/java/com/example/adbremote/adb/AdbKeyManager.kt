package com.example.adbremote.adb

import android.content.Context
import android.util.Base64
import android.util.Log
import java.io.File
import java.security.*
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
        if (!loadKeys()) {
            Log.d(TAG, "No existing keys found, generating new RSA key pair")
            generateKeys()
            saveKeys()
        } else {
            Log.d(TAG, "Loaded existing RSA keys")
        }
    }

    /**
     * Sign the authentication token from the ADB server
     */
    fun signToken(token: ByteArray): ByteArray? {
        return try {
            val key = privateKey ?: return null
            val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
            cipher.init(Cipher.ENCRYPT_MODE, key)
            cipher.doFinal(token)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sign token", e)
            null
        }
    }

    /**
     * Get the public key in ADB format
     * Format: base64(public_key) + " " + name + "\0"
     */
    fun getPublicKeyForAdb(): ByteArray {
        val key = publicKey ?: throw IllegalStateException("Public key not initialized")

        // Encode the public key in the format ADB expects
        val encodedKey = key.encoded
        val base64Key = Base64.encodeToString(encodedKey, Base64.NO_WRAP)
        val adbKey = "$base64Key $ADB_PUBLIC_KEY_NAME\u0000"

        return adbKey.toByteArray()
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
