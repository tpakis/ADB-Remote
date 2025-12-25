package com.example.adbremote.platform

import java.io.File
import java.util.prefs.Preferences

/**
 * Desktop implementation of PlatformStorage using Java Preferences for strings
 * and files in user home for binary data.
 */
actual class PlatformStorage(
    private val prefs: Preferences,
    private val dataDir: File
) {
    init {
        if (!dataDir.exists()) {
            dataDir.mkdirs()
        }
    }

    actual fun saveString(key: String, value: String) {
        prefs.put(key, value)
        prefs.flush()
    }

    actual fun getString(key: String): String? {
        return prefs.get(key, null)
    }

    actual fun saveBytes(key: String, data: ByteArray) {
        val file = File(dataDir, key)
        file.writeBytes(data)
    }

    actual fun getBytes(key: String): ByteArray? {
        val file = File(dataDir, key)
        return if (file.exists()) {
            file.readBytes()
        } else {
            null
        }
    }

    actual fun remove(key: String) {
        prefs.remove(key)
        prefs.flush()
        val file = File(dataDir, key)
        if (file.exists()) {
            file.delete()
        }
    }

    actual fun clear() {
        prefs.clear()
        prefs.flush()
    }
}

actual object PlatformStorageFactory {
    private val dataDir: File by lazy {
        val userHome = System.getProperty("user.home")
        val appDataDir = File(userHome, ".adb-remote")
        if (!appDataDir.exists()) {
            appDataDir.mkdirs()
        }
        appDataDir
    }

    actual fun create(name: String): PlatformStorage {
        val prefs = Preferences.userRoot().node("adb-remote/$name")
        val storageDir = File(dataDir, name)
        return PlatformStorage(prefs, storageDir)
    }
}
