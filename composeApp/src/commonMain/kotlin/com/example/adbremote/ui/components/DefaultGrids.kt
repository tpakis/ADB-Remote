package com.example.adbremote.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.adbremote.ui.rcu.RcuKeyInfo
import com.example.adbremote.ui.rcu.Tag
import com.example.adbremote.ui.rcu.playbackKeys

@Composable
fun PlaybackControlsGrid(
    modifier: Modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
    remoteKeyPressed: (Tag) -> Unit,
    iconSize: Dp = 20.dp,
    columnsCount: Int = 4,
    rcuKeysToDisplay: List<RcuKeyInfo> = playbackKeys
) {
    RemoteKeyGrid(
        modifier = modifier,
        remoteKeyPressed = remoteKeyPressed,
        iconSize = iconSize,
        columnsCount = columnsCount,
        rcuKeysToDisplay = rcuKeysToDisplay
    )
}
