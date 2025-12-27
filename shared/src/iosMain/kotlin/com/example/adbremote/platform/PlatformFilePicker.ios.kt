package com.example.adbremote.platform

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.*
import platform.UIKit.*
import platform.UniformTypeIdentifiers.UTTypeData
import platform.darwin.NSObject

/**
 * iOS file picker implementation using UIDocumentPickerViewController.
 */
@OptIn(ExperimentalForeignApi::class)
actual class PlatformFilePicker {

    actual fun pickFile(
        mimeType: String,
        onResult: (path: String?) -> Unit
    ) {
        // Get the root view controller
        val keyWindow = UIApplication.sharedApplication.keyWindow
        val rootViewController = keyWindow?.rootViewController

        if (rootViewController == null) {
            PlatformLogger.e("PlatformFilePicker", "No root view controller found")
            onResult(null)
            return
        }

        // Create delegate to handle picker results
        val delegate = DocumentPickerDelegate(onResult)

        // Create document picker - use UTTypeData for generic binary files (APK)
        val picker = UIDocumentPickerViewController(
            forOpeningContentTypes = listOf(UTTypeData)
        )

        picker.delegate = delegate
        picker.allowsMultipleSelection = false

        // Present the picker
        rootViewController.presentViewController(picker, animated = true, completion = null)
    }
}

/**
 * Delegate class to handle UIDocumentPickerViewController callbacks.
 */
@OptIn(ExperimentalForeignApi::class)
private class DocumentPickerDelegate(
    private val onResult: (String?) -> Unit
) : NSObject(), UIDocumentPickerDelegateProtocol {

    override fun documentPicker(
        controller: UIDocumentPickerViewController,
        didPickDocumentsAtURLs: List<*>
    ) {
        val urls = didPickDocumentsAtURLs.filterIsInstance<NSURL>()
        if (urls.isNotEmpty()) {
            val url = urls.first()
            // Start accessing security-scoped resource
            val accessed = url.startAccessingSecurityScopedResource()

            try {
                // Copy file to app's temp directory for access
                val tempPath = copyToTempDirectory(url)
                onResult(tempPath)
            } finally {
                if (accessed) {
                    url.stopAccessingSecurityScopedResource()
                }
            }
        } else {
            onResult(null)
        }
    }

    override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
        onResult(null)
    }

    private fun copyToTempDirectory(url: NSURL): String? {
        return try {
            val fileManager = NSFileManager.defaultManager
            val tempDir = NSTemporaryDirectory()
            val fileName = url.lastPathComponent ?: "temp_${NSDate().timeIntervalSince1970.toLong()}.apk"
            val destPath = "$tempDir$fileName"
            val destUrl = NSURL.fileURLWithPath(destPath)

            // Remove existing file if present
            if (fileManager.fileExistsAtPath(destPath)) {
                fileManager.removeItemAtPath(destPath, error = null)
            }

            // Copy file
            val success = fileManager.copyItemAtURL(url, toURL = destUrl, error = null)
            if (success) {
                PlatformLogger.i("PlatformFilePicker", "Copied file to: $destPath")
                destPath
            } else {
                PlatformLogger.e("PlatformFilePicker", "Failed to copy file")
                null
            }
        } catch (e: Exception) {
            PlatformLogger.e("PlatformFilePicker", "Error copying file: ${e.message}")
            null
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
@OptIn(ExperimentalForeignApi::class)
actual fun readFileBytes(path: String): ByteArray? {
    return try {
        val data = NSData.dataWithContentsOfFile(path) ?: return null
        val length = data.length.toInt()
        if (length == 0) return ByteArray(0)

        val bytes = ByteArray(length)
        bytes.usePinned { pinned ->
            platform.posix.memcpy(pinned.addressOf(0), data.bytes, data.length)
        }
        bytes
    } catch (e: Exception) {
        PlatformLogger.e("readFileBytes", "Failed to read file: $path")
        null
    }
}
