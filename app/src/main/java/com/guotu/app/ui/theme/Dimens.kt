package com.guotu.app.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

object GuotuDimens {
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

val GuotuCardShape = RoundedCornerShape(GuotuDimens.CardRadius)
val GuotuButtonShape = RoundedCornerShape(GuotuDimens.ButtonRadius)
val GuotuControlShape = RoundedCornerShape(GuotuDimens.ControlRadius)

@Composable
fun Modifier.guotuCard(
    elevation: Dp = GuotuDimens.CardElevation,
): Modifier {
    val panel = GuotuColors.Panel
    return this
        .shadow(
            elevation = elevation,
            shape = GuotuCardShape,
            clip = false,
            ambientColor = Color.Black.copy(alpha = 0.04f),
            spotColor = Color.Black.copy(alpha = 0.08f),
        )
        .clip(GuotuCardShape)
        .background(panel)
}
