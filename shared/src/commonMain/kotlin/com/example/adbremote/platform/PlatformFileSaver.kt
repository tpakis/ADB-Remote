package com.example.adbremote.platform

/**
 * Platform-specific file saver interface.
 * Allows saving text content to a file chosen by the user.
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
}

/**
 * Factory to create platform-specific file saver instances
 */
expect object PlatformFileSaverFactory {
    fun create(context: PlatformContext): PlatformFileSaver
}
