package com.example.adbremote.platform

/**
 * Platform-specific RSA cryptography for ADB authentication.
 */
expect class PlatformCrypto() {
    /**
     * Generate a new 2048-bit RSA key pair.
     */
    fun generateRsaKeyPair(): PlatformKeyPair

    /**
     * Load a key pair from encoded bytes.
     * @param privateKeyBytes PKCS#8 encoded private key
     * @param publicKeyBytes X.509 encoded public key
     */
    fun loadKeyPair(privateKeyBytes: ByteArray, publicKeyBytes: ByteArray): PlatformKeyPair

    /**
     * Sign data using RSA with PKCS#1 v1.5 Type 1 padding and SHA-1 DigestInfo wrapper.
     * This is the specific signature format required by ADB.
     *
     * The signing process:
     * 1. Wrap the token in SHA-1 DigestInfo ASN.1 structure
     * 2. Apply PKCS#1 v1.5 Type 1 padding: 0x00 || 0x01 || 0xFF... || 0x00 || DigestInfo(token)
     * 3. Perform raw RSA operation (modular exponentiation)
     */
    fun signWithSha1DigestInfo(data: ByteArray, privateKey: PlatformPrivateKey): ByteArray

    /**
     * Get the modulus of an RSA public key as a byte array.
     */
    fun getPublicKeyModulus(publicKey: PlatformPublicKey): ByteArray

    /**
     * Get the public exponent of an RSA public key.
     */
    fun getPublicKeyExponent(publicKey: PlatformPublicKey): Int

    /**
     * Compute SHA-1 hash of the data.
     */
    fun sha1(data: ByteArray): ByteArray
}

/**
 * Platform-specific RSA key pair.
 */
expect class PlatformKeyPair {
    val privateKey: PlatformPrivateKey
    val publicKey: PlatformPublicKey

    /**
     * Get the private key in PKCS#8 encoded format.
     */
    fun getPrivateKeyEncoded(): ByteArray

    /**
     * Get the public key in X.509 encoded format.
     */
    fun getPublicKeyEncoded(): ByteArray
}

/**
 * Platform-specific RSA private key.
 */
expect class PlatformPrivateKey

/**
 * Platform-specific RSA public key.
 */
expect class PlatformPublicKey
