package com.example.adbremote.platform

/**
 * Platform-specific context for initialization.
 * On Android this wraps Context, on other platforms it may be empty or contain paths.
 */
expect class PlatformContext

/**
 * Initialize platform-specific components.
 * Must be called before using platform services.
 */
expect fun initializePlatform(context: PlatformContext)

/**
 * Get the initialized platform storage factory.
 * Will throw if platform not initialized.
 */
expect fun getPlatformStorageFactory(): PlatformStorageFactory
