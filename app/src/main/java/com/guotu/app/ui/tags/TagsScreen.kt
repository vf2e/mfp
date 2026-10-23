package com.guotu.app.ui.tags

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.guotu.app.R
import com.guotu.app.data.db.TagWithCount
import com.guotu.app.ui.components.GuotuEmptyState
import com.guotu.app.ui.components.GuotuPageHeader
import com.guotu.app.ui.theme.GuotuButtonShape
import com.guotu.app.ui.theme.GuotuColors
import com.guotu.app.ui.theme.GuotuDimens
import com.guotu.app.ui.theme.guotuCard

@Composable
fun TagsScreen(
    onOpenLibrary: () -> Unit,
    viewModel: TagsViewModel = hiltViewModel(),
) {
    val tags by viewModel.tags.collectAsStateWithLifecycle()
    val ui by viewModel.ui.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GuotuColors.Canvas),
    ) {
        GuotuPageHeader(
            title = stringResource(R.string.nav_tags),
            subtitle = stringResource(R.string.tags_subtitle_ready, tags.size),
            actions = {
                IconButton(onClick = viewModel::openCreate) {
                    Icon(
                        imageVector = Icons.Outlined.Add,
                        contentDescription = stringResource(R.string.tags_create),
                        tint = GuotuColors.Signal,
                    )
                }
            },
        )

        if (tags.isEmpty()) {
            GuotuEmptyState(
                title = stringResource(R.string.tags_empty_title),
                body = stringResource(R.string.tags_empty_body),
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    horizontal = GuotuDimens.Page,
                    vertical = GuotuDimens.Page,
                ),
                verticalArrangement = Arrangement.spacedBy(GuotuDimens.ListGap),
            ) {
                items(tags, key = { it.id }) { tag ->
                    TagRow(
                        tag = tag,
                        onOpen = {
                            viewModel.openTag(tag)
                            onOpenLibrary()
                        },
                        onDelete = { viewModel.deleteTag(tag.id) },
                    )
                }
            }
        }
    }

    if (ui.showCreate) {
        CreateTagDialog(
            name = ui.draftName,
            color = ui.draftColor,
            onNameChange = viewModel::setDraftName,
            onColorChange = viewModel::setDraftColor,
            onDismiss = viewModel::dismissCreate,
            onConfirm = viewModel::createTag,
        )
    }
}

@Composable
private fun TagRow(
    tag: TagWithCount,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
) {
    val swatch = runCatching { Color(android.graphics.Color.parseColor(tag.color)) }
        .getOrDefault(GuotuColors.Signal)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .guotuCard()
            .clickable(onClick = onOpen)
            .padding(horizontal = GuotuDimens.Page, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(swatch),
        )
        Spacer(modifier = Modifier.size(GuotuDimens.ListGap))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = tag.name,
                style = MaterialTheme.typography.titleMedium,
                color = GuotuColors.Ink,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.category_count, tag.imageCount),
                style = MaterialTheme.typography.bodySmall,
                color = GuotuColors.InkMute,
            )
        }
        IconButton(onClick = onDelete) {
            Icon(
                imageVector = Icons.Outlined.Delete,
                contentDescription = stringResource(R.string.tags_delete),
                tint = GuotuColors.InkMute,
            )
        }
        Icon(
            imageVector = Icons.Outlined.ChevronRight,
            contentDescription = null,
            tint = GuotuColors.InkMute,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun CreateTagDialog(
    name: String,
    color: String,
    onNameChange: (String) -> Unit,
    onColorChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .guotuCard()
                .padding(GuotuDimens.Page),
        ) {
            Text(
                text = stringResource(R.string.tags_create),
                style = MaterialTheme.typography.titleMedium,
                color = GuotuColors.Ink,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(GuotuDimens.ListGap))
            BasicTextField(
                value = name,
                onValueChange = onNameChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = GuotuColors.Ink),
                cursorBrush = SolidColor(GuotuColors.Signal),
                decorationBox = { inner ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(GuotuButtonShape)
                            .background(GuotuColors.Canvas)
                            .padding(horizontal = GuotuDimens.Page, vertical = 12.dp),
                    ) {
                        if (name.isEmpty()) {
                            Text(
                                text = stringResource(R.string.tags_name_hint),
                                color = GuotuColors.InkMute,
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                        inner()
                    }
                },
            )
            Spacer(modifier = Modifier.height(GuotuDimens.ListGap))
            Text(
                text = stringResource(R.string.tags_color),
                style = MaterialTheme.typography.labelLarge,
                color = GuotuColors.InkMute,
            )
            Spacer(modifier = Modifier.height(GuotuDimens.Grid))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(GuotuDimens.Grid)) {
                items(TAG_COLOR_PALETTE) { hex ->
                    val c = Color(android.graphics.Color.parseColor(hex))
                    val selected = hex.equals(color, ignoreCase = true)
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(c)
                            .then(
                                if (selected) {
                                    Modifier.border(2.dp, GuotuColors.Ink, CircleShape)
                                } else {
                                    Modifier
                                },
                            )
                            .clickable { onColorChange(hex) },
                    )
                }
            }
            Spacer(modifier = Modifier.height(GuotuDimens.Page))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onDismiss) {
                    Text(text = stringResource(R.string.action_cancel), color = GuotuColors.InkMute)
                }
                TextButton(
                    onClick = onConfirm,
                    enabled = name.trim().isNotEmpty(),
                ) {
                    Text(
                        text = stringResource(R.string.action_save),
                        color = GuotuColors.Signal,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}
