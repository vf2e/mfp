package com.guotu.app.ui.components

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
import com.guotu.app.ui.theme.GuotuColors
import com.guotu.app.ui.theme.GuotuDimens

@Composable
fun GuotuPageHeader(
    title: String,
    subtitle: String? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(GuotuColors.Panel)
            .padding(horizontal = GuotuDimens.Page)
            .padding(top = GuotuDimens.Module, bottom = GuotuDimens.Page),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium,
                    color = GuotuColors.Slate,
                    fontWeight = FontWeight.Bold,
                )
                if (subtitle != null) {
                    Spacer(modifier = Modifier.height(GuotuDimens.Grid))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = GuotuColors.InkMute,
                    )
                }
            }
            Row(content = actions)
        }
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(GuotuDimens.Hairline)
            .background(GuotuColors.Rule),
    )
}

@Composable
fun GuotuEmptyState(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = GuotuDimens.Module, vertical = GuotuDimens.Module * 2),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            color = GuotuColors.Slate,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(GuotuDimens.Grid))
        Text(
            text = body,
            style = MaterialTheme.typography.bodyLarge,
            color = GuotuColors.InkMute,
        )
    }
}
