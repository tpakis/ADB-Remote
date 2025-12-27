package com.example.adbremote.platform

import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

actual class PlatformFileSaver {
    actual suspend fun saveTextFile(
        defaultFileName: String,
        content: String,
        onResult: (path: String?) -> Unit
    ) {
        saveBinaryFile(defaultFileName, content.toByteArray(), "text/plain", onResult)
    }

    actual suspend fun saveBinaryFile(
        defaultFileName: String,
        content: ByteArray,
        mimeType: String,
        onResult: (path: String?) -> Unit
    ) {
        val savedPath = try {
            // Show file dialog on AWT thread (this blocks until user selects)
            val (directory, filename) = withContext(Dispatchers.Main) {
                val dialog = FileDialog(null as Frame?, "Save File", FileDialog.SAVE)
                dialog.file = defaultFileName
                dialog.isVisible = true
                dialog.directory to dialog.file
            }

            if (directory != null && filename != null) {
                val file = File(directory, filename)
                withContext(Dispatchers.IO) {
                    file.writeBytes(content)
                }
                file.absolutePath
            } else {
                null
            }
        } catch (e: Exception) {
            PlatformLogger.e("PlatformFileSaver", "Failed to save file", e)
            null
        }
        // Call callback directly after all work is done
        onResult(savedPath)
    }
}

actual object PlatformFileSaverFactory {
    actual fun create(context: PlatformContext): PlatformFileSaver {
        return PlatformFileSaver()
    }
}
