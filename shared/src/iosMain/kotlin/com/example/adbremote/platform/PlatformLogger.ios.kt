package com.example.adbremote.platform

import platform.Foundation.NSLog

actual object PlatformLogger {
    actual fun d(tag: String, message: String) {
        NSLog("D/$tag: $message")
    }

    actual fun i(tag: String, message: String) {
        NSLog("I/$tag: $message")
    }

    actual fun w(tag: String, message: String) {
        NSLog("W/$tag: $message")
    }

    actual fun e(tag: String, message: String, throwable: Throwable?) {
        if (throwable != null) {
            NSLog("E/$tag: $message - ${throwable.message}")
        } else {
            NSLog("E/$tag: $message")
        }
    }
}
