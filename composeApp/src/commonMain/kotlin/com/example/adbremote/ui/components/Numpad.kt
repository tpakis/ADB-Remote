package com.example.adbremote.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun NumpadKey(
    modifier: Modifier = Modifier
        .size(width = 70.dp, height = 65.dp)
        .padding(4.dp),
    text: String,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit,
) {
    Button(
        modifier = modifier,
        onClick = onClick,
        shape = RoundedCornerShape(20),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = textColor
        )
    ) {
        Text(
            text = text,
            fontSize = 20.sp,
            style = TextStyle.Default.copy(fontWeight = FontWeight.Normal),
        )
    }
}

@Preview
@Composable
fun NumpadKeyPreview() {
    NumpadKey(
        text = "5",
        onClick = {}
    )
}

@Composable
fun ZeroPad(
    modifier: Modifier = Modifier
        .size(width = 70.dp, height = 65.dp)
        .padding(4.dp),
    text: String,
    textColor: Color = Color.White,
    onClick: () -> Unit,
) {
    Button(
        modifier = modifier,
        onClick = onClick,
        shape = RoundedCornerShape(20),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Text(
            text = text,
            fontSize = 20.sp,
            color = textColor,
            style = TextStyle.Default.copy(fontWeight = FontWeight.Normal),
        )
    }
}
