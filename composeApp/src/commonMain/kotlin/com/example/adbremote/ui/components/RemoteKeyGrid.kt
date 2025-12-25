package com.example.adbremote.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.adbremote.ui.rcu.RcuKeyInfo
import com.example.adbremote.ui.rcu.Tag
import com.example.adbremote.ui.rcu.backgroundColor
import com.example.adbremote.ui.rcu.playbackKeys

@Composable
fun RemoteKeyGrid(
    modifier: Modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
    remoteKeyPressed: (Tag) -> Unit,
    iconSize: Dp = 16.dp,
    columnsCount: Int = 3,
    rcuKeysToDisplay: List<RcuKeyInfo> = playbackKeys
) {
    LazyVerticalGrid(
        modifier = modifier,
        columns = GridCells.Fixed(columnsCount),
        horizontalArrangement = Arrangement.aligned(Alignment.CenterHorizontally),
    ) {
        items(rcuKeysToDisplay) { keyInfo ->
            RemoteKey(
                tag = keyInfo.tag,
                icon = keyInfo.icon,
                iconSize = iconSize,
                iconTintColor = keyInfo.tintColor,
                remoteKeyPressed = remoteKeyPressed
            )
        }
    }
}

@Composable
fun RemoteKey(
    modifier: Modifier = Modifier.fillMaxWidth(),
    iconSize: Dp = 16.dp,
    tag: Tag,
    icon: ImageVector,
    iconTintColor: Color = Color.White,
    remoteKeyPressed: (Tag) -> Unit,
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        IconButton(
            modifier = Modifier
                .background(shape = CircleShape, color = backgroundColor()),
            onClick = { remoteKeyPressed.invoke(tag) },
        ) {
            Icon(
                modifier = Modifier.padding(8.dp).size(iconSize),
                imageVector = icon,
                contentDescription = tag.toString(),
                tint = iconTintColor,
            )
        }
    }
}
