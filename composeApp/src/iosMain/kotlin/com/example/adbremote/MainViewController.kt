package com.example.adbremote

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.ComposeUIViewController
import com.example.adbremote.platform.PlatformContext
import com.example.adbremote.platform.PlatformCrypto
import com.example.adbremote.platform.PlatformFilePickerFactory
import com.example.adbremote.platform.PlatformFileSaverFactory
import com.example.adbremote.platform.PlatformStorageFactory
import com.example.adbremote.platform.initializePlatform
import com.example.adbremote.ui.AdbRemoteApp
import com.example.adbremote.ui.theme.ADBRemoteTheme
import com.example.adbremote.viewmodel.AdbController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

fun MainViewController() = ComposeUIViewController {
    // Initialize platform
    initializePlatform(PlatformContext())

    val scope = remember { CoroutineScope(SupervisorJob() + Dispatchers.Main) }

    val controller = remember {
        val storage = PlatformStorageFactory.create("adb_remote_prefs")
        val keyStorage = PlatformStorageFactory.create("adb_keys")
        val crypto = PlatformCrypto()
        AdbController(scope, storage, keyStorage, crypto)
    }

    val platformContext = remember { PlatformContext() }
    val fileSaver = remember { PlatformFileSaverFactory.create(platformContext) }
    val filePicker = remember { PlatformFilePickerFactory.create(platformContext) }

    DisposableEffect(Unit) {
        onDispose {
            controller.cleanup()
        }
    }

    ADBRemoteTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            AdbRemoteApp(controller = controller, fileSaver = fileSaver, filePicker = filePicker)
        }
    }
}
