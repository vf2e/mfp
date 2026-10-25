package com.guotu.app.ui.library

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import com.guotu.app.R
import com.guotu.app.core.util.FileSizeFormatter
import com.guotu.app.core.util.PathAbbreviator
import com.guotu.app.core.util.StorageAccessHelper
import com.guotu.app.data.db.ImageEntity
import com.guotu.app.domain.model.MarkStatus
import com.guotu.app.domain.model.SortKey
import com.guotu.app.ui.cut.CutTargetSheet
import com.guotu.app.ui.theme.GuotuButtonShape
import com.guotu.app.ui.theme.GuotuCardShape
import com.guotu.app.ui.theme.GuotuColors
import com.guotu.app.ui.theme.GuotuDimens
import com.guotu.app.ui.theme.guotuCard
import com.guotu.app.ui.util.OpenDocumentTreeContract
import com.guotu.app.ui.util.launchDocumentTreePreferAndroid
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LibraryScreen(
    onOpenSwipe: (startIndex: Int) -> Unit,
    showTagsFeatures: Boolean = false,
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val images = viewModel.images.collectAsLazyPagingItems()
    val presets by viewModel.folderPresets.collectAsStateWithLifecycle()
    val tags by viewModel.tags.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var showSearch by remember { mutableStateOf(false) }
    var showTagPicker by remember { mutableStateOf(false) }
    var showPurgeConfirm by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    val context = LocalContext.current
    var showFileFolderPicker by remember { mutableStateOf(false) }
    val openTree = rememberLauncherForActivityResult(
        contract = OpenDocumentTreeContract(),
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.onRootSelected(uri)
        }
    }

    val pickCutTarget = rememberLauncherForActivityResult(
        contract = OpenDocumentTreeContract(),
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.enqueueCut(uri)
        }
    }

    fun pickRootFolder() {
        if (StorageAccessHelper.hasAllFilesAccess()) {
            showFileFolderPicker = true
        } else {
            openTree.launchDocumentTreePreferAndroid()
        }
    }

    if (showFileFolderPicker) {
        FileFolderPickerDialog(
            onDismiss = { showFileFolderPicker = false },
            onConfirm = { path ->
                showFileFolderPicker = false
                viewModel.onRootFilePathSelected(path)
            },
        )
    }

    if (showPurgeConfirm) {
        val count = uiState.stats.toDelete.coerceAtMost(2000)
        AlertDialog(
            onDismissRequest = { showPurgeConfirm = false },
            title = { Text(stringResource(R.string.purge_confirm_title)) },
            text = { Text(stringResource(R.string.purge_confirm_body, count)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showPurgeConfirm = false
                        viewModel.purgeToDelete()
                    },
                ) {
                    Text(stringResource(R.string.purge_confirm_ok), color = GuotuColors.Danger)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPurgeConfirm = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }

    LaunchedEffect(uiState.cutMessage) {
        val msg = uiState.cutMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        viewModel.clearCutMessage()
    }
    LaunchedEffect(uiState.actionMessage) {
        val msg = uiState.actionMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        viewModel.clearActionMessage()
    }

    LaunchedEffect(uiState.scanResultMessage) {
        val msg = uiState.scanResultMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        viewModel.clearScanResultMessage()
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.refreshLastCursor()
        }
    }

    if (uiState.showCutSheet) {
        CutTargetSheet(
            count = uiState.selectedIds.size,
            presets = presets,
            policy = uiState.conflictPolicy,
            onPolicyChange = viewModel::setConflictPolicy,
            onPickFolder = { pickCutTarget.launchDocumentTreePreferAndroid() },
            onPresetClick = viewModel::enqueueCutPreset,
            onDismiss = viewModel::dismissCutSheet,
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = GuotuColors.Canvas,
        topBar = {
            LibraryBrandHeader(
                rootPath = uiState.rootPath,
                hasRoot = uiState.hasRoot,
                showSearch = showSearch,
                onToggleSearch = { showSearch = !showSearch },
                onScan = viewModel::scanNow,
                onPickRoot = { pickRootFolder() },
            )
        },
        bottomBar = {
            if (uiState.selectionMode) {
                SelectionBar(
                    count = uiState.selectedIds.size,
                    canTag = showTagsFeatures && tags.isNotEmpty(),
                    onSelectAll = viewModel::selectAllFiltered,
                    onCut = viewModel::openCutSheet,
                    onKeep = { viewModel.markSelected(MarkStatus.KEEP) },
                    onDelete = { viewModel.markSelected(MarkStatus.TO_DELETE) },
                    onTag = { showTagPicker = true },
                    onClear = viewModel::clearSelection,
                )
            }
        },
    ) { padding ->
        if (showTagPicker) {
            TagPickerDialog(
                tags = tags,
                onDismiss = { showTagPicker = false },
                onPick = { tagId ->
                    viewModel.tagSelected(tagId)
                    showTagPicker = false
                },
            )
        }
        if (!uiState.hasRoot) {
            EmptyRootState(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                onSelect = { pickRootFolder() },
                onGrantAllFiles = {
                    runCatching {
                        context.startActivity(StorageAccessHelper.manageAllFilesIntent(context))
                    }
                },
                showGrantAllFiles = !StorageAccessHelper.hasAllFilesAccess(),
            )
        } else {
            LibraryContent(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                uiState = uiState,
                images = images,
                showSearch = showSearch,
                onQuickFilter = viewModel::setQuickFilter,
                onClearExternalFilter = viewModel::clearExternalFilter,
                onPathKeyword = viewModel::setPathKeyword,
                onQueryText = viewModel::setQueryText,
                onScan = viewModel::scanNow,
                onToggleSelect = viewModel::toggleSelect,
                onEnterSelection = viewModel::enterSelection,
                onStartBrowse = {
                    scope.launch {
                        val start = viewModel.startBrowse() ?: return@launch
                        onOpenSwipe(start)
                    }
                },
                onResumeLast = {
                    scope.launch {
                        val start = viewModel.resumeLastBrowse()
                        if (start == null) {
                            snackbarHostState.showSnackbar("还没有上次浏览记录")
                            return@launch
                        }
                        onOpenSwipe(start)
                    }
                },
                onSetSort = viewModel::setSort,
                onCutKept = viewModel::prepareCutKept,
                onCutFiltered = viewModel::prepareCutCurrentFilter,
                onPurgeToDelete = { showPurgeConfirm = true },
                onExportKeep = viewModel::exportKeepList,
                browsingEnabled = !uiState.isScanning,
                onOpenItem = { imageId ->
                    scope.launch {
                        val start = viewModel.openSwipe(imageId) ?: return@launch
                        onOpenSwipe(start)
                    }
                },
            )
        }
    }
}

@Composable
private fun LibraryBrandHeader(
    rootPath: String?,
    hasRoot: Boolean,
    showSearch: Boolean,
    onToggleSearch: () -> Unit,
    onScan: () -> Unit,
    onPickRoot: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(GuotuColors.Panel),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = GuotuDimens.Page, end = GuotuDimens.Grid, top = GuotuDimens.Page, bottom = GuotuDimens.ListGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineMedium,
                    color = GuotuColors.Slate,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(GuotuDimens.Grid))
                Text(
                    text = rootPath ?: stringResource(R.string.root_path_unset),
                    style = MaterialTheme.typography.bodySmall,
                    color = GuotuColors.InkMute,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(onClick = onToggleSearch) {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = stringResource(R.string.action_search),
                    tint = if (showSearch) GuotuColors.Signal else GuotuColors.Ink,
                )
            }
            if (hasRoot) {
                IconButton(onClick = onScan) {
                    Icon(
                        imageVector = Icons.Outlined.Refresh,
                        contentDescription = stringResource(R.string.action_scan),
                        tint = GuotuColors.Ink,
                    )
                }
            }
            IconButton(onClick = onPickRoot) {
                Icon(
                    imageVector = Icons.Outlined.FolderOpen,
                    contentDescription = stringResource(R.string.action_select_root),
                    tint = GuotuColors.Ink,
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(GuotuDimens.Hairline)
                .background(GuotuColors.Rule),
        )
    }
}

@Composable
private fun SelectionBar(
    count: Int,
    canTag: Boolean,
    onSelectAll: () -> Unit,
    onCut: () -> Unit,
    onKeep: () -> Unit,
    onDelete: () -> Unit,
    onTag: () -> Unit,
    onClear: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(GuotuColors.Panel),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(GuotuDimens.Hairline)
                .background(GuotuColors.Rule),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = GuotuDimens.ListGap, vertical = GuotuDimens.Grid),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.selected_count, count),
                style = MaterialTheme.typography.labelLarge,
                color = GuotuColors.Ink,
                modifier = Modifier.padding(start = GuotuDimens.Grid),
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = stringResource(R.string.action_select_all),
                modifier = Modifier
                    .clickable(onClick = onSelectAll)
                    .padding(horizontal = GuotuDimens.ListGap, vertical = 10.dp),
                style = MaterialTheme.typography.labelLarge,
                color = GuotuColors.Ink,
            )
            Text(
                text = stringResource(R.string.action_cut),
                modifier = Modifier
                    .clickable(onClick = onCut)
                    .padding(horizontal = GuotuDimens.ListGap, vertical = 10.dp),
                style = MaterialTheme.typography.labelLarge,
                color = GuotuColors.Signal,
                fontWeight = FontWeight.SemiBold,
            )
            if (canTag) {
                Text(
                    text = stringResource(R.string.action_tag),
                    modifier = Modifier
                        .clickable(onClick = onTag)
                        .padding(horizontal = GuotuDimens.ListGap, vertical = 10.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = GuotuColors.Ink,
                )
            }
            Text(
                text = stringResource(R.string.mark_keep),
                modifier = Modifier
                    .clickable(onClick = onKeep)
                    .padding(horizontal = GuotuDimens.ListGap, vertical = 10.dp),
                style = MaterialTheme.typography.labelLarge,
                color = GuotuColors.Ink,
            )
            Text(
                text = stringResource(R.string.mark_to_delete),
                modifier = Modifier
                    .clickable(onClick = onDelete)
                    .padding(horizontal = GuotuDimens.ListGap, vertical = 10.dp),
                style = MaterialTheme.typography.labelLarge,
                color = GuotuColors.Ink,
            )
            IconButton(onClick = onClear) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = stringResource(R.string.action_cancel),
                    tint = GuotuColors.InkMute,
                )
            }
        }
    }
}

@Composable
private fun LibraryContent(
    modifier: Modifier,
    uiState: LibraryUiState,
    images: androidx.paging.compose.LazyPagingItems<ImageEntity>,
    showSearch: Boolean,
    onQuickFilter: (QuickFilter) -> Unit,
    onClearExternalFilter: () -> Unit,
    onPathKeyword: (String) -> Unit,
    onQueryText: (String) -> Unit,
    onScan: () -> Unit,
    onToggleSelect: (Long) -> Unit,
    onEnterSelection: (Long) -> Unit,
    onStartBrowse: () -> Unit,
    onResumeLast: () -> Unit,
    onSetSort: (SortKey) -> Unit,
    onCutKept: () -> Unit,
    onCutFiltered: () -> Unit,
    onPurgeToDelete: () -> Unit,
    onExportKeep: () -> Unit,
    browsingEnabled: Boolean,
    onOpenItem: (Long) -> Unit,
) {
    Column(modifier = modifier) {
        if (uiState.stats.total > 0) {
            ProgressStatsBar(stats = uiState.stats, filtered = uiState.imageCount)
        }
        QuickFilterRow(current = uiState.quickFilter, onSelect = onQuickFilter)
        SortRow(sort = uiState.sort, onSelect = onSetSort)

        uiState.externalFilterLabel?.let { label ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = GuotuDimens.Page, vertical = GuotuDimens.Grid),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.library_filter_chip, label),
                    modifier = Modifier
                        .clip(GuotuButtonShape)
                        .background(GuotuColors.SignalSoft)
                        .padding(horizontal = GuotuDimens.ListGap, vertical = GuotuDimens.Grid),
                    style = MaterialTheme.typography.labelLarge,
                    color = GuotuColors.Signal,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = stringResource(R.string.library_filter_clear),
                    modifier = Modifier
                        .clickable(onClick = onClearExternalFilter)
                        .padding(GuotuDimens.Grid),
                    style = MaterialTheme.typography.labelLarge,
                    color = GuotuColors.InkMute,
                )
            }
        }

        if (uiState.imageCount > 0 || images.itemCount > 0) {
            BrowseActionRow(
                hasLast = !uiState.lastRelativePath.isNullOrBlank(),
                toDeleteCount = uiState.stats.toDelete,
                keepCount = uiState.stats.keep,
                browsingEnabled = browsingEnabled,
                onStartBrowse = onStartBrowse,
                onResumeLast = onResumeLast,
                onCutKept = onCutKept,
                onCutFiltered = onCutFiltered,
                onPurgeToDelete = onPurgeToDelete,
                onExportKeep = onExportKeep,
            )
        }

        if (showSearch) {
            SearchField(
                value = uiState.pathKeyword,
                onValueChange = onPathKeyword,
                placeholder = stringResource(R.string.filter_path_keyword),
            )
            SearchField(
                value = uiState.queryText,
                onValueChange = onQueryText,
                placeholder = stringResource(R.string.filter_search),
            )
        }

        if (uiState.isScanning) {
            ScanBanner(found = uiState.scanFound)
        }

        uiState.scanError?.let { err ->
            Text(
                text = stringResource(R.string.scan_failed, err),
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
                style = MaterialTheme.typography.bodySmall,
                color = GuotuColors.Danger,
            )
        }

        Text(
            text = stringResource(R.string.image_count, uiState.imageCount),
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelLarge,
            color = GuotuColors.InkMute,
        )

        val showLoading = uiState.isScanning && images.itemCount == 0
        val showEmpty = !showLoading &&
            images.itemCount == 0 &&
            images.loadState.refresh is LoadState.NotLoading

        when {
            showLoading -> {
                LoadingPane(modifier = Modifier.weight(1f))
            }
            showEmpty -> {
                EmptyImagesState(
                    modifier = Modifier.weight(1f),
                    onScan = onScan,
                    hint = if (uiState.hasRoot) {
                        stringResource(R.string.scan_zero_hint)
                    } else {
                        stringResource(R.string.library_empty_subtitle)
                    },
                )
            }
            else -> {
                LazyVerticalGrid(
                    columns = when (uiState.gridColumns) {
                        2, 3, 4 -> GridCells.Fixed(uiState.gridColumns)
                        else -> GridCells.Adaptive(148.dp)
                    },
                    contentPadding = PaddingValues(
                        horizontal = GuotuDimens.Page,
                        vertical = GuotuDimens.Grid,
                    ),
                    verticalArrangement = Arrangement.spacedBy(GuotuDimens.ListGap),
                    horizontalArrangement = Arrangement.spacedBy(GuotuDimens.ListGap),
                    modifier = Modifier.weight(1f),
                ) {
                    items(
                        count = images.itemCount,
                        key = { index -> images.peek(index)?.id ?: index },
                    ) { index ->
                        val item = images[index]
                        if (item != null) {
                            ImageGridItem(
                                image = item,
                                selected = item.id in uiState.selectedIds,
                                selectionMode = uiState.selectionMode,
                                onClick = {
                                    if (uiState.selectionMode) {
                                        onToggleSelect(item.id)
                                    } else {
                                        onOpenItem(item.id)
                                    }
                                },
                                onLongClick = { onEnterSelection(item.id) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SortRow(
    sort: com.guotu.app.domain.model.SortSpec,
    onSelect: (SortKey) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = GuotuDimens.Page, vertical = GuotuDimens.Grid),
        horizontalArrangement = Arrangement.spacedBy(GuotuDimens.Grid),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.sort_label),
            style = MaterialTheme.typography.labelLarge,
            color = GuotuColors.InkMute,
        )
        val options = listOf(
            SortKey.DATE_MODIFIED to R.string.sort_date,
            SortKey.NAME to R.string.sort_name,
            SortKey.SIZE to R.string.sort_size,
            SortKey.PATH to R.string.sort_path,
        )
        options.forEach { (key, label) ->
            val selected = sort.key == key
            val arrow = when {
                !selected -> ""
                sort.ascending -> " ↑"
                else -> " ↓"
            }
            Text(
                text = stringResource(label) + arrow,
                modifier = Modifier
                    .clip(GuotuButtonShape)
                    .background(if (selected) GuotuColors.SignalSoft else Color.Transparent)
                    .clickable { onSelect(key) }
                    .padding(horizontal = GuotuDimens.ListGap, vertical = GuotuDimens.Grid),
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) GuotuColors.Signal else GuotuColors.InkMute,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun ProgressStatsBar(
    stats: com.guotu.app.data.repo.LibraryStats,
    filtered: Int,
) {
    val done = stats.progressDone
    val total = stats.total.coerceAtLeast(1)
    val ratio = (done.toFloat() / total.toFloat()).coerceIn(0f, 1f)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(GuotuColors.Panel)
            .padding(horizontal = GuotuDimens.Page, vertical = GuotuDimens.Grid),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(R.string.stats_progress, done, stats.total),
                style = MaterialTheme.typography.labelLarge,
                color = GuotuColors.Ink,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResource(R.string.stats_filtered, filtered),
                style = MaterialTheme.typography.labelMedium,
                color = GuotuColors.InkMute,
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(GuotuButtonShape)
                .background(GuotuColors.Rule),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(ratio)
                    .height(6.dp)
                    .clip(GuotuButtonShape)
                    .background(GuotuColors.Signal),
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(
                R.string.stats_breakdown,
                stats.keep,
                stats.pending,
                stats.toDelete,
                stats.moved,
            ),
            style = MaterialTheme.typography.bodySmall,
            color = GuotuColors.InkMute,
        )
    }
}

@Composable
private fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = GuotuDimens.Page, vertical = GuotuDimens.Grid)
            .border(GuotuDimens.Hairline, GuotuColors.Rule, GuotuButtonShape)
            .background(GuotuColors.Panel, GuotuButtonShape)
            .padding(horizontal = 14.dp, vertical = GuotuDimens.ListGap),
    ) {
        if (value.isEmpty()) {
            Text(
                text = placeholder,
                style = MaterialTheme.typography.bodyMedium,
                color = GuotuColors.InkMute,
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = GuotuColors.Ink),
            cursorBrush = SolidColor(GuotuColors.Signal),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun LoadingPane(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            color = GuotuColors.Signal,
            strokeWidth = 2.dp,
            modifier = Modifier.size(28.dp),
        )
    }
}

@Composable
private fun QuickFilterRow(
    current: QuickFilter,
    onSelect: (QuickFilter) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(GuotuColors.Panel),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = GuotuDimens.Page, vertical = GuotuDimens.Grid),
            horizontalArrangement = Arrangement.spacedBy(GuotuDimens.Grid),
        ) {
            val filters = listOf(
                QuickFilter.ALL to R.string.filter_all,
                QuickFilter.UNSORTED to R.string.filter_unreviewed,
                QuickFilter.KEEP to R.string.mark_keep,
                QuickFilter.PENDING to R.string.mark_pending,
                QuickFilter.TO_DELETE to R.string.mark_to_delete,
                QuickFilter.MOVED to R.string.mark_moved,
            )
            filters.forEach { (filter, label) ->
                val selected = current == filter
                Text(
                    text = stringResource(label),
                    modifier = Modifier
                        .clip(GuotuButtonShape)
                        .background(if (selected) GuotuColors.SignalSoft else Color.Transparent)
                        .clickable { onSelect(filter) }
                        .padding(horizontal = GuotuDimens.ListGap, vertical = GuotuDimens.Grid),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (selected) GuotuColors.Signal else GuotuColors.InkMute,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(GuotuDimens.Hairline)
                .background(GuotuColors.Rule),
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ImageGridItem(
    image: ImageEntity,
    selected: Boolean,
    selectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val parentDir = PathAbbreviator.parentDirectory(image.relativePath, image.fileName)
    val sizeLabel = FileSizeFormatter.format(image.sizeBytes)
    val extLabel = image.ext.uppercase().ifEmpty { "VIDEO" }
    var hasThumb by remember(image.id) { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .guotuCard(elevation = if (selected) 4.dp else GuotuDimens.CardElevation)
            .then(
                if (selected) {
                    Modifier.border(2.dp, GuotuColors.Signal, GuotuCardShape)
                } else {
                    Modifier
                },
            )
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF1F2937),
                            Color(0xFF111827),
                            Color(0xFF0B1220),
                        ),
                    ),
                ),
        ) {
            if (!hasThumb) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val step = 18.dp.toPx()
                    val line = Color.White.copy(alpha = 0.045f)
                    var x = -size.height
                    while (x < size.width + size.height) {
                        drawLine(
                            color = line,
                            start = Offset(x, 0f),
                            end = Offset(x + size.height, size.height),
                            strokeWidth = 1.dp.toPx(),
                        )
                        x += step
                    }
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .clip(GuotuButtonShape)
                        .background(Color.White.copy(alpha = 0.08f))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    Text(
                        text = extLabel,
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White.copy(alpha = 0.88f),
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            ImageThumbnail(
                imageId = image.id,
                docUri = image.docUri,
                contentDescription = image.fileName,
                onLoaded = { hasThumb = it },
            )
            // 底张暗角，让大小角标更清晰
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(40.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.45f)),
                        ),
                    ),
            )
            Text(
                text = sizeLabel,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(GuotuDimens.Grid)
                    .clip(GuotuButtonShape)
                    .background(Color.Black.copy(alpha = 0.55f))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelMedium,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
            )
            StatusBadge(
                status = image.markStatus,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(GuotuDimens.Grid),
            )
            if (selectionMode) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(GuotuDimens.Grid)
                        .size(18.dp)
                        .clip(GuotuButtonShape)
                        .border(
                            1.5.dp,
                            if (selected) GuotuColors.Signal else Color.White.copy(alpha = 0.7f),
                            GuotuButtonShape,
                        )
                        .background(if (selected) GuotuColors.Signal else Color.Transparent),
                )
            }
        }
        Column(modifier = Modifier.padding(GuotuDimens.ListGap)) {
            Text(
                text = image.fileName,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleMedium,
                color = GuotuColors.Ink,
                fontWeight = FontWeight.SemiBold,
            )
            if (parentDir != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = parentDir,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall,
                    color = GuotuColors.InkMute,
                )
            }
        }
    }
}

@Composable
private fun StatusBadge(status: String, modifier: Modifier = Modifier) {
    if (status == MarkStatus.KEEP.name) {
        Box(
            modifier = modifier
                .size(32.dp)
                .clip(GuotuButtonShape)
                .background(Color.Black.copy(alpha = 0.35f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Favorite,
                contentDescription = stringResource(R.string.mark_keep),
                tint = Color(0xFFFF2D55),
                modifier = Modifier.size(18.dp),
            )
        }
        return
    }
    val (labelRes, bg, fg) = when (status) {
        MarkStatus.PENDING.name -> Triple(R.string.mark_pending, GuotuColors.SignalSoft, GuotuColors.Slate)
        MarkStatus.TO_DELETE.name, MarkStatus.UNSORTED.name ->
            Triple(R.string.mark_to_delete, GuotuColors.Danger, Color.White)
        MarkStatus.MOVED.name -> Triple(R.string.mark_moved, GuotuColors.Slate, Color.White)
        else -> return
    }
    Text(
        text = stringResource(labelRes),
        modifier = modifier
            .clip(GuotuButtonShape)
            .background(bg)
            .padding(horizontal = GuotuDimens.Grid, vertical = 4.dp),
        style = MaterialTheme.typography.labelSmall,
        color = fg,
    )
}

@Composable
private fun EmptyRootState(
    modifier: Modifier = Modifier,
    onSelect: () -> Unit,
    onGrantAllFiles: () -> Unit,
    showGrantAllFiles: Boolean,
) {
    Column(
        modifier = modifier.padding(GuotuDimens.Module),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.Start,
    ) {
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.displayLarge,
            color = GuotuColors.Slate,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(GuotuDimens.Grid))
        Text(
            text = stringResource(R.string.app_tagline),
            style = MaterialTheme.typography.bodyLarge,
            color = GuotuColors.InkMute,
        )
        Spacer(modifier = Modifier.height(GuotuDimens.Module))
        Text(
            text = stringResource(R.string.library_empty_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = GuotuColors.Ink,
        )
        if (showGrantAllFiles) {
            Spacer(modifier = Modifier.height(GuotuDimens.ListGap))
            Text(
                text = stringResource(R.string.all_files_access_hint),
                style = MaterialTheme.typography.bodySmall,
                color = GuotuColors.InkMute,
            )
        }
        Spacer(modifier = Modifier.height(GuotuDimens.Module))
        Text(
            text = stringResource(R.string.action_select_root),
            modifier = Modifier
                .clip(GuotuButtonShape)
                .background(GuotuColors.Signal)
                .clickable(onClick = onSelect)
                .padding(horizontal = GuotuDimens.Module, vertical = 14.dp),
            style = MaterialTheme.typography.titleMedium,
            color = GuotuColors.OnSignal,
            fontWeight = FontWeight.SemiBold,
        )
        if (showGrantAllFiles) {
            Spacer(modifier = Modifier.height(GuotuDimens.ListGap))
            Text(
                text = stringResource(R.string.action_grant_all_files),
                modifier = Modifier
                    .clip(GuotuButtonShape)
                    .background(GuotuColors.SignalSoft)
                    .clickable(onClick = onGrantAllFiles)
                    .padding(horizontal = GuotuDimens.Module, vertical = 12.dp),
                style = MaterialTheme.typography.labelLarge,
                color = GuotuColors.Signal,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun EmptyImagesState(
    modifier: Modifier = Modifier,
    onScan: () -> Unit,
    hint: String,
) {
    Column(
        modifier = modifier.padding(horizontal = GuotuDimens.Module, vertical = GuotuDimens.Module * 2),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.library_empty_title),
            style = MaterialTheme.typography.headlineMedium,
            color = GuotuColors.Slate,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(GuotuDimens.Grid))
        Text(
            text = hint,
            style = MaterialTheme.typography.bodyLarge,
            color = GuotuColors.InkMute,
        )
        Spacer(modifier = Modifier.height(GuotuDimens.Module))
        Text(
            text = stringResource(R.string.action_scan),
            modifier = Modifier
                .clip(GuotuButtonShape)
                .background(GuotuColors.SignalSoft)
                .clickable(onClick = onScan)
                .padding(horizontal = GuotuDimens.Page, vertical = GuotuDimens.ListGap),
            style = MaterialTheme.typography.labelLarge,
            color = GuotuColors.Signal,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun TagPickerDialog(
    tags: List<com.guotu.app.data.db.TagEntity>,
    onDismiss: () -> Unit,
    onPick: (Long) -> Unit,
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .guotuCard()
                .padding(GuotuDimens.Page),
        ) {
            Text(
                text = stringResource(R.string.action_tag),
                style = MaterialTheme.typography.titleMedium,
                color = GuotuColors.Ink,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(GuotuDimens.ListGap))
            tags.forEach { tag ->
                val swatch = runCatching {
                    Color(android.graphics.Color.parseColor(tag.color))
                }.getOrDefault(GuotuColors.Signal)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPick(tag.id) }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(GuotuButtonShape)
                            .background(swatch),
                    )
                    Spacer(modifier = Modifier.size(GuotuDimens.ListGap))
                    Text(
                        text = tag.name,
                        style = MaterialTheme.typography.bodyLarge,
                        color = GuotuColors.Ink,
                    )
                }
            }
            Spacer(modifier = Modifier.height(GuotuDimens.Grid))
            Text(
                text = stringResource(R.string.action_cancel),
                modifier = Modifier
                    .align(Alignment.End)
                    .clickable(onClick = onDismiss)
                    .padding(GuotuDimens.Grid),
                style = MaterialTheme.typography.labelLarge,
                color = GuotuColors.InkMute,
            )
        }
    }
}
