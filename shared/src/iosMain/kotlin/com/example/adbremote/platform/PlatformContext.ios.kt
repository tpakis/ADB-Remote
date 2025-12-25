package com.example.adbremote.platform

actual class PlatformContext

actual fun initializePlatform(context: PlatformContext) {
    // No initialization needed for iOS
}

actual fun getPlatformStorageFactory(): PlatformStorageFactory {
    return PlatformStorageFactory
}
