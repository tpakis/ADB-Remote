package com.example.adbremote.ui.remote

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Adjust
import androidx.compose.material.icons.filled.FilterTiltShift
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.adbremote.ui.components.PddIconButton
import com.example.adbremote.ui.rcu.Tag
import com.example.adbremote.ui.rcu.backgroundColor

@Composable
fun DpadVolumeChannelsRow(modifier: Modifier = Modifier.fillMaxWidth(), remoteKeyPressed: (Tag) -> Unit) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        VolumeKeys(remoteKeyPressed)
        DpadKeys(remoteKeyPressed)
        ChannelKeys(remoteKeyPressed)
    }
}

@Composable
private fun RowScope.VolumeKeys(remoteKeyPressed: (Tag) -> Unit) {
    Column(
        modifier = Modifier
            .size(height = 150.dp, width = 10.dp)
            .background(shape = CircleShape, color = backgroundColor())
            .weight(weight = 0.25f),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        PddIconButton(
            imageVector = Icons.Filled.Add,
        ) {
            remoteKeyPressed(Tag.VOLUME_UP)
        }

        // Mute button - using text instead of missing icon
        PddIconButton(
            imageVector = Icons.Filled.VolumeMute,
        ) {
            remoteKeyPressed(Tag.VOLUME_MUTE)
        }

        PddIconButton(
            imageVector = Icons.Outlined.KeyboardArrowDown,
        ) {
            remoteKeyPressed(Tag.VOLUME_DOWN)
        }
    }
}

@Composable
fun RowScope.DpadKeys(remoteKeyPressed: (Tag) -> Unit) {
    Box(
        modifier = Modifier
            .size(width = 100.dp, height = 150.dp)
            .aspectRatio(1.1f, matchHeightConstraintsFirst = true)
            .weight(1f)
            .padding(6.dp)
            .background(shape = CircleShape, color = backgroundColor()),
    ) {

        PddIconButton(
            modifier = Modifier.align(Alignment.TopCenter).padding(bottom = 8.dp),
            imageVector = Icons.Outlined.KeyboardArrowUp,
        ) {
            remoteKeyPressed(Tag.DPAD_UP)
        }

        PddIconButton(
            modifier = Modifier.align(Alignment.BottomCenter),
            imageVector = Icons.Outlined.KeyboardArrowDown,
        ) {
            remoteKeyPressed(Tag.DPAD_DOWN)
        }

        // Center/OK button
        PddIconButton(
            modifier = Modifier.align(Alignment.Center).size(30.dp),
            imageVector = Icons.Default.Adjust,
            iconSize = 20.dp,
        ) {
            remoteKeyPressed(Tag.DPAD_CENTER)
        }

        PddIconButton(
            modifier = Modifier.align(Alignment.CenterStart),
            imageVector = Icons.Outlined.KeyboardArrowLeft,
        ) {
            remoteKeyPressed(Tag.DPAD_LEFT)
        }

        PddIconButton(
            modifier = Modifier.align(Alignment.CenterEnd),
            imageVector = Icons.Outlined.KeyboardArrowRight,
        ) {
            remoteKeyPressed(Tag.DPAD_RIGHT)
        }
    }
}

@Composable
private fun RowScope.ChannelKeys(remoteKeyPressed: (Tag) -> Unit) {
    Column(
        modifier = Modifier.size(height = 150.dp, width = 10.dp)
            .background(shape = CircleShape, color = backgroundColor())
            .weight(weight = 0.25f),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        PddIconButton(
            imageVector = Icons.Filled.KeyboardArrowUp,
        ) {
            remoteKeyPressed(Tag.CHANNEL_UP)
        }

        Text(text = "CH", fontSize = 12.sp, color = Color.White)

        PddIconButton(
            imageVector = Icons.Filled.KeyboardArrowDown,
        ) {
            remoteKeyPressed(Tag.CHANNEL_DOWN)
        }
    }
}
