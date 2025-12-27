package com.example.adbremote.platform

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

actual class PlatformFileSaver(private val context: Context) {
    actual suspend fun saveTextFile(
        defaultFileName: String,
        content: String,
        onResult: (path: String?) -> Unit
    ) {
        val savedPath = withContext(Dispatchers.IO) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    // Android 10+ (API 29+): Use MediaStore - no permissions needed
                    saveWithMediaStore(defaultFileName, content.toByteArray(), "text/plain")
                } else {
                    // Android 9 and below: Use legacy approach (requires WRITE_EXTERNAL_STORAGE)
                    saveWithLegacyStorage(defaultFileName, content.toByteArray())
                }
            } catch (e: Exception) {
                PlatformLogger.e("PlatformFileSaver", "Failed to save file", e)
                null
            }
        }
        // Call callback directly - caller handles threading via CompletableDeferred
        onResult(savedPath)
    }

    actual suspend fun saveBinaryFile(
        defaultFileName: String,
        content: ByteArray,
        mimeType: String,
        onResult: (path: String?) -> Unit
    ) {
        val savedPath = withContext(Dispatchers.IO) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    saveWithMediaStore(defaultFileName, content, mimeType)
                } else {
                    saveWithLegacyStorage(defaultFileName, content)
                }
            } catch (e: Exception) {
                PlatformLogger.e("PlatformFileSaver", "Failed to save binary file", e)
                null
            }
        }
        // Call callback directly - caller handles threading via CompletableDeferred
        onResult(savedPath)
    }

    /**
     * Save file using MediaStore API (Android 10+)
     * No permissions required for Downloads folder
     */
    private fun saveWithMediaStore(defaultFileName: String, content: ByteArray, mimeType: String): String? {
        val resolver = context.contentResolver

        // Generate unique filename if needed
        val actualFileName = generateUniqueFileName(defaultFileName)

        val contentValues = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, actualFileName)
            put(MediaStore.Downloads.MIME_TYPE, mimeType)
            put(MediaStore.Downloads.IS_PENDING, 1)
        }

        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
        if (uri == null) {
            PlatformLogger.e("PlatformFileSaver", "Failed to create MediaStore entry")
            return null
        }

        resolver.openOutputStream(uri)?.use { outputStream ->
            outputStream.write(content)
        }

        // Mark as complete
        contentValues.clear()
        contentValues.put(MediaStore.Downloads.IS_PENDING, 0)
        resolver.update(uri, contentValues, null, null)

        val path = "${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)}/$actualFileName"
        PlatformLogger.i("PlatformFileSaver", "File saved to: $path")
        return path
    }

    /**
     * Save file using legacy storage API (Android 9 and below)
     * Requires WRITE_EXTERNAL_STORAGE permission
     */
    @Suppress("DEPRECATION")
    private fun saveWithLegacyStorage(defaultFileName: String, content: ByteArray): String? {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (!downloadsDir.exists()) {
            downloadsDir.mkdirs()
        }

        val actualFileName = generateUniqueFileName(defaultFileName)
        val file = File(downloadsDir, actualFileName)

        file.writeBytes(content)
        PlatformLogger.i("PlatformFileSaver", "File saved to: ${file.absolutePath}")
        return file.absolutePath
    }

    /**
     * Generate a unique filename by adding timestamp if file already exists
     */
    private fun generateUniqueFileName(defaultFileName: String): String {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val file = File(downloadsDir, defaultFileName)

        return if (file.exists()) {
            val timestamp = System.currentTimeMillis()
            val nameWithoutExt = defaultFileName.substringBeforeLast(".")
            val ext = defaultFileName.substringAfterLast(".", "")
            "${nameWithoutExt}_${timestamp}.${ext}"
        } else {
            defaultFileName
        }
    }
}

actual object PlatformFileSaverFactory {
    actual fun create(context: PlatformContext): PlatformFileSaver {
        return PlatformFileSaver(context.androidContext)
    }
}
