package com.example.adbremote.ui.rcu

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Data class representing information about a remote control key.
 */
@Immutable
data class RcuKeyInfo(
    val tag: Tag,
    val icon: ImageVector,
    val title: String = "",
    val tintColor: Color = Color.White
)
