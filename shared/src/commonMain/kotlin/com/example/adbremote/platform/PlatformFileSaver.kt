package com.example.adbremote.platform

/**
 * Platform-specific file saver interface.
 * Allows saving text or binary content to a file chosen by the user.
 */
expect class PlatformFileSaver {
    /**
     * Shows a file save dialog and saves the content to the selected location.
     * @param defaultFileName The suggested filename
     * @param content The text content to save
     * @param onResult Callback with the result - path on success, null on cancel/failure
     */
    suspend fun saveTextFile(
        defaultFileName: String,
        content: String,
        onResult: (path: String?) -> Unit
    )

    /**
     * Shows a file save dialog and saves binary content to the selected location.
     * @param defaultFileName The suggested filename
     * @param content The binary content to save
     * @param mimeType The MIME type of the file (e.g., "application/zip")
     * @param onResult Callback with the result - path on success, null on cancel/failure
     */
    suspend fun saveBinaryFile(
        defaultFileName: String,
        content: ByteArray,
        mimeType: String,
        onResult: (path: String?) -> Unit
    )
}

/**
 * Extension function that wraps saveTextFile to properly await the result.
 * Uses a CompletableDeferred to bridge the callback to a suspending result.
 */
suspend fun PlatformFileSaver.saveTextFileAwait(
    defaultFileName: String,
    content: String
): String? {
    val deferred = kotlinx.coroutines.CompletableDeferred<String?>()
    saveTextFile(defaultFileName, content) { path ->
        deferred.complete(path)
    }
    return deferred.await()
}

/**
 * Extension function that wraps saveBinaryFile to properly await the result.
 * Uses a CompletableDeferred to bridge the callback to a suspending result.
 */
suspend fun PlatformFileSaver.saveBinaryFileAwait(
    defaultFileName: String,
    content: ByteArray,
    mimeType: String
): String? {
    val deferred = kotlinx.coroutines.CompletableDeferred<String?>()
    saveBinaryFile(defaultFileName, content, mimeType) { path ->
        deferred.complete(path)
    }
    return deferred.await()
}

/**
 * Factory to create platform-specific file saver instances
 */
expect object PlatformFileSaverFactory {
    fun create(context: PlatformContext): PlatformFileSaver
}
