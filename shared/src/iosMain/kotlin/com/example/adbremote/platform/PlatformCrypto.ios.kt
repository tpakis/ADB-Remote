package com.example.adbremote.platform

import kotlinx.cinterop.*
import platform.CoreFoundation.*
import platform.Foundation.*
import platform.Security.*

/**
 * iOS implementation of PlatformCrypto using Security.framework.
 */
@OptIn(ExperimentalForeignApi::class)
actual class PlatformCrypto actual constructor() {

    actual fun generateRsaKeyPair(): PlatformKeyPair {
        memScoped {
            val keySize = 2048

            val privateKeyAttr = CFDictionaryCreateMutable(null, 0, null, null)
            CFDictionaryAddValue(privateKeyAttr, kSecAttrIsPermanent, kCFBooleanFalse)

            val publicKeyAttr = CFDictionaryCreateMutable(null, 0, null, null)
            CFDictionaryAddValue(publicKeyAttr, kSecAttrIsPermanent, kCFBooleanFalse)

            val keyPairAttr = CFDictionaryCreateMutable(null, 0, null, null)
            CFDictionaryAddValue(keyPairAttr, kSecAttrKeyType, kSecAttrKeyTypeRSA)
            CFDictionaryAddValue(keyPairAttr, kSecAttrKeySizeInBits, CFNumberCreate(null, kCFNumberIntType, cValuesOf(keySize)))
            CFDictionaryAddValue(keyPairAttr, kSecPrivateKeyAttrs, privateKeyAttr)
            CFDictionaryAddValue(keyPairAttr, kSecPublicKeyAttrs, publicKeyAttr)

            val errorRef = alloc<CFErrorRefVar>()
            val privateKey = SecKeyCreateRandomKey(keyPairAttr, errorRef.ptr)

            if (privateKey == null) {
                val error = errorRef.value
                val desc = if (error != null) CFErrorCopyDescription(error)?.let { CFBridgingRelease(it) as? String } else null
                throw Exception("Failed to generate RSA key pair: $desc")
            }

            val publicKey = SecKeyCopyPublicKey(privateKey)
                ?: throw Exception("Failed to get public key from private key")

            return PlatformKeyPair(
                PlatformPrivateKey(privateKey),
                PlatformPublicKey(publicKey)
            )
        }
    }

    actual fun loadKeyPair(privateKeyBytes: ByteArray, publicKeyBytes: ByteArray): PlatformKeyPair {
        memScoped {
            // Load private key from PKCS#8 format
            val privateKeyData = privateKeyBytes.toNSData()
            val privateKeyAttr = CFDictionaryCreateMutable(null, 0, null, null)
            CFDictionaryAddValue(privateKeyAttr, kSecAttrKeyType, kSecAttrKeyTypeRSA)
            CFDictionaryAddValue(privateKeyAttr, kSecAttrKeyClass, kSecAttrKeyClassPrivate)
            CFDictionaryAddValue(privateKeyAttr, kSecAttrKeySizeInBits, CFNumberCreate(null, kCFNumberIntType, cValuesOf(2048)))

            val errorRef = alloc<CFErrorRefVar>()
            val privateKey = SecKeyCreateWithData(CFBridgingRetain(privateKeyData) as CFDataRef, privateKeyAttr, errorRef.ptr)
                ?: throw Exception("Failed to load private key")

            // Load public key from X.509 format
            val publicKeyData = publicKeyBytes.toNSData()
            val publicKeyAttr = CFDictionaryCreateMutable(null, 0, null, null)
            CFDictionaryAddValue(publicKeyAttr, kSecAttrKeyType, kSecAttrKeyTypeRSA)
            CFDictionaryAddValue(publicKeyAttr, kSecAttrKeyClass, kSecAttrKeyClassPublic)
            CFDictionaryAddValue(publicKeyAttr, kSecAttrKeySizeInBits, CFNumberCreate(null, kCFNumberIntType, cValuesOf(2048)))

            val publicKey = SecKeyCreateWithData(CFBridgingRetain(publicKeyData) as CFDataRef, publicKeyAttr, errorRef.ptr)
                ?: throw Exception("Failed to load public key")

            return PlatformKeyPair(
                PlatformPrivateKey(privateKey),
                PlatformPublicKey(publicKey)
            )
        }
    }

    /**
     * Sign data using RSA with PKCS#1 v1.5 Type 1 padding and SHA-1 DigestInfo wrapper.
     */
    actual fun signWithSha1DigestInfo(data: ByteArray, privateKey: PlatformPrivateKey): ByteArray {
        // SHA-1 DigestInfo prefix (ASN.1 DER encoding)
        val digestInfoPrefix = byteArrayOf(
            0x30, 0x21,
            0x30, 0x09,
            0x06, 0x05,
            0x2b, 0x0e, 0x03, 0x02, 0x1a,
            0x05, 0x00,
            0x04, 0x14
        )

        val digestInfo = digestInfoPrefix + data

        // For ADB, we need PKCS#1 v1.5 Type 1 padding with raw RSA
        // iOS's SecKeyCreateSignature uses standard padding, so we need to do it manually

        // Key size is 256 bytes for 2048-bit key
        val keySize = 256
        val paddingLength = keySize - digestInfo.size - 3

        if (paddingLength < 8) {
            throw IllegalStateException("DigestInfo too large for key size")
        }

        // Build PKCS#1 v1.5 Type 1 padded message
        val paddedMessage = ByteArray(keySize)
        paddedMessage[0] = 0x00
        paddedMessage[1] = 0x01
        for (i in 0 until paddingLength) {
            paddedMessage[2 + i] = 0xFF.toByte()
        }
        paddedMessage[2 + paddingLength] = 0x00
        paddedMessage.indices.drop(3 + paddingLength).forEachIndexed { i, idx ->
            if (i < digestInfo.size) paddedMessage[idx] = digestInfo[i]
        }

        // Perform raw RSA operation using SecKeyCreateDecryptedData with raw mode
        // (private key "decrypt" = sign for RSA)
        memScoped {
            val inputData = paddedMessage.toNSData()
            val errorRef = alloc<CFErrorRefVar>()

            // Use raw RSA (kSecKeyAlgorithmRSAEncryptionRaw)
            val resultData = SecKeyCreateDecryptedData(
                privateKey.key,
                kSecKeyAlgorithmRSAEncryptionRaw,
                CFBridgingRetain(inputData) as CFDataRef,
                errorRef.ptr
            )

            if (resultData == null) {
                val error = errorRef.value
                val desc = if (error != null) CFErrorCopyDescription(error)?.let { CFBridgingRelease(it) as? String } else null
                throw Exception("Failed to sign: $desc")
            }

            return (CFBridgingRelease(resultData) as NSData).toByteArray()
        }
    }

    actual fun getPublicKeyModulus(publicKey: PlatformPublicKey): ByteArray {
        memScoped {
            val errorRef = alloc<CFErrorRefVar>()
            val keyData = SecKeyCopyExternalRepresentation(publicKey.key, errorRef.ptr)
                ?: throw Exception("Failed to export public key")

            val data = (CFBridgingRelease(keyData) as NSData).toByteArray()

            // The exported key is in PKCS#1 RSAPublicKey format:
            // SEQUENCE { INTEGER modulus, INTEGER exponent }
            // Parse to extract modulus
            return parseRsaPublicKeyModulus(data)
        }
    }

    actual fun getPublicKeyExponent(publicKey: PlatformPublicKey): Int {
        memScoped {
            val errorRef = alloc<CFErrorRefVar>()
            val keyData = SecKeyCopyExternalRepresentation(publicKey.key, errorRef.ptr)
                ?: throw Exception("Failed to export public key")

            val data = (CFBridgingRelease(keyData) as NSData).toByteArray()

            // Parse to extract exponent
            return parseRsaPublicKeyExponent(data)
        }
    }

    actual fun sha1(data: ByteArray): ByteArray {
        // Pure Kotlin SHA1 implementation for iOS
        return sha1Pure(data)
    }

    // Pure Kotlin SHA1 implementation
    private fun sha1Pure(data: ByteArray): ByteArray {
        val h = intArrayOf(
            0x67452301,
            0xEFCDAB89.toInt(),
            0x98BADCFE.toInt(),
            0x10325476,
            0xC3D2E1F0.toInt()
        )

        val ml = data.size.toLong() * 8
        val paddedLength = ((data.size + 9 + 63) / 64) * 64
        val padded = ByteArray(paddedLength)
        data.copyInto(padded)
        padded[data.size] = 0x80.toByte()

        // Append length in big-endian
        for (i in 0..7) {
            padded[paddedLength - 8 + i] = ((ml shr (56 - i * 8)) and 0xFF).toByte()
        }

        val w = IntArray(80)
        for (chunk in 0 until paddedLength / 64) {
            val offset = chunk * 64
            for (i in 0..15) {
                w[i] = ((padded[offset + i * 4].toInt() and 0xFF) shl 24) or
                        ((padded[offset + i * 4 + 1].toInt() and 0xFF) shl 16) or
                        ((padded[offset + i * 4 + 2].toInt() and 0xFF) shl 8) or
                        (padded[offset + i * 4 + 3].toInt() and 0xFF)
            }
            for (i in 16..79) {
                w[i] = (w[i - 3] xor w[i - 8] xor w[i - 14] xor w[i - 16]).rotateLeft(1)
            }

            var a = h[0]
            var b = h[1]
            var c = h[2]
            var d = h[3]
            var e = h[4]

            for (i in 0..79) {
                val (f, k) = when (i) {
                    in 0..19 -> ((b and c) or (b.inv() and d)) to 0x5A827999
                    in 20..39 -> (b xor c xor d) to 0x6ED9EBA1
                    in 40..59 -> ((b and c) or (b and d) or (c and d)) to 0x8F1BBCDC.toInt()
                    else -> (b xor c xor d) to 0xCA62C1D6.toInt()
                }
                val temp = a.rotateLeft(5) + f + e + k + w[i]
                e = d
                d = c
                c = b.rotateLeft(30)
                b = a
                a = temp
            }

            h[0] += a
            h[1] += b
            h[2] += c
            h[3] += d
            h[4] += e
        }

        val result = ByteArray(20)
        for (i in 0..4) {
            result[i * 4] = (h[i] shr 24).toByte()
            result[i * 4 + 1] = (h[i] shr 16).toByte()
            result[i * 4 + 2] = (h[i] shr 8).toByte()
            result[i * 4 + 3] = h[i].toByte()
        }
        return result
    }

    private fun Int.rotateLeft(bits: Int): Int = (this shl bits) or (this ushr (32 - bits))

    private fun parseRsaPublicKeyModulus(data: ByteArray): ByteArray {
        // Simple ASN.1 DER parser for PKCS#1 RSAPublicKey
        var offset = 0

        // SEQUENCE tag
        if (data[offset++] != 0x30.toByte()) throw Exception("Invalid RSA public key format")

        // Skip length (may be 1-3 bytes)
        offset += readDerLength(data, offset).second

        // INTEGER (modulus)
        if (data[offset++] != 0x02.toByte()) throw Exception("Invalid RSA public key format")

        val (modulusLen, lenBytes) = readDerLength(data, offset)
        offset += lenBytes

        // Skip leading zero if present
        var modulusStart = offset
        var modulusLength = modulusLen
        if (data[modulusStart] == 0.toByte()) {
            modulusStart++
            modulusLength--
        }

        return data.copyOfRange(modulusStart, modulusStart + modulusLength)
    }

    private fun parseRsaPublicKeyExponent(data: ByteArray): Int {
        var offset = 0

        // SEQUENCE tag
        if (data[offset++] != 0x30.toByte()) throw Exception("Invalid RSA public key format")
        offset += readDerLength(data, offset).second

        // INTEGER (modulus) - skip
        if (data[offset++] != 0x02.toByte()) throw Exception("Invalid RSA public key format")
        val (modulusLen, lenBytes) = readDerLength(data, offset)
        offset += lenBytes + modulusLen

        // INTEGER (exponent)
        if (data[offset++] != 0x02.toByte()) throw Exception("Invalid RSA public key format")
        val (expLen, expLenBytes) = readDerLength(data, offset)
        offset += expLenBytes

        var exp = 0
        for (i in 0 until expLen) {
            exp = (exp shl 8) or (data[offset + i].toInt() and 0xFF)
        }
        return exp
    }

    private fun readDerLength(data: ByteArray, offset: Int): Pair<Int, Int> {
        val firstByte = data[offset].toInt() and 0xFF
        return if (firstByte < 0x80) {
            Pair(firstByte, 1)
        } else {
            val numLenBytes = firstByte and 0x7F
            var length = 0
            for (i in 0 until numLenBytes) {
                length = (length shl 8) or (data[offset + 1 + i].toInt() and 0xFF)
            }
            Pair(length, 1 + numLenBytes)
        }
    }
}

actual class PlatformKeyPair(
    actual val privateKey: PlatformPrivateKey,
    actual val publicKey: PlatformPublicKey
) {
    @OptIn(ExperimentalForeignApi::class)
    actual fun getPrivateKeyEncoded(): ByteArray {
        memScoped {
            val errorRef = alloc<CFErrorRefVar>()
            val keyData = SecKeyCopyExternalRepresentation(privateKey.key, errorRef.ptr)
                ?: throw Exception("Failed to export private key")
            return (CFBridgingRelease(keyData) as NSData).toByteArray()
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    actual fun getPublicKeyEncoded(): ByteArray {
        memScoped {
            val errorRef = alloc<CFErrorRefVar>()
            val keyData = SecKeyCopyExternalRepresentation(publicKey.key, errorRef.ptr)
                ?: throw Exception("Failed to export public key")
            return (CFBridgingRelease(keyData) as NSData).toByteArray()
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
actual class PlatformPrivateKey(val key: SecKeyRef)

@OptIn(ExperimentalForeignApi::class)
actual class PlatformPublicKey(val key: SecKeyRef)

// Helper extensions
@OptIn(ExperimentalForeignApi::class)
private fun ByteArray.toNSData(): NSData {
    return this.usePinned { pinned ->
        NSData.dataWithBytes(pinned.addressOf(0), this.size.toULong())
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun NSData.toByteArray(): ByteArray {
    val size = this.length.toInt()
    val bytes = ByteArray(size)
    bytes.usePinned { pinned ->
        this.getBytes(pinned.addressOf(0), size.toULong())
    }
    return bytes
}
