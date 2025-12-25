package com.example.adbremote.platform

import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.PublicKey
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher

actual class PlatformCrypto actual constructor() {

    actual fun generateRsaKeyPair(): PlatformKeyPair {
        val keyPairGenerator = KeyPairGenerator.getInstance("RSA")
        keyPairGenerator.initialize(2048)
        val keyPair = keyPairGenerator.generateKeyPair()
        return PlatformKeyPair(
            PlatformPrivateKey(keyPair.private),
            PlatformPublicKey(keyPair.public)
        )
    }

    actual fun loadKeyPair(privateKeyBytes: ByteArray, publicKeyBytes: ByteArray): PlatformKeyPair {
        val keyFactory = KeyFactory.getInstance("RSA")

        val privateKeySpec = PKCS8EncodedKeySpec(privateKeyBytes)
        val privateKey = keyFactory.generatePrivate(privateKeySpec)

        val publicKeySpec = X509EncodedKeySpec(publicKeyBytes)
        val publicKey = keyFactory.generatePublic(publicKeySpec)

        return PlatformKeyPair(
            PlatformPrivateKey(privateKey),
            PlatformPublicKey(publicKey)
        )
    }

    /**
     * Sign data using RSA with PKCS#1 v1.5 Type 1 padding and SHA-1 DigestInfo wrapper.
     */
    actual fun signWithSha1DigestInfo(data: ByteArray, privateKey: PlatformPrivateKey): ByteArray {
        val rsaKey = privateKey.key as RSAPrivateKey

        // SHA-1 DigestInfo prefix (ASN.1 DER encoding)
        val digestInfoPrefix = byteArrayOf(
            0x30, 0x21,                         // SEQUENCE, length 33
            0x30, 0x09,                         // SEQUENCE, length 9
            0x06, 0x05,                         // OID, length 5
            0x2b, 0x0e, 0x03, 0x02, 0x1a,      // SHA-1 OID: 1.3.14.3.2.26
            0x05, 0x00,                         // NULL
            0x04, 0x14                          // OCTET STRING, length 20
        )

        // Build DigestInfo: prefix + data (the data IS the SHA-1 hash from ADB)
        val digestInfo = digestInfoPrefix + data

        // Key size in bytes (256 for 2048-bit key)
        val keySize = (rsaKey.modulus.bitLength() + 7) / 8

        // Build PKCS#1 v1.5 Type 1 padded message
        // Format: 0x00 || 0x01 || 0xFF...0xFF || 0x00 || DigestInfo
        val paddingLength = keySize - digestInfo.size - 3

        if (paddingLength < 8) {
            throw IllegalStateException("DigestInfo too large for key size")
        }

        val paddedMessage = ByteArray(keySize)
        paddedMessage[0] = 0x00
        paddedMessage[1] = 0x01
        for (i in 0 until paddingLength) {
            paddedMessage[2 + i] = 0xFF.toByte()
        }
        paddedMessage[2 + paddingLength] = 0x00
        System.arraycopy(digestInfo, 0, paddedMessage, 3 + paddingLength, digestInfo.size)

        // Perform raw RSA operation (private key encrypt = sign)
        val cipher = Cipher.getInstance("RSA/ECB/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, privateKey.key)
        return cipher.doFinal(paddedMessage)
    }

    actual fun getPublicKeyModulus(publicKey: PlatformPublicKey): ByteArray {
        val rsaKey = publicKey.key as RSAPublicKey
        val modulusBytes = rsaKey.modulus.toByteArray()
        // Remove leading zero if present (BigInteger sign extension)
        return if (modulusBytes[0] == 0.toByte() && modulusBytes.size > 256) {
            modulusBytes.copyOfRange(1, modulusBytes.size)
        } else {
            modulusBytes
        }
    }

    actual fun getPublicKeyExponent(publicKey: PlatformPublicKey): Int {
        val rsaKey = publicKey.key as RSAPublicKey
        return rsaKey.publicExponent.toInt()
    }

    actual fun sha1(data: ByteArray): ByteArray {
        val md = MessageDigest.getInstance("SHA-1")
        return md.digest(data)
    }
}

actual class PlatformKeyPair(
    actual val privateKey: PlatformPrivateKey,
    actual val publicKey: PlatformPublicKey
) {
    actual fun getPrivateKeyEncoded(): ByteArray = privateKey.key.encoded
    actual fun getPublicKeyEncoded(): ByteArray = publicKey.key.encoded
}

actual class PlatformPrivateKey(val key: PrivateKey)

actual class PlatformPublicKey(val key: PublicKey)
