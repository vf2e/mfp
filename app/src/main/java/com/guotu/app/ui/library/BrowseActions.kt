package com.guotu.app.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.guotu.app.R
import com.guotu.app.ui.theme.GuotuButtonShape
import com.guotu.app.ui.theme.GuotuColors
import com.guotu.app.ui.theme.GuotuDimens

@Composable
fun BrowseActionRow(
    hasLast: Boolean,
    toDeleteCount: Int,
    keepCount: Int,
    browsingEnabled: Boolean,
    onStartBrowse: () -> Unit,
    onResumeLast: () -> Unit,
    onCutKept: () -> Unit,
    onCutFiltered: () -> Unit,
    onPurgeToDelete: () -> Unit,
    onExportKeep: () -> Unit,
) {
    var moreExpanded by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = GuotuDimens.Page, vertical = GuotuDimens.Grid),
        verticalArrangement = Arrangement.spacedBy(GuotuDimens.Grid),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(GuotuDimens.ListGap)) {
            Text(
                text = stringResource(R.string.action_start_browse),
                modifier = Modifier
                    .weight(1f)
                    .alpha(if (browsingEnabled) 1f else 0.45f)
                    .clip(GuotuButtonShape)
                    .background(GuotuColors.Signal)
                    .clickable(enabled = browsingEnabled, onClick = onStartBrowse)
                    .padding(vertical = 12.dp),
                style = MaterialTheme.typography.labelLarge,
                color = GuotuColors.OnSignal,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
            if (hasLast) {
                Text(
                    text = stringResource(R.string.action_resume_last),
                    modifier = Modifier
                        .weight(1f)
                        .alpha(if (browsingEnabled) 1f else 0.45f)
                        .clip(GuotuButtonShape)
                        .border(GuotuDimens.Hairline, GuotuColors.Signal, GuotuButtonShape)
                        .clickable(enabled = browsingEnabled, onClick = onResumeLast)
                        .padding(vertical = 12.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = GuotuColors.Signal,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                )
            }
        }
        Box {
            Text(
                text = stringResource(R.string.action_more),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(GuotuButtonShape)
                    .border(GuotuDimens.Hairline, GuotuColors.Rule, GuotuButtonShape)
                    .clickable { moreExpanded = true }
                    .padding(vertical = 12.dp),
                style = MaterialTheme.typography.labelLarge,
                color = GuotuColors.Ink,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
            DropdownMenu(
                expanded = moreExpanded,
                onDismissRequest = { moreExpanded = false },
            ) {
                DropdownMenuItem(
                    text = {
                        Text(
                            stringResource(R.string.action_cut_kept) +
                                if (keepCount > 0) " ($keepCount)" else "",
                        )
                    },
                    onClick = {
                        moreExpanded = false
                        onCutKept()
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.action_cut_filtered)) },
                    onClick = {
                        moreExpanded = false
                        onCutFiltered()
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.action_export_keep)) },
                    onClick = {
                        moreExpanded = false
                        onExportKeep()
                    },
                )
                DropdownMenuItem(
                    text = {
                        Text(
                            text = stringResource(R.string.action_purge_delete) +
                                if (toDeleteCount > 0) " ($toDeleteCount)" else "",
                            color = GuotuColors.Danger,
                        )
                    },
                    enabled = toDeleteCount > 0,
                    onClick = {
                        moreExpanded = false
                        onPurgeToDelete()
                    },
                )
            }
        }
    }
    Text(
        text = stringResource(R.string.browse_hint),
        modifier = Modifier.padding(horizontal = GuotuDimens.Page, vertical = 4.dp),
        style = MaterialTheme.typography.bodySmall,
        color = GuotuColors.InkMute,
    )
}

@Composable
fun ScanBanner(found: Int) {
    Text(
        text = stringResource(R.string.scanning_progress, found),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = GuotuDimens.Page, vertical = 6.dp)
            .clip(GuotuButtonShape)
            .background(GuotuColors.SignalSoft)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        style = MaterialTheme.typography.bodySmall,
        color = GuotuColors.Signal,
        fontWeight = FontWeight.SemiBold,
    )
}
