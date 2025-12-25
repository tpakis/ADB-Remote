package com.example.adbremote.adb

/**
 * iOS implementation using pure Kotlin big integer arithmetic.
 * Optimized for the specific operations needed (modular arithmetic with 2048-bit numbers).
 */
actual fun calculateMontgomeryConstants(modulusBytes: ByteArray, len: Int): MontgomeryData {
    // Convert modulus bytes (big-endian) to little-endian uint32 array
    val n = UIntArray(len)
    for (i in 0 until len) {
        val byteIndex = modulusBytes.size - 1 - (i * 4)
        var word = 0u
        if (byteIndex >= 0) word = word or ((modulusBytes[byteIndex].toUInt() and 0xFFu))
        if (byteIndex - 1 >= 0) word = word or ((modulusBytes[byteIndex - 1].toUInt() and 0xFFu) shl 8)
        if (byteIndex - 2 >= 0) word = word or ((modulusBytes[byteIndex - 2].toUInt() and 0xFFu) shl 16)
        if (byteIndex - 3 >= 0) word = word or ((modulusBytes[byteIndex - 3].toUInt() and 0xFFu) shl 24)
        n[i] = word
    }

    // Calculate n0inv = -(1/n[0]) mod 2^32 using extended Euclidean algorithm
    val n0inv = calculateN0Inv(n[0])

    // Calculate R^2 mod n using Montgomery reduction-friendly method
    val rr = calculateRRModN(n, len)

    // Convert to byte arrays
    val modulusLE = uintArrayToByteArray(n)
    val rrLE = uintArrayToByteArray(rr)

    return MontgomeryData(n0inv, modulusLE, rrLE)
}

/**
 * Calculate n0inv = -(1/n0) mod 2^32
 * Uses Newton's method for modular inverse
 */
private fun calculateN0Inv(n0: UInt): Int {
    // Newton's method: x_{i+1} = x_i * (2 - n * x_i) mod 2^32
    // Converges in log2(32) = 5 iterations starting from x = 1
    var x = 1u
    repeat(5) {
        x = x * (2u - n0 * x)
    }
    // Return -x mod 2^32
    return (0u - x).toInt()
}

/**
 * Calculate R^2 mod n where R = 2^(len*32)
 * Uses repeated squaring and reduction
 */
private fun calculateRRModN(n: UIntArray, len: Int): UIntArray {
    // Start with R mod n, then square to get R^2 mod n
    // R mod n = 2^(len*32) mod n

    // First compute 2^(len*32) mod n by starting with 1 and doubling len*32 times
    // But that's expensive. Instead, we use: R mod n = 2^(len*32) - n * floor(2^(len*32) / n)
    // Since R > n (R is 2^2048 and n is a 2048-bit number), R mod n = R - n * q

    // Simpler approach: compute R mod n by noting that:
    // 2^(len*32) mod n = (2^(len*32) - n) if n's high bit isn't set
    // For a proper 2048-bit modulus, we need to compute properly

    // Let's use the standard approach: start with 2^k mod n where k is small,
    // then repeatedly square and reduce

    // Actually, the most reliable method is:
    // 1. Compute 2^(2*len*32) mod n by repeated squaring

    var result = UIntArray(len)

    // Start with 1
    result[0] = 1u

    // Square (len * 32 * 2) times, taking mod n each time
    // This computes 2^(len*32*2) mod n = R^2 mod n
    repeat(len * 32 * 2) {
        // Double the number (shift left by 1)
        var carry = 0u
        for (i in 0 until len) {
            val newVal = (result[i] shl 1) or carry
            carry = result[i] shr 31
            result[i] = newVal
        }

        // If result >= n, subtract n
        if (carry != 0u || compareUIntArrays(result, n) >= 0) {
            subtractInPlace(result, n)
        }
    }

    return result
}

/**
 * Compare two UIntArrays (little-endian)
 * Returns: negative if a < b, 0 if a == b, positive if a > b
 */
private fun compareUIntArrays(a: UIntArray, b: UIntArray): Int {
    for (i in a.size - 1 downTo 0) {
        if (a[i] > b[i]) return 1
        if (a[i] < b[i]) return -1
    }
    return 0
}

/**
 * Subtract b from a in place (a = a - b), assuming a >= b
 */
private fun subtractInPlace(a: UIntArray, b: UIntArray) {
    var borrow = 0uL
    for (i in a.indices) {
        val aVal = a[i].toULong()
        val bVal = b[i].toULong()
        val diff = aVal - bVal - borrow
        a[i] = diff.toUInt()
        // If we borrowed (underflow occurred), the high bits will be set
        borrow = if (aVal < bVal + borrow) 1uL else 0uL
    }
}

/**
 * Convert UIntArray to ByteArray (little-endian)
 */
private fun uintArrayToByteArray(arr: UIntArray): ByteArray {
    val result = ByteArray(arr.size * 4)
    for (i in arr.indices) {
        val word = arr[i]
        result[i * 4] = (word and 0xFFu).toByte()
        result[i * 4 + 1] = ((word shr 8) and 0xFFu).toByte()
        result[i * 4 + 2] = ((word shr 16) and 0xFFu).toByte()
        result[i * 4 + 3] = ((word shr 24) and 0xFFu).toByte()
    }
    return result
}
