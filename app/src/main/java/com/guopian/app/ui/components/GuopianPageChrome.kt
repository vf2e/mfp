package com.guopian.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.guopian.app.ui.theme.GuopianColors
import com.guopian.app.ui.theme.GuopianDimens

@Composable
fun GuopianPageHeader(
    title: String,
    subtitle: String? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(GuopianColors.Panel)
            .padding(horizontal = GuopianDimens.Page)
            .padding(top = GuopianDimens.Module, bottom = GuopianDimens.Page),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium,
                    color = GuopianColors.Slate,
                    fontWeight = FontWeight.Bold,
                )
                if (subtitle != null) {
                    Spacer(modifier = Modifier.height(GuopianDimens.Grid))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = GuopianColors.InkMute,
                    )
                }
            }
            Row(content = actions)
        }
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(GuopianDimens.Hairline)
            .background(GuopianColors.Rule),
    )
}

@Composable
fun GuopianEmptyState(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = GuopianDimens.Module, vertical = GuopianDimens.Module * 2),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            color = GuopianColors.Slate,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(GuopianDimens.Grid))
        Text(
            text = body,
            style = MaterialTheme.typography.bodyLarge,
            color = GuopianColors.InkMute,
        )
    }
}
