package com.example.adbremote.platform

/**
 * Platform-specific clipboard interface.
 * Allows copying text to the system clipboard.
 */
expect class PlatformClipboard {
    /**
     * Copies the given text to the system clipboard.
     * @param text The text to copy
     */
    fun copyToClipboard(text: String)
}

/**
 * Factory to create platform-specific clipboard instances
 */
expect object PlatformClipboardFactory {
    fun create(context: PlatformContext): PlatformClipboard
}
