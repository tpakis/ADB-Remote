package com.example.adbremote.adb

import java.math.BigInteger

/**
 * Android/JVM implementation using java.math.BigInteger for efficient modular arithmetic.
 */
actual fun calculateMontgomeryConstants(modulusBytes: ByteArray, len: Int): MontgomeryData {
    // Convert modulus to BigInteger (big-endian, unsigned)
    val n = BigInteger(1, modulusBytes)

    // Calculate n0inv = -(1/n[0]) mod 2^32
    // n[0] is the least significant 32 bits of n
    val n0 = n.and(BigInteger.valueOf(0xFFFFFFFFL))
    val twoTo32 = BigInteger.ONE.shiftLeft(32)
    val n0inv = n0.modInverse(twoTo32).negate().mod(twoTo32).toInt()

    // Calculate R^2 mod n where R = 2^(len*32)
    val r = BigInteger.ONE.shiftLeft(len * 32)
    val rr = r.multiply(r).mod(n)

    // Convert modulus to little-endian uint32_t array (as bytes)
    val modulusLE = ByteArray(len * 4)
    for (i in 0 until len) {
        val word = n.shiftRight(i * 32).and(BigInteger.valueOf(0xFFFFFFFFL)).toInt()
        modulusLE[i * 4] = (word and 0xFF).toByte()
        modulusLE[i * 4 + 1] = ((word shr 8) and 0xFF).toByte()
        modulusLE[i * 4 + 2] = ((word shr 16) and 0xFF).toByte()
        modulusLE[i * 4 + 3] = ((word shr 24) and 0xFF).toByte()
    }

    // Convert R^2 mod n to little-endian uint32_t array (as bytes)
    val rrLE = ByteArray(len * 4)
    for (i in 0 until len) {
        val word = rr.shiftRight(i * 32).and(BigInteger.valueOf(0xFFFFFFFFL)).toInt()
        rrLE[i * 4] = (word and 0xFF).toByte()
        rrLE[i * 4 + 1] = ((word shr 8) and 0xFF).toByte()
        rrLE[i * 4 + 2] = ((word shr 16) and 0xFF).toByte()
        rrLE[i * 4 + 3] = ((word shr 24) and 0xFF).toByte()
    }

    return MontgomeryData(n0inv, modulusLE, rrLE)
}
