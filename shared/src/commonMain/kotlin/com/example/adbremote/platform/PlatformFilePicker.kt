package com.example.adbremote.platform

/**
 * Platform-specific file picker interface.
 * Allows picking files using the native file picker.
 */
expect class PlatformFilePicker {
    /**
     * Shows a file picker dialog to select a file.
     * @param mimeType The MIME type filter (e.g., "application/vnd.android.package-archive" for APK)
     * @param onResult Callback with the result - file path on success, null on cancel
     */
    fun pickFile(
        mimeType: String,
        onResult: (path: String?) -> Unit
    )
}

/**
 * Factory to create platform-specific file picker instances
 */
expect object PlatformFilePickerFactory {
    fun create(context: PlatformContext): PlatformFilePicker
}

/**
 * Read file bytes from a local file path.
 * Used after file picker returns a path to read the actual content.
 */
expect fun readFileBytes(path: String): ByteArray?
