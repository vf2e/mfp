package com.guotu.app.ui.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.guotu.app.data.prefs.DoubleTapAction
import com.guotu.app.data.prefs.SettingsRepository
import com.guotu.app.data.repo.RootFolderRepository
import com.guotu.app.work.ScanScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val rootPath: String? = null,
    val imageCount: Int = 0,
    val lastScannedAt: Long = 0L,
    val pageSize: Int = 100,
    val gridColumns: Int = 0,
    val markAutoNext: Boolean = true,
    val swapSwipe: Boolean = false,
    val swipeMarkGesture: Boolean = true,
    val defaultUnreviewed: Boolean = true,
    val showTagsTab: Boolean = false,
    val doubleTapAction: DoubleTapAction = DoubleTapAction.KEEP,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val rootFolderRepository: RootFolderRepository,
    private val settingsRepository: SettingsRepository,
    private val scanScheduler: ScanScheduler,
) : ViewModel() {

    private val gesturePrefs = combine(
        settingsRepository.markAutoNext,
        settingsRepository.swapSwipe,
        settingsRepository.swipeMarkGesture,
        settingsRepository.doubleTapAction,
    ) { autoNext, swap, swipeMark, doubleTap ->
        GesturePrefs(autoNext, swap, swipeMark, doubleTap)
    }

    private val libraryPrefs = combine(
        settingsRepository.pageSize,
        settingsRepository.gridColumns,
        settingsRepository.defaultUnreviewed,
        settingsRepository.showTagsTab,
    ) { pageSize, gridColumns, defaultUnreviewed, showTags ->
        LibraryPrefs(pageSize, gridColumns, defaultUnreviewed, showTags)
    }

    val uiState: StateFlow<SettingsUiState> = combine(
        rootFolderRepository.observeCurrent(),
        libraryPrefs,
        gesturePrefs,
    ) { root, library, gesture ->
        SettingsUiState(
            rootPath = root?.displayPath,
            imageCount = root?.imageCount ?: 0,
            lastScannedAt = root?.lastScannedAt ?: 0L,
            pageSize = library.pageSize,
            gridColumns = library.gridColumns,
            markAutoNext = gesture.markAutoNext,
            swapSwipe = gesture.swapSwipe,
            swipeMarkGesture = gesture.swipeMark,
            defaultUnreviewed = library.defaultUnreviewed,
            showTagsTab = library.showTags,
            doubleTapAction = gesture.doubleTap,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState(),
    )

    fun changeRoot(uri: Uri) {
        viewModelScope.launch {
            rootFolderRepository.setRootTreeUri(uri)
            scanScheduler.enqueueFullScan()
        }
    }

    fun changeRootFilePath(absolutePath: String) {
        viewModelScope.launch {
            rootFolderRepository.setRootFilePath(absolutePath)
            scanScheduler.enqueueFullScan()
        }
    }

    fun scanNow() = scanScheduler.enqueueFullScan()

    fun setPageSize(size: Int) {
        viewModelScope.launch { settingsRepository.setPageSize(size) }
    }

    fun setGridColumns(columns: Int) {
        viewModelScope.launch { settingsRepository.setGridColumns(columns) }
    }

    fun setMarkAutoNext(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setMarkAutoNext(enabled) }
    }

    fun setSwapSwipe(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setSwapSwipe(enabled) }
    }

    fun setSwipeMarkGesture(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setSwipeMarkGesture(enabled) }
    }

    fun setDefaultUnreviewed(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setDefaultUnreviewed(enabled) }
    }

    fun setDoubleTapAction(action: DoubleTapAction) {
        viewModelScope.launch { settingsRepository.setDoubleTapAction(action) }
    }

    fun setShowTagsTab(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setShowTagsTab(enabled) }
    }
}

private data class GesturePrefs(
    val markAutoNext: Boolean,
    val swapSwipe: Boolean,
    val swipeMark: Boolean,
    val doubleTap: DoubleTapAction,
)

private data class LibraryPrefs(
    val pageSize: Int,
    val gridColumns: Int,
    val defaultUnreviewed: Boolean,
    val showTags: Boolean,
)
