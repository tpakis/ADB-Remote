package com.example.adbremote.platform

import java.awt.FileDialog
import java.awt.Frame
import java.io.FilenameFilter

actual class PlatformFilePicker {
    actual fun pickFile(
        mimeType: String,
        onResult: (path: String?) -> Unit
    ) {
        try {
            val dialog = FileDialog(null as Frame?, "Select File", FileDialog.LOAD)

            // Set file filter based on mime type
            when (mimeType) {
                "application/vnd.android.package-archive" -> {
                    dialog.filenameFilter = FilenameFilter { _, name ->
                        name.lowercase().endsWith(".apk")
                    }
                }
            }

            dialog.isVisible = true

            val directory = dialog.directory
            val filename = dialog.file

            if (directory != null && filename != null) {
                onResult("$directory$filename")
            } else {
                onResult(null)
            }
        } catch (e: Exception) {
            PlatformLogger.e("PlatformFilePicker", "Failed to pick file", e)
            onResult(null)
        }
    }
}

actual object PlatformFilePickerFactory {
    actual fun create(context: PlatformContext): PlatformFilePicker {
        return PlatformFilePicker()
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
