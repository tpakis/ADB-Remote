package com.example.adbremote.platform

import android.content.Context

actual class PlatformContext(val androidContext: Context)

actual fun initializePlatform(context: PlatformContext) {
    PlatformStorageFactory.initialize(context.androidContext)
}

actual fun getPlatformStorageFactory(): PlatformStorageFactory {
    return PlatformStorageFactory
}
