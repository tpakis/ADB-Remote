package com.example.adbremote.platform

import android.content.Context
import android.net.Uri

/**
 * Android file picker that uses Storage Access Framework.
 * The launcher function must be provided from the Composable layer
 * using rememberLauncherForActivityResult.
 */
actual class PlatformFilePicker(
    private val context: Context,
    private val launchPicker: ((Array<String>, (Uri?) -> Unit) -> Unit)?
) {
    private var pendingCallback: ((String?) -> Unit)? = null

    actual fun pickFile(
        mimeType: String,
        onResult: (path: String?) -> Unit
    ) {
        val launcher = launchPicker
        if (launcher == null) {
            PlatformLogger.e("PlatformFilePicker", "Launcher not set")
            onResult(null)
            return
        }

        launcher(arrayOf(mimeType)) { uri ->
            if (uri != null) {
                // Get the actual file path or use content URI
                val path = getPathFromUri(uri)
                onResult(path)
            } else {
                onResult(null)
            }
        }
    }

    private fun getPathFromUri(uri: Uri): String? {
        // For content:// URIs, we need to copy to a temp file or use the URI directly
        // Since adb install needs a local path, we'll copy the file to cache
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            if (inputStream == null) {
                PlatformLogger.e("PlatformFilePicker", "Cannot open input stream for URI: $uri")
                return null
            }

            // Get filename from URI
            val fileName = getFileName(uri) ?: "temp_${System.currentTimeMillis()}.apk"
            val tempFile = java.io.File(context.cacheDir, fileName)

            inputStream.use { input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            PlatformLogger.i("PlatformFilePicker", "Copied file to: ${tempFile.absolutePath}")
            tempFile.absolutePath
        } catch (e: Exception) {
            PlatformLogger.e("PlatformFilePicker", "Failed to copy file from URI", e)
            null
        }
    }

    private fun getFileName(uri: Uri): String? {
        var name: String? = null
        if (uri.scheme == "content") {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        name = it.getString(nameIndex)
                    }
                }
            }
        }
        if (name == null) {
            name = uri.path?.substringAfterLast('/')
        }
        return name
    }
}

actual object PlatformFilePickerFactory {
    actual fun create(context: PlatformContext): PlatformFilePicker {
        // This creates a picker without a launcher - launcher must be set later
        return PlatformFilePicker(context.androidContext, null)
    }

    /**
     * Create a file picker with a launcher function from Compose.
     */
    fun createWithLauncher(
        context: Context,
        launchPicker: (Array<String>, (Uri?) -> Unit) -> Unit
    ): PlatformFilePicker {
        return PlatformFilePicker(context, launchPicker)
    }
}

/**
 * Read file bytes from a local file path.
 */
actual fun readFileBytes(path: String): ByteArray? {
    return try {
        java.io.File(path).readBytes()
    } catch (e: Exception) {
        PlatformLogger.e("readFileBytes", "Failed to read file: $path", e)
        null
    }
}
