package com.example.adbremote.platform

/**
 * Platform-specific storage for persisting data.
 * Implementations should provide thread-safe access to storage.
 */
expect class PlatformStorage {
    /**
     * Save a string value.
     */
    fun saveString(key: String, value: String)

    /**
     * Get a string value, or null if not found.
     */
    fun getString(key: String): String?

    /**
     * Save binary data (e.g., for RSA keys).
     */
    fun saveBytes(key: String, data: ByteArray)

    /**
     * Get binary data, or null if not found.
     */
    fun getBytes(key: String): ByteArray?

    /**
     * Remove a value by key.
     */
    fun remove(key: String)

    /**
     * Clear all stored data.
     */
    fun clear()
}

/**
 * Factory to create platform storage instances.
 * Platform implementations will provide concrete factory methods.
 */
expect object PlatformStorageFactory {
    /**
     * Create a storage instance for the given name/namespace.
     */
    fun create(name: String): PlatformStorage
}
