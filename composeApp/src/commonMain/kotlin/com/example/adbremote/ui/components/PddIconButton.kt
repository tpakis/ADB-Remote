package com.example.adbremote.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.BlurOff
import androidx.compose.material.icons.outlined.FilterTiltShift
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.adbremote.ui.rcu.backgroundColor
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun PddIconButton(
    modifier: Modifier = Modifier,
    imageVector: ImageVector? = null,
    iconSize: Dp = 16.dp,
    onClick: () -> Unit,
) {
    IconButton(
        modifier = modifier
            .background(shape = CircleShape, color = backgroundColor()),
        onClick = onClick,
    ) {
        imageVector?.let {
            Icon(
                modifier = Modifier.padding(8.dp).size(iconSize),
                imageVector = it,
                tint = Color.White,
                contentDescription = null,
            )
        }
    }
}

@Preview
@Composable
fun PddIconButtonPreview() {
    PddIconButton(
        imageVector = Icons.Outlined.FilterTiltShift,
        onClick = {}
    )
}