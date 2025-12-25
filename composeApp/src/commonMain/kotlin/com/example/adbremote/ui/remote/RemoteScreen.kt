package com.example.adbremote.ui.remote

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.adbremote.ui.components.*
import com.example.adbremote.ui.rcu.*

typealias StyleDimensions = Pair<Dp, Dp>

@Composable
fun RemoteScreen(
    callback: (Tag) -> Unit,
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .padding(16.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // D-pad with Volume and Channel controls
        DpadVolumeChannelsRow(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            remoteKeyPressed = callback
        )

        // Main controls: Back, Home, Settings
        MainControlsRow(
            iconSize = iconBig,
            remoteKeyPressed = callback
        )

        // Deeplinks: Apps, Recordings, Search, Info
        DeeplinksControlsRow(
            modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
            iconSize = iconMedium,
            remoteKeyPressed = callback
        )

        // Guide row: Guide, Menu, Assistant, Power
        GuideRow(
            modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
            iconSize = iconMedium,
            remoteKeyPressed = callback
        )

        // HbbTV color keys
        HbbTvControlsRow(
            modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
            iconSize = iconMedium,
            remoteKeyPressed = callback
        )

        // Numpad - using regular Column/Row instead of LazyVerticalGrid
        Numpad(
            modifier = Modifier.padding(top = 24.dp),
            onButtonClicked = callback,
            numpadButtonDimensions = StyleDimensions(60.dp, 60.dp),
            textColor = Color.White,
        )

        // Playback controls - using regular Row instead of LazyVerticalGrid
        PlaybackControlsRow(
            modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
            iconSize = iconMedium,
            remoteKeyPressed = callback
        )

        // External apps
        ExternalAppsRow(
            modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
            iconSize = iconSmall,
            remoteKeyPressed = callback
        )
    }
}

@Composable
fun Numpad(
    modifier: Modifier = Modifier.padding(16.dp),
    onButtonClicked: (Tag) -> Unit,
    numpadButtonDimensions: StyleDimensions = StyleDimensions(70.dp, 65.dp),
    iconSize: Dp = 20.dp,
    textColor: Color = Color.White,
) {
    val numpadTags = listOf(
        listOf(Tag.NUMBER_1, Tag.NUMBER_2, Tag.NUMBER_3),
        listOf(Tag.NUMBER_4, Tag.NUMBER_5, Tag.NUMBER_6),
        listOf(Tag.NUMBER_7, Tag.NUMBER_8, Tag.NUMBER_9),
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Numbers 1-9
        numpadTags.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                row.forEach { tag ->
                    NumpadKey(
                        text = tag.name.removePrefix("NUMBER_"),
                        textColor = textColor,
                        onClick = { onButtonClicked(tag) },
                        modifier = Modifier
                            .size(width = numpadButtonDimensions.first, height = numpadButtonDimensions.second)
                            .padding(4.dp),
                    )
                }
            }
        }

        // Bottom row: Teletext, 0, Previous
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            val key1 = teletextKey
            RemoteKey(
                modifier = Modifier.size(width = numpadButtonDimensions.first, height = numpadButtonDimensions.second),
                tag = key1.tag,
                icon = key1.icon,
                iconSize = 20.dp,
                iconTintColor = key1.tintColor,
                remoteKeyPressed = onButtonClicked
            )

            ZeroPad(
                text = "0",
                textColor = textColor,
                onClick = { onButtonClicked(Tag.NUMBER_0) },
                modifier = Modifier
                    .size(width = numpadButtonDimensions.first, height = numpadButtonDimensions.second)
                    .padding(4.dp),
            )

            val key2 = previousKey
            RemoteKey(
                modifier = Modifier.size(width = numpadButtonDimensions.first, height = numpadButtonDimensions.second),
                tag = key2.tag,
                icon = key2.icon,
                iconSize = iconSize,
                iconTintColor = key2.tintColor,
                remoteKeyPressed = onButtonClicked
            )
        }
    }
}

@Composable
fun PlaybackControlsRow(
    modifier: Modifier = Modifier,
    iconSize: Dp = 20.dp,
    remoteKeyPressed: (Tag) -> Unit
) {
    // First row: Rewind, Play/Pause, Fast Forward, Stop
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            playbackKeys.take(4).forEach { keyInfo ->
                RemoteKey(
                    tag = keyInfo.tag,
                    icon = keyInfo.icon,
                    iconSize = iconSize,
                    iconTintColor = keyInfo.tintColor,
                    remoteKeyPressed = remoteKeyPressed
                )
            }
        }

        // Second row: Previous, Next, Record, Captions
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            playbackKeys.drop(4).forEach { keyInfo ->
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
}
