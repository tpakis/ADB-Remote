package com.example.adbremote.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun HelpScreen(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // App Overview
        Text(
            text = "About ADB Remote",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "ADB Remote allows you to send ADB (Android Debug Bridge) commands to Android devices over your local network. Control your Android TV, phone, or tablet wirelessly.",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Getting Started
        Text(
            text = "Getting Started",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Android TV Section
        HelpCard(
            title = "Android TV Devices",
            content = """Android TV devices typically have ADB over network enabled by default.

1. Go to Settings > Device Preferences > About
2. Find the IP Address of your TV
3. In this app, tap "Add Device"
4. Enter the IP address (port 5555 is default)
5. Tap Connect and accept the connection on your TV"""
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Mobile Devices Section
        HelpCard(
            title = "Mobile Devices (Phones/Tablets)",
            isWarning = true,
            content = """Mobile devices require enabling ADB over TCP/IP first. You'll need a computer with ADB installed.

Steps to enable WiFi ADB:

1. Enable Developer Options on your phone
   (Settings > About > Tap Build Number 7 times)

2. Enable USB Debugging in Developer Options

3. Connect your phone to a computer via USB

4. On the computer, run:"""
        ) {
            // Code block for command
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "adb tcpip 5555",
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(12.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = """5. Disconnect the USB cable

6. Find your phone's IP address
   (Settings > WiFi > tap your network)

7. Add the device in this app using the IP address

Note: WiFi ADB resets after reboot. You'll need to repeat steps 3-5 after restarting your phone.""",
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Network Scanner
        HelpCard(
            title = "Network Scanner",
            content = """Use the "Scan" button on the Devices tab to automatically discover ADB-enabled devices on your local network.

The scanner checks for devices with port 5555 open. Discovered devices can be added with one tap."""
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Local ADB (Desktop)
        HelpCard(
            title = "Local ADB (Desktop Only)",
            content = """On the desktop app, the "Local ADB" button on the Devices tab detects devices already connected to your computer's ADB server (via USB or emulator).

Tap the button to scan for locally connected devices. A dialog will list all available devices — select one to start using it directly. If you are already connected to another device (via network or local ADB), it will be disconnected automatically.

This is the same check that runs when the desktop app starts."""
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Tabs Overview
        Text(
            text = "App Features",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(12.dp))

        HelpCard(
            title = "Devices Tab",
            content = """Manage your ADB devices:
- Add devices manually by IP address
- Scan network for devices
- Connect/disconnect from devices
- Edit or remove saved devices"""
        )

        Spacer(modifier = Modifier.height(12.dp))

        HelpCard(
            title = "Commands Tab",
            content = """Execute ADB shell commands:
- Quick commands for common operations
- Install APK files remotely
- Dump logcat to file
- Generate bug reports
- Command history with timestamps"""
        )

        Spacer(modifier = Modifier.height(12.dp))

        HelpCard(
            title = "RCU Tab (Remote Control)",
            content = """Control Android TV devices:
- D-pad navigation
- Volume and channel controls
- Number pad
- Playback controls
- Quick app launchers (Netflix, YouTube, etc.)"""
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Troubleshooting
        Text(
            text = "Troubleshooting",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(12.dp))

        HelpCard(
            title = "Connection Issues",
            content = """If you can't connect to a device:

- Ensure both devices are on the same WiFi network
- Check that the IP address is correct
- Verify ADB over TCP/IP is enabled (mobile devices)
- Try port 5555 (default ADB port)
- Check if a firewall is blocking the connection
- On the target device, revoke USB debugging authorizations and try again"""
        )

        Spacer(modifier = Modifier.height(12.dp))

        HelpCard(
            title = "Authentication",
            content = """When connecting for the first time:

- A prompt will appear on the target device
- Select "Always allow from this computer"
- If you deny, you'll need to accept on next attempt

The RSA key is stored locally and reused for future connections."""
        )

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun HelpCard(
    title: String,
    content: String,
    isWarning: Boolean = false,
    modifier: Modifier = Modifier,
    additionalContent: @Composable ColumnScope.() -> Unit = {}
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isWarning)
                MaterialTheme.colorScheme.secondaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Icon(
                    imageVector = if (isWarning) Icons.Default.Warning else Icons.Default.Info,
                    contentDescription = null,
                    tint = if (isWarning)
                        MaterialTheme.colorScheme.onSecondaryContainer
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Text(
                text = content,
                style = MaterialTheme.typography.bodyMedium
            )
            additionalContent()
        }
    }
}
