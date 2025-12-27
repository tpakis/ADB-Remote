package com.example.adbremote

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
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

class MainActivity : ComponentActivity() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var controller: AdbController? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize platform
        val platformContext = PlatformContext(applicationContext)
        initializePlatform(platformContext)

        // Create controller
        val storage = PlatformStorageFactory.create("adb_remote_prefs")
        val keyStorage = PlatformStorageFactory.create("adb_keys")
        val crypto = PlatformCrypto()
        controller = AdbController(scope, storage, keyStorage, crypto)

        // Create file saver using factory (requires context for Android)
        val fileSaver = PlatformFileSaverFactory.create(platformContext)

        setContent {
            // State to hold the pending callback for file picker
            val pendingCallback = remember { mutableStateOf<((Uri?) -> Unit)?>(null) }

            // Create SAF launcher for file picking
            val filePickerLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.OpenDocument()
            ) { uri ->
                pendingCallback.value?.invoke(uri)
                pendingCallback.value = null
            }

            // Create file picker with launcher
            val filePicker = remember {
                PlatformFilePickerFactory.createWithLauncher(applicationContext) { mimeTypes, callback ->
                    pendingCallback.value = callback
                    filePickerLauncher.launch(mimeTypes)
                }
            }

            ADBRemoteTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    controller?.let { ctrl ->
                        AdbRemoteApp(controller = ctrl, fileSaver = fileSaver, filePicker = filePicker)
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        controller?.cleanup()
    }
}
