package com.guopian.app.ui.tags

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
import com.guopian.app.R
import com.guopian.app.data.db.TagWithCount
import com.guopian.app.ui.components.GuopianEmptyState
import com.guopian.app.ui.components.GuopianPageHeader
import com.guopian.app.ui.theme.GuopianButtonShape
import com.guopian.app.ui.theme.GuopianColors
import com.guopian.app.ui.theme.GuopianDimens
import com.guopian.app.ui.theme.guopianCard

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
            .background(GuopianColors.Canvas),
    ) {
        GuopianPageHeader(
            title = stringResource(R.string.nav_tags),
            subtitle = stringResource(R.string.tags_subtitle_ready, tags.size),
            actions = {
                IconButton(onClick = viewModel::openCreate) {
                    Icon(
                        imageVector = Icons.Outlined.Add,
                        contentDescription = stringResource(R.string.tags_create),
                        tint = GuopianColors.Signal,
                    )
                }
            },
        )

        if (tags.isEmpty()) {
            GuopianEmptyState(
                title = stringResource(R.string.tags_empty_title),
                body = stringResource(R.string.tags_empty_body),
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    horizontal = GuopianDimens.Page,
                    vertical = GuopianDimens.Page,
                ),
                verticalArrangement = Arrangement.spacedBy(GuopianDimens.ListGap),
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
        .getOrDefault(GuopianColors.Signal)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .guopianCard()
            .clickable(onClick = onOpen)
            .padding(horizontal = GuopianDimens.Page, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(swatch),
        )
        Spacer(modifier = Modifier.size(GuopianDimens.ListGap))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = tag.name,
                style = MaterialTheme.typography.titleMedium,
                color = GuopianColors.Ink,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.category_count, tag.videoCount),
                style = MaterialTheme.typography.bodySmall,
                color = GuopianColors.InkMute,
            )
        }
        IconButton(onClick = onDelete) {
            Icon(
                imageVector = Icons.Outlined.Delete,
                contentDescription = stringResource(R.string.tags_delete),
                tint = GuopianColors.InkMute,
            )
        }
        Icon(
            imageVector = Icons.Outlined.ChevronRight,
            contentDescription = null,
            tint = GuopianColors.InkMute,
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
                .guopianCard()
                .padding(GuopianDimens.Page),
        ) {
            Text(
                text = stringResource(R.string.tags_create),
                style = MaterialTheme.typography.titleMedium,
                color = GuopianColors.Ink,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(GuopianDimens.ListGap))
            BasicTextField(
                value = name,
                onValueChange = onNameChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = GuopianColors.Ink),
                cursorBrush = SolidColor(GuopianColors.Signal),
                decorationBox = { inner ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(GuopianButtonShape)
                            .background(GuopianColors.Canvas)
                            .padding(horizontal = GuopianDimens.Page, vertical = 12.dp),
                    ) {
                        if (name.isEmpty()) {
                            Text(
                                text = stringResource(R.string.tags_name_hint),
                                color = GuopianColors.InkMute,
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                        inner()
                    }
                },
            )
            Spacer(modifier = Modifier.height(GuopianDimens.ListGap))
            Text(
                text = stringResource(R.string.tags_color),
                style = MaterialTheme.typography.labelLarge,
                color = GuopianColors.InkMute,
            )
            Spacer(modifier = Modifier.height(GuopianDimens.Grid))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(GuopianDimens.Grid)) {
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
                                    Modifier.border(2.dp, GuopianColors.Ink, CircleShape)
                                } else {
                                    Modifier
                                },
                            )
                            .clickable { onColorChange(hex) },
                    )
                }
            }
            Spacer(modifier = Modifier.height(GuopianDimens.Page))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onDismiss) {
                    Text(text = stringResource(R.string.action_cancel), color = GuopianColors.InkMute)
                }
                TextButton(
                    onClick = onConfirm,
                    enabled = name.trim().isNotEmpty(),
                ) {
                    Text(
                        text = stringResource(R.string.action_save),
                        color = GuopianColors.Signal,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}
