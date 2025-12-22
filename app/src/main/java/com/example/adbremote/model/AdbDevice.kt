package com.example.adbremote.model

data class AdbDevice(
    val name: String,
    val host: String,
    val port: Int = 5555,
    val isConnected: Boolean = false
)

data class CommandResult(
    val command: String,
    val output: String,
    val isError: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)
