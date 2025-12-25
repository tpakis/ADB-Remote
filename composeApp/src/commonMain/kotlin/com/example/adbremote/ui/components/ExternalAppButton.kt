package com.example.adbremote.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.adbremote.ui.rcu.RcuKeyInfo
import com.example.adbremote.ui.rcu.Tag

@Composable
fun ExternalAppButton(
    modifier: Modifier = Modifier,
    rcuKeyInfo: RcuKeyInfo,
    onClick: (Tag) -> Unit,
    backgroundColor: Color = Color.Transparent,
    iconSize: Dp = 10.dp,
    fontColor: Color = Color.White,
) {
    Button(
        onClick = { onClick(rcuKeyInfo.tag) },
        modifier = modifier
            .fillMaxWidth()
            .shadow(0.dp),
        shape = RoundedCornerShape(28.dp),
        contentPadding = PaddingValues(6.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = backgroundColor,
            contentColor = fontColor
        ),
        border = BorderStroke(1.dp, Color.White)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.CenterStart),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = rcuKeyInfo.icon,
                    modifier = Modifier.size(iconSize),
                    contentDescription = rcuKeyInfo.title,
                    tint = rcuKeyInfo.tintColor
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = rcuKeyInfo.title,
                    textAlign = TextAlign.Center,
                    fontSize = 8.sp,
                )
            }

        }
    }
}
