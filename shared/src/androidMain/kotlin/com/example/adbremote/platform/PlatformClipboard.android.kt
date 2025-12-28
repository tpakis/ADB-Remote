package com.example.adbremote.platform

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context

actual class PlatformClipboard(private val context: Context) {
    actual fun copyToClipboard(text: String) {
        val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("ADB Remote", text)
        clipboardManager.setPrimaryClip(clip)
    }
}

actual object PlatformClipboardFactory {
    actual fun create(context: PlatformContext): PlatformClipboard {
        return PlatformClipboard(context.androidContext)
    }
}
