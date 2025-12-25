package com.example.adbremote.platform

import kotlinx.cinterop.*
import platform.Foundation.*

/**
 * iOS implementation of PlatformStorage using NSUserDefaults for strings
 * and files in Documents directory for binary data.
 */
@OptIn(ExperimentalForeignApi::class)
actual class PlatformStorage(
    private val defaults: NSUserDefaults,
    private val dataDir: String,
    private val prefix: String
) {
    init {
        val fileManager = NSFileManager.defaultManager
        if (!fileManager.fileExistsAtPath(dataDir)) {
            fileManager.createDirectoryAtPath(dataDir, true, null, null)
        }
    }

    actual fun saveString(key: String, value: String) {
        defaults.setObject(value, "$prefix$key")
        defaults.synchronize()
    }

    actual fun getString(key: String): String? {
        return defaults.stringForKey("$prefix$key")
    }

    actual fun saveBytes(key: String, data: ByteArray) {
        val filePath = "$dataDir/$key"
        val nsData = data.toNSData()
        nsData.writeToFile(filePath, true)
    }

    actual fun getBytes(key: String): ByteArray? {
        val filePath = "$dataDir/$key"
        val fileManager = NSFileManager.defaultManager
        if (!fileManager.fileExistsAtPath(filePath)) {
            return null
        }
        val nsData = NSData.dataWithContentsOfFile(filePath) ?: return null
        return nsData.toByteArray()
    }

    actual fun remove(key: String) {
        defaults.removeObjectForKey("$prefix$key")
        defaults.synchronize()

        val filePath = "$dataDir/$key"
        val fileManager = NSFileManager.defaultManager
        if (fileManager.fileExistsAtPath(filePath)) {
            fileManager.removeItemAtPath(filePath, null)
        }
    }

    actual fun clear() {
        // Clear all keys with our prefix
        val dict = defaults.dictionaryRepresentation()
        for (key in dict.keys) {
            if ((key as? String)?.startsWith(prefix) == true) {
                defaults.removeObjectForKey(key)
            }
        }
        defaults.synchronize()
    }
}

actual object PlatformStorageFactory {
    private val documentsDir: String by lazy {
        val paths = NSSearchPathForDirectoriesInDomains(
            NSDocumentDirectory,
            NSUserDomainMask,
            true
        )
        (paths.firstOrNull() as? String) ?: ""
    }

    actual fun create(name: String): PlatformStorage {
        val defaults = NSUserDefaults.standardUserDefaults
        val dataDir = "$documentsDir/adb-remote/$name"
        return PlatformStorage(defaults, dataDir, "adb_remote_${name}_")
    }
}

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
