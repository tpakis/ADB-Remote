# ADB Remote

A cross-platform application that allows you to send ADB (Android Debug Bridge) commands to Android devices over TCP/IP network. Built with Kotlin Multiplatform and Compose Multiplatform.

## Supported Platforms

- **Android** (API 26+ / Android 8.0+)
- **Desktop** (macOS, Windows, Linux)
- **iOS** (iOS 14+)

## Features

### Core Functionality
- Connect to Android devices over TCP/IP
- **RSA Authentication Support** - Automatically authenticates with devices using RSA key signing
- Execute ADB shell commands remotely
- View command history and output
- Persistent key storage for seamless reconnection

### Device Management
- Add devices by IP address and port
- **Emulator Support** - Connect to emulators using `emulator-5554@host` syntax
- **Network Scanner** - Scan local network for ADB-enabled devices (port 5555)
- **Device Persistence** - Saved devices restored on app restart
- **Auto-reconnect** - Remembers last connected device

### Command Execution
- Execute any ADB shell command
- **Recent Commands** - Last 10 successful commands in horizontal scroll list
- Quick command shortcuts (getprop, list packages, top processes)
- Real-time output display with timestamps

### RCU (Remote Control Unit)
TV remote control interface for Android TV devices:
- D-pad navigation (Up/Down/Left/Right/Center)
- Volume and Channel controls
- Main controls (Back/Home/Settings)
- Number pad (0-9)
- Playback controls (Play/Pause/Stop/Rewind/FF)
- HbbTV color keys (Red/Green/Yellow/Blue)
- External app buttons (Netflix/YouTube/Prime Video)

### UI
- Modern Material Design 3
- Compose Multiplatform UI
- Dark/Light theme support
- Bottom navigation (Devices / Commands / RCU)

## Installation

### Android

1. Download the latest `ADB Remote-x.x.x.apk` from the [Releases](https://github.com/user/android-adb/releases) page
2. Transfer the APK to your Android device
3. Enable **"Install from unknown sources"** in your device settings if prompted
4. Open the APK file to install
5. Grant network permissions when requested

### macOS

#### Option 1: DMG Installer (Recommended)

1. Download the latest `ADB Remote-x.x.x.dmg` from the [Releases](https://github.com/user/android-adb/releases) page
2. Open the DMG file
3. Drag "ADB Remote" to your Applications folder
4. **Important**: Since the app is not signed with an Apple Developer certificate, you need to allow it to run:
   - Try to open the app - you'll see a security warning
   - Go to **System Settings > Privacy & Security**
   - Scroll down to find the message about "ADB Remote" being blocked
   - Click **"Open Anyway"**
   - Alternatively, right-click (or Control+click) on the app and select **"Open"** from the context menu

#### Option 2: JAR File (Requires Java 17+)

1. Download `ADB Remote-macos-arm64-x.x.x-release.jar` (Apple Silicon) or build from source for Intel
2. Run with: `java -jar "ADB Remote-macos-arm64-1.0.0-release.jar"`

### Linux

#### Option 1: DEB Package (Debian/Ubuntu)

Build the DEB package on a Linux machine:
```bash
./gradlew :composeApp:packageReleaseDeb
```
The package will be at `composeApp/build/compose/binaries/main-release/deb/`

Install:
```bash
sudo dpkg -i adb-remote_1.0.0-1_amd64.deb
```

#### Option 2: JAR File (Requires Java 17+)

1. Build the JAR on Linux: `./gradlew :composeApp:packageReleaseUberJarForCurrentOS`
2. Run with: `java -jar "ADB Remote-linux-x64-1.0.0-release.jar"`

### Windows

#### Option 1: MSI Installer

Build on a Windows machine:
```bash
./gradlew :composeApp:packageReleaseMsi
```

#### Option 2: JAR File (Requires Java 17+)

1. Build the JAR on Windows: `./gradlew :composeApp:packageReleaseUberJarForCurrentOS`
2. Run with: `java -jar "ADB Remote-windows-x64-1.0.0-release.jar"`

Or double-click the JAR file if Java is properly associated with `.jar` files.

### Building from Source

For all platforms, you can build and run directly:
```bash
# Run the desktop app
./gradlew :composeApp:run

# Build release packages for current OS
./gradlew :composeApp:packageReleaseDistributionForCurrentOS

# Build cross-platform JAR
./gradlew :composeApp:packageReleaseUberJarForCurrentOS
```

## Requirements

- Target device must have ADB over TCP/IP enabled
- Both devices must be on the same network
- For first-time connection: accept the connection prompt on the target device

## Setting Up Target Device

Before you can connect to a target Android device, you need to enable ADB over TCP/IP on it:

### Method 1: Using USB Connection (Recommended)

1. Connect the target device to a computer via USB
2. Enable USB debugging on the target device (Settings > Developer Options > USB Debugging)
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
   - Enter the device address in one of these formats:
     - IP with port: `192.168.1.100:5555`
     - IP with default port: `192.168.1.100`
     - Emulator on host: `emulator-5554@host`
   - Tap "Add"

2. **Scan for Devices** (Optional)
   - Tap the scan button to find ADB-enabled devices on your local network
   - Discovered devices can be added with one tap

3. **Connect to Device**
   - Select the device from the list
   - Tap "Connect"
   - If connecting for the first time, accept the connection on the target device
   - Once connected, the "Commands" and "RCU" tabs become available

4. **Execute Commands**
   - Switch to the "Commands" tab
   - Enter an ADB shell command (e.g., `ls /sdcard`, `pm list packages`)
   - Tap send or use quick command chips
   - View the output in the command history

5. **Remote Control** (for Android TV)
   - Switch to the "RCU" tab
   - Use the on-screen remote to control the device
   - Each button sends the corresponding ADB keyevent command

## Quick Commands

The app includes several quick command shortcuts:
- **Device Info**: Shows all device properties (`getprop`)
- **List Apps**: Lists all installed packages (`pm list packages`)
- **Top Processes**: Shows running processes (`top -n 1`)
- **Install APK**: Pick an APK file from your device and install it on the remote Android device
- **Dump Logcat to File**: Save logcat output to a file (found in Command Browser > Debug)

## Building the Project

### Android

```bash
# Build debug APK
./gradlew :composeApp:assembleDebug

# APK location: composeApp/build/outputs/apk/debug/
```

### Desktop

```bash
# Run desktop app
./gradlew :composeApp:run

# Build distribution packages
./gradlew :composeApp:packageDmg    # macOS
./gradlew :composeApp:packageMsi    # Windows
./gradlew :composeApp:packageDeb    # Linux
```

### iOS

#### Option 1: Using Xcode (Recommended)

```bash
open iosApp/iosApp.xcodeproj
```
Then select an iPhone simulator and click Run (▶️).

#### Option 2: Command Line

```bash
# 1. Build the Kotlin framework for iOS Simulator
./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64

# 2. Build the Xcode project
cd iosApp && xcodebuild -project iosApp.xcodeproj -scheme iosApp \
  -destination 'platform=iOS Simulator,name=iPhone 15 Pro' \
  -derivedDataPath build build

# 3. Install on booted simulator
xcrun simctl install booted iosApp/build/Build/Products/Debug-iphonesimulator/iosApp.app

# 4. Launch the app
xcrun simctl launch booted com.example.adbremote
```

**Quick rebuild after code changes:**
```bash
./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64 && \
cd iosApp && xcodebuild -project iosApp.xcodeproj -scheme iosApp \
  -destination 'platform=iOS Simulator,name=iPhone 15 Pro' \
  -derivedDataPath build build && \
xcrun simctl install booted build/Build/Products/Debug-iphonesimulator/iosApp.app && \
xcrun simctl launch booted com.example.adbremote
```

### Other Commands

```bash
# Compile shared module
./gradlew :shared:compileDebugKotlinAndroid
./gradlew :shared:compileKotlinDesktop

# List available tasks
./gradlew tasks
```

## Project Structure

```
android-adb/
├── shared/                    # Kotlin Multiplatform shared code
│   └── src/
│       ├── commonMain/        # Shared business logic
│       │   └── kotlin/.../
│       │       ├── adb/       # ADB protocol implementation
│       │       ├── model/     # Data models
│       │       ├── data/      # Repository layer
│       │       ├── viewmodel/ # Business logic
│       │       └── platform/  # Platform abstractions (expect)
│       ├── androidMain/       # Android implementations (actual)
│       ├── desktopMain/       # Desktop implementations (actual)
│       └── iosMain/           # iOS implementations (actual)
├── composeApp/                # Compose Multiplatform UI
│   └── src/
│       ├── commonMain/        # Shared UI code
│       │   └── kotlin/.../ui/
│       │       ├── DeviceListScreen.kt
│       │       ├── CommandScreen.kt
│       │       ├── remote/    # RCU screen
│       │       └── components/
│       ├── androidMain/       # Android entry point
│       ├── desktopMain/       # Desktop entry point
│       └── iosMain/           # iOS entry point
└── iosApp/                    # iOS Xcode project
```

## Technical Details

### ADB Protocol Implementation

This app implements the ADB wire protocol to communicate with devices:
- Connection establishment (CNXN)
- RSA authentication (AUTH with TOKEN, SIGNATURE, RSAPUBLICKEY)
- Command execution through shell service
- Binary protocol with checksums
- Support for streaming output
- Automatic key generation and management

### Architecture

- **Language**: Kotlin 1.9.20
- **UI Framework**: Compose Multiplatform 1.5.11
- **Architecture**: MVVM with StateFlow
- **Platforms**: Android (API 26+), Desktop (JVM), iOS (Kotlin/Native)
- **Gradle**: 8.5

### Platform Abstractions

The app uses Kotlin's `expect`/`actual` mechanism for platform-specific code:
- `PlatformSocket` - TCP socket operations
- `PlatformCrypto` - RSA key generation and signing
- `PlatformStorage` - Preferences and file storage
- `PlatformLogger` - Logging facade
- `NetworkScanner` - Local network device discovery
- `PlatformFileSaver` - Save files to device storage (Downloads on Android, Documents on iOS)
- `PlatformFilePicker` - Native file picker (SAF on Android, UIDocumentPicker on iOS, AWT FileDialog on Desktop)

## Troubleshooting

### Connection Failed

- Verify both devices are on the same network
- Check that the target device has ADB over TCP/IP enabled
- Ensure the IP address is correct
- Check that no firewall is blocking port 5555

### Authentication

The app automatically handles ADB authentication using RSA keys:
- On first connection, the app generates an RSA key pair
- The public key is sent to the target device
- You'll see a prompt on the target device to accept the connection
- Once accepted (with "Always allow"), future connections are automatic
- Keys are stored securely in app-specific storage

### Command Execution Failed

- Verify the command syntax is correct
- Some commands may require root access on the target device
- Check if the connection is still active

### Connection Lost After Background

- The app detects when the socket connection is lost
- You'll see "Connection lost. Please reconnect." message
- Simply tap Connect again to reconnect

## Security Note

ADB over TCP/IP is a powerful feature that should be used carefully:
- Only enable it on trusted networks
- Disable it when not in use
- Be aware that anyone on the network can potentially connect to the device
- Do not use this on public networks

## Permissions

### Android
- `INTERNET`: To communicate with devices over network
- `ACCESS_NETWORK_STATE`: To check network connectivity
- `ACCESS_WIFI_STATE`: To access WiFi information

### Desktop & iOS
- Network access for TCP/IP communication

## License

This project is created for educational purposes.

## Disclaimer

Use this tool responsibly and only on devices you own or have explicit permission to access. The developers are not responsible for any misuse of this application.
