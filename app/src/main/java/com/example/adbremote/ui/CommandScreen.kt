package com.example.adbremote.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.example.adbremote.model.CommandResult
import com.example.adbremote.viewmodel.AdbUiState
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CommandScreen(
    uiState: AdbUiState,
    onExecuteCommand: (String) -> Unit,
    onClearHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    var commandText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

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
            modifier = Modifier
                .fillMaxWidth()
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
            if (uiState.commandHistory.isEmpty()) {
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
                    CommandResultCard(result)
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
fun CommandResultCard(result: CommandResult) {
    val dateFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }

    Card(
        modifier = Modifier.fillMaxWidth(),
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
                        imageVector = if (result.isError) Icons.Default.Error else Icons.Default.CheckCircle,
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
                    text = dateFormat.format(Date(result.timestamp)),
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
