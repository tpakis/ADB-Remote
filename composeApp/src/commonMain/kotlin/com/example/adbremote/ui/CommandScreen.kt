package com.example.adbremote.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.adbremote.model.CommandResult
import com.example.adbremote.viewmodel.AdbUiState

// Data model for predefined commands
data class PredefinedCommand(
    val name: String,
    val command: String,
    val description: String = "",
    val saveToFile: Boolean = false,
    val defaultFileName: String = "",
    val isBugreport: Boolean = false  // Special flag for bugreport command
)

data class CommandCategory(
    val name: String,
    val commands: List<PredefinedCommand>
)

val predefinedCommands = listOf(
    CommandCategory(
        name = "Accessibility",
        commands = listOf(
            PredefinedCommand(
                name = "Enable TalkBack",
                command = "settings put secure enabled_accessibility_services com.google.android.marvin.talkback/com.google.android.marvin.talkback.TalkBackService",
                description = "Enable screen reader"
            ),
            PredefinedCommand(
                name = "Disable TalkBack",
                command = "settings put secure enabled_accessibility_services com.android.talkback/com.google.android.marvin.talkback.TalkBackService",
                description = "Disable screen reader"
            ),
            PredefinedCommand(
                name = "Font Scale: Default (1.0)",
                command = "settings put system font_scale 1.0"
            ),
            PredefinedCommand(
                name = "Font Scale: Large (1.15)",
                command = "settings put system font_scale 1.15"
            ),
            PredefinedCommand(
                name = "Font Scale: Largest (1.30)",
                command = "settings put system font_scale 1.30"
            ),
            PredefinedCommand(
                name = "Font Scale: Mobile (2.0)",
                command = "settings put system font_scale 2.0"
            )
        )
    ),
    CommandCategory(
        name = "Debug",
        commands = listOf(
            PredefinedCommand(
                name = "Dump Logcat to File",
                command = "logcat -d",
                description = "Save logcat output to a file",
                saveToFile = true,
                defaultFileName = "logcat.txt"
            ),
            PredefinedCommand(
                name = "Generate Bug Report",
                command = "bugreport",
                description = "Generate and save a full bug report (may take minutes)",
                isBugreport = true,
                defaultFileName = "bugreport.zip"
            ),
            PredefinedCommand(
                name = "List SurfaceViews",
                command = "dumpsys SurfaceFlinger --list",
                description = "List all SurfaceViews for all displays"
            ),
            PredefinedCommand(
                name = "Power State",
                command = "dumpsys power | grep -i wake",
                description = "Get current system power state"
            ),
            PredefinedCommand(
                name = "Force App Update Check",
                command = "am broadcast -a otupdate.CHECK_UPDATE_ACTION",
                description = "Trigger OTA update check"
            ),
            PredefinedCommand(
                name = "Force Config Update Check",
                command = "am broadcast -a otupdate.CHECK_CONFIG_ACTION",
                description = "Trigger config update check"
            ),
            PredefinedCommand(
                name = "Force Crash",
                command = "am broadcast -a debug.CRASH",
                description = "Debug/QA builds only"
            ),
            PredefinedCommand(
                name = "Force Crash (with reason)",
                command = "am broadcast -a debug.CRASH --es reason \"test crash\"",
                description = "Debug/QA builds only"
            ),
            PredefinedCommand(
                name = "Force Native Crash",
                command = "am broadcast -a debug.NATIVE_CRASH",
                description = "Debug/QA builds only"
            ),
            PredefinedCommand(
                name = "Force ANR",
                command = "am broadcast -a debug.ANR",
                description = "Debug/QA builds, via service start timeout"
            )
        )
    ),
    CommandCategory(
        name = "Connectivity",
        commands = listOf(
            PredefinedCommand(
                name = "WiFi: Enable",
                command = "svc wifi enable"
            ),
            PredefinedCommand(
                name = "WiFi: Disable",
                command = "svc wifi disable"
            ),
            PredefinedCommand(
                name = "Mobile Data: Enable",
                command = "svc data enable"
            ),
            PredefinedCommand(
                name = "Mobile Data: Disable",
                command = "svc data disable"
            ),
            PredefinedCommand(
                name = "Airplane Mode: Enable",
                command = "cmd connectivity airplane-mode enable",
                description = "Mobile devices only"
            ),
            PredefinedCommand(
                name = "Airplane Mode: Disable",
                command = "cmd connectivity airplane-mode disable",
                description = "Mobile devices only"
            )
        )
    )
)

// Data class to track file operation status
data class FileOperationStatus(
    val isInProgress: Boolean = false,
    val operationName: String = "",
    val fileName: String = ""
)

@Composable
fun CommandScreen(
    uiState: AdbUiState,
    onExecuteCommand: (String) -> Unit,
    onSaveToFileCommand: (command: String, defaultFileName: String) -> Unit,
    onBugreport: (defaultFileName: String) -> Unit,
    isGeneratingBugreport: Boolean,
    fileOperationStatus: FileOperationStatus = FileOperationStatus(),
    onInstallApk: () -> Unit,
    isInstalling: Boolean,
    onClearHistory: () -> Unit,
    onCopyToClipboard: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var commandText by remember { mutableStateOf("") }
    var showCommandBrowser by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    // Command browser dialog
    if (showCommandBrowser) {
        CommandBrowserDialog(
            onDismiss = { showCommandBrowser = false },
            onCommandSelected = { cmd ->
                when {
                    cmd.isBugreport -> onBugreport(cmd.defaultFileName)
                    cmd.saveToFile -> onSaveToFileCommand(cmd.command, cmd.defaultFileName)
                    else -> onExecuteCommand(cmd.command)
                }
                showCommandBrowser = false
            },
            isGeneratingBugreport = isGeneratingBugreport
        )
    }

    // Auto-scroll to bottom when new items are added
    LaunchedEffect(uiState.commandHistory.size) {
        if (uiState.commandHistory.isNotEmpty()) {
            listState.animateScrollToItem(uiState.commandHistory.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Quick commands
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Quick Commands",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
            IconButton(
                onClick = { showCommandBrowser = true },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.Search,
                    contentDescription = "Browse commands",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            QuickCommandChip(
                label = "Device Info",
                onClick = { onExecuteCommand("getprop") }
            )
            QuickCommandChip(
                label = "List Apps",
                onClick = { onExecuteCommand("pm list packages") }
            )
            QuickCommandChip(
                label = "Top Processes",
                onClick = { onExecuteCommand("top -n 1") }
            )
            // Install APK chip
            AssistChip(
                onClick = onInstallApk,
                enabled = !isInstalling,
                label = {
                    if (isInstalling) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(12.dp),
                                strokeWidth = 2.dp
                            )
                            Text("Installing...", style = MaterialTheme.typography.labelSmall)
                        }
                    } else {
                        Text("Install APK", style = MaterialTheme.typography.labelSmall)
                    }
                }
            )
        }

        // Recent commands
        if (uiState.recentCommands.isNotEmpty()) {
            Text(
                text = "Recent Commands",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                uiState.recentCommands.forEach { command ->
                    QuickCommandChip(
                        label = command,
                        onClick = { onExecuteCommand(command) }
                    )
                }
            }
        }

        // Command history header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Command History",
                style = MaterialTheme.typography.titleMedium
            )
            if (uiState.commandHistory.isNotEmpty()) {
                IconButton(onClick = onClearHistory) {
                    Icon(Icons.Default.Delete, contentDescription = "Clear history")
                }
            }
        }

        // Command history
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Show file operation in progress card
            if (fileOperationStatus.isInProgress) {
                item {
                    FileOperationCard(
                        operationName = fileOperationStatus.operationName,
                        fileName = fileOperationStatus.fileName
                    )
                }
            }

            if (uiState.commandHistory.isEmpty() && !fileOperationStatus.isInProgress) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No commands executed yet\nEnter a command below",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(uiState.commandHistory) { result ->
                    CommandResultCard(
                        result = result,
                        onLongPress = { text -> onCopyToClipboard(text) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Command input
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = commandText,
                onValueChange = { commandText = it },
                label = { Text("Command") },
                placeholder = { Text("e.g., ls /sdcard") },
                enabled = !uiState.isExecuting,
                singleLine = true,
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(
                    onSend = {
                        if (commandText.isNotBlank()) {
                            onExecuteCommand(commandText)
                            commandText = ""
                        }
                    }
                ),
                trailingIcon = {
                    if (uiState.isExecuting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    }
                }
            )
            Button(
                onClick = {
                    if (commandText.isNotBlank()) {
                        onExecuteCommand(commandText)
                        commandText = ""
                    }
                },
                enabled = commandText.isNotBlank() && !uiState.isExecuting
            ) {
                Icon(Icons.Default.Send, contentDescription = null)
            }
        }
    }
}

@Composable
fun QuickCommandChip(
    label: String,
    onClick: () -> Unit
) {
    AssistChip(
        onClick = onClick,
        label = { Text(label, style = MaterialTheme.typography.labelSmall) }
    )
}

@Composable
fun FileOperationCard(
    operationName: String,
    fileName: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 3.dp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = operationName,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = fileName,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CommandResultCard(
    result: CommandResult,
    onLongPress: (String) -> Unit = {}
) {
    // Combine command and output for clipboard
    val clipboardText = "${result.command}\n\n${result.output}"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { },
                onLongClick = { onLongPress(clipboardText) }
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (result.isError)
                MaterialTheme.colorScheme.errorContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (result.isError) Icons.Default.Warning else Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (result.isError)
                            MaterialTheme.colorScheme.error
                        else
                            MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = result.command,
                        style = MaterialTheme.typography.titleSmall,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Text(
                    text = formatTimestamp(result.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            Text(
                text = result.output,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                color = if (result.isError)
                    MaterialTheme.colorScheme.onErrorContainer
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// Platform-agnostic timestamp formatting
private fun formatTimestamp(timestamp: Long): String {
    // Simple time format without java.text dependency
    val seconds = (timestamp / 1000) % 60
    val minutes = (timestamp / 60000) % 60
    val hours = (timestamp / 3600000) % 24
    return "${hours.toInt().padZero()}:${minutes.toInt().padZero()}:${seconds.toInt().padZero()}"
}

// Pad single digit numbers with leading zero
private fun Int.padZero(): String = if (this < 10) "0$this" else "$this"

@Composable
fun CommandBrowserDialog(
    onDismiss: () -> Unit,
    onCommandSelected: (PredefinedCommand) -> Unit,
    isGeneratingBugreport: Boolean = false
) {
    var expandedCategory by remember { mutableStateOf<String?>(predefinedCommands.firstOrNull()?.name) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Command Browser")
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                predefinedCommands.forEach { category ->
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column {
                                // Category header
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = category.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    IconButton(
                                        onClick = {
                                            expandedCategory = if (expandedCategory == category.name) null else category.name
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            if (expandedCategory == category.name)
                                                Icons.Default.KeyboardArrowUp
                                            else
                                                Icons.Default.KeyboardArrowDown,
                                            contentDescription = "Toggle"
                                        )
                                    }
                                }

                                // Commands list (expanded)
                                if (expandedCategory == category.name) {
                                    category.commands.forEach { cmd ->
                                        val isBugreportInProgress = cmd.isBugreport && isGeneratingBugreport
                                        Surface(
                                            onClick = { if (!isBugreportInProgress) onCommandSelected(cmd) },
                                            modifier = Modifier.fillMaxWidth(),
                                            color = MaterialTheme.colorScheme.surface
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(
                                                    horizontal = 12.dp,
                                                    vertical = 8.dp
                                                )
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Text(
                                                        text = cmd.name,
                                                        style = MaterialTheme.typography.bodyMedium
                                                    )
                                                    if (isBugreportInProgress) {
                                                        CircularProgressIndicator(
                                                            modifier = Modifier.size(16.dp),
                                                            strokeWidth = 2.dp
                                                        )
                                                    }
                                                }
                                                if (cmd.description.isNotEmpty()) {
                                                    Text(
                                                        text = if (isBugreportInProgress) "Generating bugreport..." else cmd.description,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                                Text(
                                                    text = cmd.command,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontFamily = FontFamily.Monospace,
                                                    color = MaterialTheme.colorScheme.outline,
                                                    modifier = Modifier.padding(top = 4.dp)
                                                )
                                            }
                                        }
                                        Divider()
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
