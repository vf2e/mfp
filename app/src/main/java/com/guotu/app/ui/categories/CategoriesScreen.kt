package com.guotu.app.ui.categories

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
import com.guotu.app.R
import com.guotu.app.core.util.PathAbbreviator
import com.guotu.app.ui.components.GuotuEmptyState
import com.guotu.app.ui.components.GuotuPageHeader
import com.guotu.app.ui.theme.GuotuColors
import com.guotu.app.ui.theme.GuotuDimens
import com.guotu.app.ui.theme.guotuCard

@Composable
fun CategoriesScreen(
    onOpenLibrary: () -> Unit,
    viewModel: CategoriesViewModel = hiltViewModel(),
) {
    val ui by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GuotuColors.Canvas),
    ) {
        GuotuPageHeader(
            title = stringResource(R.string.nav_categories),
            subtitle = if (ui.hasRoot) {
                stringResource(R.string.categories_subtitle_ready, ui.total)
            } else {
                stringResource(R.string.categories_subtitle)
            },
        )

        when {
            !ui.hasRoot -> {
                GuotuEmptyState(
                    title = stringResource(R.string.categories_empty_title),
                    body = stringResource(R.string.categories_need_root),
                    modifier = Modifier.fillMaxSize(),
                )
            }
            ui.total <= 0 -> {
                GuotuEmptyState(
                    title = stringResource(R.string.categories_empty_title),
                    body = stringResource(R.string.categories_need_scan),
                    modifier = Modifier.fillMaxSize(),
                )
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        horizontal = GuotuDimens.Page,
                        vertical = GuotuDimens.Page,
                    ),
                    verticalArrangement = Arrangement.spacedBy(GuotuDimens.Module),
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
    Column(verticalArrangement = Arrangement.spacedBy(GuotuDimens.ListGap)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = GuotuColors.Ink,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = GuotuDimens.Grid),
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .guotuCard(),
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
                            .height(GuotuDimens.Hairline)
                            .background(GuotuColors.Rule),
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
            .padding(horizontal = GuotuDimens.Page, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = GuotuColors.Ink,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = stringResource(R.string.category_count, count),
            style = MaterialTheme.typography.labelLarge,
            color = GuotuColors.InkMute,
        )
        Icon(
            imageVector = Icons.Outlined.ChevronRight,
            contentDescription = null,
            tint = GuotuColors.InkMute,
            modifier = Modifier
                .padding(start = GuotuDimens.Grid)
                .size(18.dp),
        )
    }
}
