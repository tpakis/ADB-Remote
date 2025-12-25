package com.example.adbremote.platform

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import java.io.File

/**
 * Android implementation of PlatformStorage using SharedPreferences for strings
 * and files for binary data.
 */
actual class PlatformStorage(
    private val prefs: SharedPreferences,
    private val filesDir: File
) {
    actual fun saveString(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }

    actual fun getString(key: String): String? {
        return prefs.getString(key, null)
    }

    actual fun saveBytes(key: String, data: ByteArray) {
        // For binary data, save to file
        val file = File(filesDir, key)
        file.writeBytes(data)
    }

    actual fun getBytes(key: String): ByteArray? {
        val file = File(filesDir, key)
        return if (file.exists()) {
            file.readBytes()
        } else {
            null
        }
    }

    actual fun remove(key: String) {
        prefs.edit().remove(key).apply()
        // Also try to remove file if exists
        val file = File(filesDir, key)
        if (file.exists()) {
            file.delete()
        }
    }

    actual fun clear() {
        prefs.edit().clear().apply()
    }
}

/**
 * Android implementation of PlatformStorageFactory.
 */
actual object PlatformStorageFactory {
    private var context: Context? = null

    fun initialize(context: Context) {
        this.context = context.applicationContext
    }

    actual fun create(name: String): PlatformStorage {
        val ctx = context ?: throw IllegalStateException("PlatformStorageFactory not initialized. Call initialize() first.")
        val prefs = ctx.getSharedPreferences(name, Context.MODE_PRIVATE)
        return PlatformStorage(prefs, ctx.filesDir)
    }
}
