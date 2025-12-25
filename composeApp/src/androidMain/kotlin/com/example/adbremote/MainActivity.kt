package com.example.adbremote

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.example.adbremote.platform.PlatformContext
import com.example.adbremote.platform.PlatformCrypto
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
        initializePlatform(PlatformContext(applicationContext))

        // Create controller
        val storage = PlatformStorageFactory.create("adb_remote_prefs")
        val keyStorage = PlatformStorageFactory.create("adb_keys")
        val crypto = PlatformCrypto()
        controller = AdbController(scope, storage, keyStorage, crypto)

        setContent {
            ADBRemoteTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    controller?.let { ctrl ->
                        AdbRemoteApp(controller = ctrl)
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
