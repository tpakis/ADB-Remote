package com.example.adbremote.platform

import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

actual class PlatformClipboard {
    actual fun copyToClipboard(text: String) {
        val clipboard = Toolkit.getDefaultToolkit().systemClipboard
        val selection = StringSelection(text)
        clipboard.setContents(selection, selection)
    }
}

actual object PlatformClipboardFactory {
    actual fun create(context: PlatformContext): PlatformClipboard {
        return PlatformClipboard()
    }
}
