package com.example.adbremote.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.adbremote.ui.rcu.*

@Composable
fun MainControlsRow(
    modifier: Modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
    remoteKeyPressed: (Tag) -> Unit,
    iconSize: Dp = 16.dp,
    rcuKeysToDisplay: List<RcuKeyInfo> = listOf(backKey, homeKey, settingsKey)
) {
    RemoteKeyRow(modifier, remoteKeyPressed, iconSize, rcuKeysToDisplay)
}

@Composable
fun DeeplinksControlsRow(
    modifier: Modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
    remoteKeyPressed: (Tag) -> Unit,
    iconSize: Dp = 16.dp,
    rcuKeysToDisplay: List<RcuKeyInfo> = listOf(appsKey, recordingsKey, searchKey, infoKey)
) {
    RemoteKeyRow(modifier, remoteKeyPressed, iconSize, rcuKeysToDisplay)
}

@Composable
fun HbbTvControlsRow(
    modifier: Modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
    remoteKeyPressed: (Tag) -> Unit,
    iconSize: Dp = 16.dp,
    rcuKeysToDisplay: List<RcuKeyInfo> = hbbtvKeys
) {
    RemoteKeyRow(modifier, remoteKeyPressed, iconSize, rcuKeysToDisplay)
}

@Composable
fun GuideRow(
    modifier: Modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
    remoteKeyPressed: (Tag) -> Unit,
    iconSize: Dp = 16.dp,
    rcuKeysToDisplay: List<RcuKeyInfo> = listOf(guideKey, menuKey, assistantKey, powerKey)
) {
    RemoteKeyRow(modifier, remoteKeyPressed, iconSize, rcuKeysToDisplay)
}

@Composable
fun ExternalAppsRow(
    modifier: Modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
    remoteKeyPressed: (Tag) -> Unit,
    iconSize: Dp = 16.dp,
    rcuKeysToDisplay: List<RcuKeyInfo> = externalAppsKeys
) {
    Row(modifier = modifier) {
        rcuKeysToDisplay.forEach {
            ExternalAppButton(
                modifier = Modifier.padding(horizontal = 12.dp).weight(1f),
                rcuKeyInfo = it,
                iconSize = iconSize,
                onClick = remoteKeyPressed
            )
        }
    }
}
