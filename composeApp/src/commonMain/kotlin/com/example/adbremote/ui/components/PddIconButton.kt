package com.example.adbremote.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.adbremote.ui.rcu.backgroundColor

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
