package com.example.adbremote.platform

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.Foundation.*

@OptIn(ExperimentalForeignApi::class)
actual class PlatformFileSaver {
    actual suspend fun saveTextFile(
        defaultFileName: String,
        content: String,
        onResult: (path: String?) -> Unit
    ) {
        withContext(Dispatchers.Default) {
            try {
                // Save to Documents folder
                val paths = NSSearchPathForDirectoriesInDomains(
                    NSDocumentDirectory,
                    NSUserDomainMask,
                    true
                )
                val documentsDir = paths.firstOrNull() as? String

                if (documentsDir == null) {
                    withContext(Dispatchers.Main) {
                        onResult(null)
                    }
                    return@withContext
                }

                var filePath = "$documentsDir/$defaultFileName"

                // If file exists, add timestamp to avoid overwriting
                val fileManager = NSFileManager.defaultManager
                if (fileManager.fileExistsAtPath(filePath)) {
                    val timestamp = NSDate().timeIntervalSince1970.toLong()
                    val nameWithoutExt = defaultFileName.substringBeforeLast(".")
                    val ext = defaultFileName.substringAfterLast(".", "")
                    filePath = "$documentsDir/${nameWithoutExt}_${timestamp}.${ext}"
                }

                val nsString = content as NSString
                val success = nsString.writeToFile(
                    filePath,
                    atomically = true,
                    encoding = NSUTF8StringEncoding,
                    error = null
                )

                withContext(Dispatchers.Main) {
                    if (success) {
                        onResult(filePath)
                    } else {
                        onResult(null)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onResult(null)
                }
            }
        }
    }
}

actual object PlatformFileSaverFactory {
    actual fun create(context: PlatformContext): PlatformFileSaver {
        return PlatformFileSaver()
    }
}
