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
        withContext(Dispatchers.Main) {
            try {
                val dialog = FileDialog(null as Frame?, "Save File", FileDialog.SAVE)
                dialog.file = defaultFileName
                dialog.isVisible = true

                val directory = dialog.directory
                val filename = dialog.file

                if (directory != null && filename != null) {
                    val file = File(directory, filename)
                    withContext(Dispatchers.IO) {
                        file.writeText(content)
                    }
                    onResult(file.absolutePath)
                } else {
                    onResult(null)
                }
            } catch (e: Exception) {
                PlatformLogger.e("PlatformFileSaver", "Failed to save file", e)
                onResult(null)
            }
        }
    }
}

actual object PlatformFileSaverFactory {
    actual fun create(context: PlatformContext): PlatformFileSaver {
        return PlatformFileSaver()
    }
}
