# ADB Remote

An Android application that allows you to send ADB (Android Debug Bridge) commands to other Android devices on the same network.

## Features

- Connect to Android devices over TCP/IP
- Execute ADB shell commands remotely
- View command history and output
- Modern Material Design 3 UI with Jetpack Compose
- Dark/Light theme support

## Requirements

- Android device running Android 8.0 (API 26) or higher
- Target device must have ADB over TCP/IP enabled
- Both devices must be on the same network

## Setting Up Target Device

Before you can connect to a target Android device, you need to enable ADB over TCP/IP on it:

### Method 1: Using USB Connection (Recommended)

1. Connect the target device to a computer via USB
2. Enable USB debugging on the target device (Settings → Developer Options → USB Debugging)
3. Run the following commands on your computer:
   ```bash
   adb tcpip 5555
   adb connect <device-ip>:5555
   ```
4. Disconnect the USB cable
5. The device will continue to accept ADB connections over TCP/IP

### Method 2: Using Root Access

If your target device is rooted, you can enable ADB over TCP/IP directly on the device:

1. Install a terminal app on the target device
2. Run:
   ```bash
   su
   setprop service.adb.tcp.port 5555
   stop adbd
   start adbd
   ```

## Usage

1. **Add a Device**
   - Launch the app and tap the "+" button
   - Enter the target device's IP address
   - Enter the port (default: 5555)
   - Tap "Add"

2. **Connect to Device**
   - Select the device from the list
   - Tap "Connect"
   - Once connected, the "Commands" tab will become available

3. **Execute Commands**
   - Switch to the "Commands" tab
   - Enter an ADB shell command (e.g., `ls /sdcard`, `pm list packages`)
   - Tap send or use quick command chips
   - View the output in the command history

## Quick Commands

The app includes several quick command shortcuts:
- **Device Info**: Shows all device properties (`getprop`)
- **List Apps**: Lists all installed packages (`pm list packages`)
- **Top Processes**: Shows running processes (`top -n 1`)

## Troubleshooting

### Connection Failed

- Verify both devices are on the same network
- Check that the target device has ADB over TCP/IP enabled
- Ensure the IP address is correct
- Check that no firewall is blocking port 5555

### Authentication Required Error

The target device requires authentication. This typically means ADB over TCP/IP wasn't properly enabled. Follow the setup instructions above to enable it first.

### Command Execution Failed

- Verify the command syntax is correct
- Some commands may require root access on the target device
- Check if the connection is still active

## Security Note

ADB over TCP/IP is a powerful feature that should be used carefully:
- Only enable it on trusted networks
- Disable it when not in use
- Be aware that anyone on the network can potentially connect to the device
- Do not use this on public networks

## Technical Details

### ADB Protocol Implementation

This app implements the ADB wire protocol to communicate with devices:
- Connection establishment (CNXN)
- Command execution through shell service
- Binary protocol with checksums
- Support for streaming output

### Architecture

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose with Material 3
- **Architecture**: MVVM with StateFlow
- **Networking**: Java sockets with Kotlin coroutines
- **Minimum SDK**: 26 (Android 8.0)

## Building the Project

1. Clone the repository
2. Open in Android Studio
3. Sync Gradle
4. Run on your device or emulator

```bash
./gradlew assembleDebug
```

## Permissions

The app requires the following permissions:
- `INTERNET`: To communicate with devices over network
- `ACCESS_NETWORK_STATE`: To check network connectivity
- `ACCESS_WIFI_STATE`: To access WiFi information
- `CHANGE_WIFI_MULTICAST_STATE`: For potential future device discovery features

## License

This project is created for educational purposes.

## Disclaimer

Use this tool responsibly and only on devices you own or have explicit permission to access. The developers are not responsible for any misuse of this application.
