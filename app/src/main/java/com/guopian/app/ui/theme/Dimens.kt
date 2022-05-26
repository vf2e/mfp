package com.guopian.app.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

object GuopianDimens {
    val Grid = 8.dp
    val Page = 16.dp
    val Module = 24.dp
    val ListGap = 12.dp
    val CardRadius = 16.dp
    val ButtonRadius = 12.dp
    val ControlRadius = 12.dp
    val Hairline = 1.dp
    val CardElevation = 2.dp
}

val GuopianCardShape = RoundedCornerShape(GuopianDimens.CardRadius)
val GuopianButtonShape = RoundedCornerShape(GuopianDimens.ButtonRadius)
val GuopianControlShape = RoundedCornerShape(GuopianDimens.ControlRadius)

@Composable
fun Modifier.guopianCard(
    elevation: Dp = GuopianDimens.CardElevation,
): Modifier {
    val panel = GuopianColors.Panel
    return this
        .shadow(
            elevation = elevation,
            shape = GuopianCardShape,
            clip = false,
            ambientColor = Color.Black.copy(alpha = 0.04f),
            spotColor = Color.Black.copy(alpha = 0.08f),
        )
        .clip(GuopianCardShape)
        .background(panel)
}
