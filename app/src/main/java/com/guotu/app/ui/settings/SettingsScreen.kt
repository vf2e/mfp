package com.guotu.app.ui.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.guotu.app.BuildConfig
import com.guotu.app.R
import com.guotu.app.core.util.StorageAccessHelper
import com.guotu.app.data.prefs.DoubleTapAction
import com.guotu.app.ui.components.GuotuHairline
import com.guotu.app.ui.components.GuotuPageHeader
import com.guotu.app.ui.components.GuotuSegmentedRail
import com.guotu.app.ui.components.GuotuStepPicker
import com.guotu.app.ui.theme.GuotuColors
import com.guotu.app.ui.theme.GuotuDimens
import com.guotu.app.ui.library.FileFolderPickerDialog
import com.guotu.app.ui.theme.guotuCard
import com.guotu.app.ui.util.OpenDocumentTreeContract
import com.guotu.app.ui.util.launchDocumentTreePreferAndroid
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.runtime.LaunchedEffect

@Composable
fun SettingsScreen(
    onCheckUpdate: () -> Unit = {},
    onOpenUpdate: () -> Unit = {},
    updateChecking: Boolean = false,
    updateAvailableName: String? = null,
    updateDownloadPercent: Int? = null,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val ui by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasAllFiles by remember { mutableStateOf(StorageAccessHelper.hasAllFilesAccess()) }
    var showFileFolderPicker by remember { mutableStateOf(false) }
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            hasAllFiles = StorageAccessHelper.hasAllFilesAccess()
        }
    }
    val openTree = rememberLauncherForActivityResult(
        contract = OpenDocumentTreeContract(),
    ) { uri: Uri? ->
        if (uri != null) viewModel.changeRoot(uri)
    }
    if (showFileFolderPicker) {
        FileFolderPickerDialog(
            onDismiss = { showFileFolderPicker = false },
            onConfirm = { path ->
                showFileFolderPicker = false
                viewModel.changeRootFilePath(path)
            },
        )
    }
    val scannedLabel = if (ui.lastScannedAt > 0L) {
        SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(ui.lastScannedAt))
    } else {
        stringResource(R.string.settings_never_scanned)
    }

    // LazyColumn — 可滚动；勿用 fillMaxSize+Column+verticalScroll（易被底栏裁切且滚不动）
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(GuotuColors.Canvas),
        contentPadding = PaddingValues(bottom = GuotuDimens.Module),
    ) {
        item {
            GuotuPageHeader(
                title = stringResource(R.string.nav_settings),
                subtitle = stringResource(R.string.settings_subtitle),
            )
        }

        item { SectionLabel(stringResource(R.string.settings_section_library)) }
        item {
            Panel {
                LinkRow(
                    title = stringResource(R.string.settings_root_folder),
                    subtitle = ui.rootPath ?: stringResource(R.string.root_path_unset),
                    onClick = {
                        if (StorageAccessHelper.hasAllFilesAccess()) {
                            showFileFolderPicker = true
                        } else {
                            openTree.launchDocumentTreePreferAndroid()
                        }
                    },
                )
                GuotuHairline()
                LinkRow(
                    title = stringResource(R.string.settings_all_files_access),
                    subtitle = if (hasAllFiles) {
                        stringResource(R.string.settings_all_files_on)
                    } else {
                        stringResource(R.string.settings_all_files_off)
                    },
                    onClick = {
                        runCatching {
                            context.startActivity(StorageAccessHelper.manageAllFilesIntent(context))
                        }
                        hasAllFiles = StorageAccessHelper.hasAllFilesAccess()
                    },
                )
                GuotuHairline()
                LinkRow(
                    title = stringResource(R.string.action_scan),
                    subtitle = stringResource(R.string.settings_scan_detail, ui.imageCount, scannedLabel),
                    onClick = viewModel::scanNow,
                    chevron = false,
                    actionHint = stringResource(R.string.settings_tap_to_run),
                )
                GuotuHairline()
                ValueRow(
                    title = stringResource(R.string.settings_library_count),
                    value = stringResource(R.string.settings_library_count_value, ui.imageCount),
                )
                GuotuHairline()
                SwitchRow(
                    title = stringResource(R.string.settings_show_tags_tab),
                    subtitle = stringResource(R.string.settings_show_tags_tab_hint),
                    checked = ui.showTagsTab,
                    onCheckedChange = viewModel::setShowTagsTab,
                )
                GuotuHairline()
                SwitchRow(
                    title = stringResource(R.string.settings_default_unreviewed),
                    subtitle = stringResource(R.string.settings_default_unreviewed_hint),
                    checked = ui.defaultUnreviewed,
                    onCheckedChange = viewModel::setDefaultUnreviewed,
                )
            }
        }

        item { SectionLabel(stringResource(R.string.settings_section_playback)) }
        item {
            Panel {
                FieldBlock(
                    title = stringResource(R.string.settings_page_size),
                    subtitle = stringResource(R.string.settings_page_size_hint),
                ) {
                    GuotuSegmentedRail(
                        options = listOf(
                            50 to "50",
                            100 to "100",
                            200 to "200",
                            500 to "500",
                        ),
                        selected = ui.pageSize,
                        onSelect = viewModel::setPageSize,
                    )
                }
                GuotuHairline()
                FieldBlock(
                    title = stringResource(R.string.settings_grid_columns),
                    subtitle = stringResource(R.string.settings_grid_columns_hint),
                ) {
                    GuotuSegmentedRail(
                        options = listOf(
                            0 to stringResource(R.string.settings_grid_auto),
                            2 to "2",
                            3 to "3",
                            4 to "4",
                        ),
                        selected = ui.gridColumns,
                        onSelect = viewModel::setGridColumns,
                    )
                }
            }
        }

        item { SectionLabel(stringResource(R.string.settings_section_gesture)) }
        item {
            Panel {
                FieldBlock(
                    title = stringResource(R.string.settings_double_tap),
                    subtitle = stringResource(R.string.settings_double_tap_hint),
                ) {
                    GuotuSegmentedRail(
                        options = listOf(
                            DoubleTapAction.KEEP to stringResource(R.string.mark_keep),
                            DoubleTapAction.PENDING to stringResource(R.string.mark_pending),
                            DoubleTapAction.TO_DELETE to stringResource(R.string.mark_to_delete),
                            DoubleTapAction.TOGGLE_PLAY to stringResource(R.string.settings_double_tap_play),
                        ),
                        selected = ui.doubleTapAction,
                        onSelect = viewModel::setDoubleTapAction,
                    )
                }
                GuotuHairline()
                SwitchRow(
                    title = stringResource(R.string.settings_swipe_mark),
                    subtitle = stringResource(R.string.settings_swipe_mark_hint),
                    checked = ui.swipeMarkGesture,
                    onCheckedChange = viewModel::setSwipeMarkGesture,
                )
                GuotuHairline()
                SwitchRow(
                    title = stringResource(R.string.settings_swap_swipe),
                    subtitle = stringResource(R.string.settings_swap_swipe_hint),
                    checked = ui.swapSwipe,
                    onCheckedChange = viewModel::setSwapSwipe,
                )
                GuotuHairline()
                SwitchRow(
                    title = stringResource(R.string.settings_mark_auto_next),
                    subtitle = stringResource(R.string.settings_mark_auto_next_hint),
                    checked = ui.markAutoNext,
                    onCheckedChange = viewModel::setMarkAutoNext,
                )
            }
        }

        item { SectionLabel(stringResource(R.string.settings_section_about)) }
        item {
            Panel {
                ValueRow(
                    title = stringResource(R.string.app_name),
                    value = "v${BuildConfig.VERSION_NAME}",
                )
                GuotuHairline()
                LinkRow(
                    title = when {
                        updateDownloadPercent != null -> stringResource(R.string.settings_updating)
                        updateAvailableName != null -> stringResource(R.string.settings_update_available)
                        else -> stringResource(R.string.settings_check_update)
                    },
                    subtitle = when {
                        updateChecking -> stringResource(R.string.settings_checking_update)
                        updateDownloadPercent != null -> stringResource(
                            R.string.update_downloading,
                            updateDownloadPercent,
                        )
                        updateAvailableName != null -> stringResource(
                            R.string.settings_update_available_hint,
                            updateAvailableName,
                        )
                        else -> stringResource(R.string.settings_check_update_hint)
                    },
                    onClick = {
                        when {
                            updateDownloadPercent != null -> onOpenUpdate()
                            updateAvailableName != null -> onOpenUpdate()
                            updateChecking -> Unit
                            else -> onCheckUpdate()
                        }
                    },
                    chevron = false,
                    actionHint = when {
                        updateChecking -> null
                        updateDownloadPercent != null -> "$updateDownloadPercent%"
                        updateAvailableName != null -> stringResource(R.string.settings_do_update)
                        else -> stringResource(R.string.settings_do_check)
                    },
                )
                GuotuHairline()
                Text(
                    text = stringResource(R.string.privacy_local_only),
                    style = MaterialTheme.typography.bodyMedium,
                    color = GuotuColors.InkMute,
                    modifier = Modifier.padding(horizontal = GuotuDimens.Page, vertical = 16.dp),
                )
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = GuotuColors.InkMute,
        modifier = Modifier.padding(
            start = GuotuDimens.Page,
            end = GuotuDimens.Page,
            top = GuotuDimens.Module,
            bottom = GuotuDimens.Grid,
        ),
    )
}

@Composable
private fun Panel(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = GuotuDimens.Page)
            .guotuCard(),
        content = { content() },
    )
}

@Composable
private fun FieldBlock(
    title: String,
    subtitle: String,
    content: @Composable () -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = GuotuDimens.Page, vertical = 16.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = GuotuColors.Ink,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = GuotuColors.InkMute,
        )
        Spacer(modifier = Modifier.height(14.dp))
        content()
    }
}

@Composable
private fun LinkRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    chevron: Boolean = true,
    actionHint: String? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = GuotuDimens.Page, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = GuotuColors.Ink,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = GuotuColors.InkMute,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        when {
            actionHint != null -> Text(
                text = actionHint,
                style = MaterialTheme.typography.labelMedium,
                color = GuotuColors.Signal,
                fontWeight = FontWeight.SemiBold,
            )
            chevron -> Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = GuotuColors.InkMute,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun ValueRow(title: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = GuotuDimens.Page, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = GuotuColors.Ink,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = GuotuColors.InkMute,
        )
    }
}

@Composable
private fun SwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = GuotuDimens.Page, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = GuotuDimens.ListGap)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = GuotuColors.Ink,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = GuotuColors.InkMute,
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = GuotuColors.Signal,
                checkedBorderColor = GuotuColors.Signal,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = GuotuColors.Rule,
                uncheckedBorderColor = GuotuColors.Rule,
            ),
        )
    }
}
