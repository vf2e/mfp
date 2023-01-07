package com.guopian.app.ui.categories

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.guopian.app.R
import com.guopian.app.core.util.PathAbbreviator
import com.guopian.app.ui.components.GuopianEmptyState
import com.guopian.app.ui.components.GuopianPageHeader
import com.guopian.app.ui.theme.GuopianColors
import com.guopian.app.ui.theme.GuopianDimens
import com.guopian.app.ui.theme.guopianCard

@Composable
fun CategoriesScreen(
    onOpenLibrary: () -> Unit,
    viewModel: CategoriesViewModel = hiltViewModel(),
) {
    val ui by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GuopianColors.Canvas),
    ) {
        GuopianPageHeader(
            title = stringResource(R.string.nav_categories),
            subtitle = if (ui.hasRoot) {
                stringResource(R.string.categories_subtitle_ready, ui.total)
            } else {
                stringResource(R.string.categories_subtitle)
            },
        )

        when {
            !ui.hasRoot -> {
                GuopianEmptyState(
                    title = stringResource(R.string.categories_empty_title),
                    body = stringResource(R.string.categories_need_root),
                    modifier = Modifier.fillMaxSize(),
                )
            }
            ui.total <= 0 -> {
                GuopianEmptyState(
                    title = stringResource(R.string.categories_empty_title),
                    body = stringResource(R.string.categories_need_scan),
                    modifier = Modifier.fillMaxSize(),
                )
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        horizontal = GuopianDimens.Page,
                        vertical = GuopianDimens.Page,
                    ),
                    verticalArrangement = Arrangement.spacedBy(GuopianDimens.Module),
                ) {
                    item {
                        CategorySection(
                            title = stringResource(R.string.categories_section_status),
                            buckets = ui.statusBuckets,
                            onClick = {
                                viewModel.openBucket(it)
                                onOpenLibrary()
                            },
                        )
                    }
                    if (ui.extBuckets.isNotEmpty()) {
                        item {
                            CategorySection(
                                title = stringResource(R.string.categories_section_format),
                                buckets = ui.extBuckets,
                                onClick = {
                                    viewModel.openBucket(it)
                                    onOpenLibrary()
                                },
                            )
                        }
                    }
                    if (ui.folderBuckets.isNotEmpty()) {
                        item {
                            CategorySection(
                                title = stringResource(R.string.categories_section_folder),
                                buckets = ui.folderBuckets,
                                abbreviatePath = true,
                                onClick = {
                                    viewModel.openBucket(it)
                                    onOpenLibrary()
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategorySection(
    title: String,
    buckets: List<CategoryBucket>,
    abbreviatePath: Boolean = false,
    onClick: (CategoryBucket) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(GuopianDimens.ListGap)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = GuopianColors.Ink,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = GuopianDimens.Grid),
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .guopianCard(),
        ) {
            buckets.forEachIndexed { index, bucket ->
                CategoryRow(
                    title = if (abbreviatePath) PathAbbreviator.abbreviate(bucket.title) else bucket.title,
                    count = bucket.count,
                    onClick = { onClick(bucket) },
                )
                if (index != buckets.lastIndex) {
                    Spacer(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(GuopianDimens.Hairline)
                            .background(GuopianColors.Rule),
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryRow(
    title: String,
    count: Int,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = GuopianDimens.Page, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = GuopianColors.Ink,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = stringResource(R.string.category_count, count),
            style = MaterialTheme.typography.labelLarge,
            color = GuopianColors.InkMute,
        )
        Icon(
            imageVector = Icons.Outlined.ChevronRight,
            contentDescription = null,
            tint = GuopianColors.InkMute,
            modifier = Modifier
                .padding(start = GuopianDimens.Grid)
                .size(18.dp),
        )
    }
}
