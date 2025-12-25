package com.example.adbremote.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.adbremote.ui.rcu.RcuKeyInfo
import com.example.adbremote.ui.rcu.Tag

@Composable
fun RemoteKeyRow(
    modifier: Modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
    remoteKeyPressed: (Tag) -> Unit,
    iconSize: Dp = 16.dp,
    rcuKeysToDisplay: List<RcuKeyInfo>
) {
    Row(modifier) {
        rcuKeysToDisplay.forEach { keyInfo ->
            RemoteKey(
                modifier = Modifier.weight(1f),
                tag = keyInfo.tag,
                icon = keyInfo.icon,
                iconSize = iconSize,
                iconTintColor = keyInfo.tintColor,
                remoteKeyPressed = remoteKeyPressed
            )
        }
    }
}
