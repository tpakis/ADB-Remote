package com.example.adbremote.platform

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
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
        val savedPath = withContext(Dispatchers.Default) {
            try {
                // Save to Documents folder
                val paths = NSSearchPathForDirectoriesInDomains(
                    NSDocumentDirectory,
                    NSUserDomainMask,
                    true
                )
                val documentsDir = paths.firstOrNull() as? String ?: return@withContext null

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

                if (success) filePath else null
            } catch (e: Exception) {
                null
            }
        }
        // Call callback directly after work is done
        onResult(savedPath)
    }

    @OptIn(kotlinx.cinterop.BetaInteropApi::class)
    actual suspend fun saveBinaryFile(
        defaultFileName: String,
        content: ByteArray,
        mimeType: String,
        onResult: (path: String?) -> Unit
    ) {
        val savedPath = withContext(Dispatchers.Default) {
            try {
                // Save to Documents folder
                val paths = NSSearchPathForDirectoriesInDomains(
                    NSDocumentDirectory,
                    NSUserDomainMask,
                    true
                )
                val documentsDir = paths.firstOrNull() as? String ?: return@withContext null

                var filePath = "$documentsDir/$defaultFileName"

                // If file exists, add timestamp to avoid overwriting
                val fileManager = NSFileManager.defaultManager
                if (fileManager.fileExistsAtPath(filePath)) {
                    val timestamp = NSDate().timeIntervalSince1970.toLong()
                    val nameWithoutExt = defaultFileName.substringBeforeLast(".")
                    val ext = defaultFileName.substringAfterLast(".", "")
                    filePath = "$documentsDir/${nameWithoutExt}_${timestamp}.${ext}"
                }

                // Convert ByteArray to NSData
                val nsData = content.usePinned { pinned ->
                    NSData.dataWithBytes(pinned.addressOf(0), content.size.toULong())
                }

                val success = nsData.writeToFile(filePath, atomically = true)

                if (success) filePath else null
            } catch (e: Exception) {
                null
            }
        }
        // Call callback directly after work is done
        onResult(savedPath)
    }
}

actual object PlatformFileSaverFactory {
    actual fun create(context: PlatformContext): PlatformFileSaver {
        return PlatformFileSaver()
    }
}
