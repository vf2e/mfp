package com.guopian.app.ui.cut

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.guopian.app.R
import com.guopian.app.data.db.FolderPresetEntity
import com.guopian.app.domain.model.ConflictPolicy
import com.guopian.app.ui.components.GuopianHairline
import com.guopian.app.ui.components.GuopianSegmentedRail
import com.guopian.app.ui.theme.GuopianButtonShape
import com.guopian.app.ui.theme.GuopianColors
import com.guopian.app.ui.theme.GuopianDimens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CutTargetSheet(
    count: Int,
    presets: List<FolderPresetEntity>,
    policy: ConflictPolicy,
    onPolicyChange: (ConflictPolicy) -> Unit,
    onPickFolder: () -> Unit,
    onPresetClick: (FolderPresetEntity) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = GuopianColors.Panel,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = GuopianDimens.Page)
                .padding(bottom = GuopianDimens.Module),
        ) {
            Text(
                text = stringResource(R.string.cut_title, count),
                style = MaterialTheme.typography.headlineMedium,
                color = GuopianColors.Slate,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(GuopianDimens.Grid))
            Text(
                text = stringResource(R.string.cut_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = GuopianColors.InkMute,
            )
            Spacer(modifier = Modifier.height(GuopianDimens.Module))
            Text(
                text = stringResource(R.string.cut_conflict_policy),
                style = MaterialTheme.typography.labelLarge,
                color = GuopianColors.InkMute,
            )
            Spacer(modifier = Modifier.height(GuopianDimens.ListGap))
            GuopianSegmentedRail(
                options = listOf(
                    ConflictPolicy.RENAME to stringResource(R.string.conflict_rename),
                    ConflictPolicy.SKIP to stringResource(R.string.conflict_skip),
                    ConflictPolicy.OVERWRITE to stringResource(R.string.conflict_overwrite),
                ),
                selected = policy,
                onSelect = onPolicyChange,
            )
            Spacer(modifier = Modifier.height(GuopianDimens.Module))
            Text(
                text = stringResource(R.string.cut_pick_folder),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(GuopianButtonShape)
                    .background(GuopianColors.Signal)
                    .clickable(onClick = onPickFolder)
                    .padding(vertical = 14.dp),
                style = MaterialTheme.typography.titleMedium,
                color = GuopianColors.OnSignal,
                fontWeight = FontWeight.SemiBold,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            if (presets.isNotEmpty()) {
                Spacer(modifier = Modifier.height(GuopianDimens.Module))
                Text(
                    text = stringResource(R.string.cut_recent_folders),
                    style = MaterialTheme.typography.labelLarge,
                    color = GuopianColors.InkMute,
                )
                Spacer(modifier = Modifier.height(GuopianDimens.Grid))
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                ) {
                    items(presets, key = { it.id }) { preset ->
                        Text(
                            text = preset.name,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onPresetClick(preset) }
                                .padding(vertical = 14.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodyLarge,
                            color = GuopianColors.Ink,
                        )
                        GuopianHairline(startInset = false)
                    }
                }
            }
            Spacer(modifier = Modifier.height(GuopianDimens.Grid))
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel), color = GuopianColors.InkMute)
            }
        }
    }
}
