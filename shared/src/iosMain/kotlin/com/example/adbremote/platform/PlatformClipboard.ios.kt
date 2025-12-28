package com.example.adbremote.platform

import platform.UIKit.UIPasteboard

actual class PlatformClipboard {
    actual fun copyToClipboard(text: String) {
        UIPasteboard.generalPasteboard.string = text
    }
}

actual object PlatformClipboardFactory {
    actual fun create(context: PlatformContext): PlatformClipboard {
        return PlatformClipboard()
    }
}
