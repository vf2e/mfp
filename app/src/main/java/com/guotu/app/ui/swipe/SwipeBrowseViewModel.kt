package com.guotu.app.ui.swipe

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.guotu.app.data.db.FolderPresetDao
import com.guotu.app.data.db.FolderPresetEntity
import com.guotu.app.data.db.ImageEntity
import com.guotu.app.data.prefs.DoubleTapAction
import com.guotu.app.data.prefs.SettingsRepository
import com.guotu.app.data.repo.ImageRepository
import com.guotu.app.data.repo.RootBrowseStateStore
import com.guotu.app.data.repo.RootFolderRepository
import com.guotu.app.domain.model.ConflictPolicy
import com.guotu.app.domain.model.MarkStatus
import com.guotu.app.domain.usecase.MarkImageUseCase
import com.guotu.app.player.playlist.PlaylistSession
import com.guotu.app.work.MoveScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.ArrayDeque
import javax.inject.Inject

data class SwipeUiState(
    val ready: Boolean = false,
    val currentIndex: Int = 0,
    val total: Int = 0,
    val pageIndex: Int = 0,
    val pageCount: Int = 0,
    val totalInLibrary: Int = 0,
    val playlistEpoch: Int = 0,
    val image: ImageEntity? = null,
    val uiVisible: Boolean = true,
    val markStatus: MarkStatus = MarkStatus.TO_DELETE,
    val error: String? = null,
    val showCutSheet: Boolean = false,
    val conflictPolicy: ConflictPolicy = ConflictPolicy.RENAME,
    val cutMessage: String? = null,
    val pendingScrollIndex: Int? = null,
    val keepBurstEpoch: Int = 0,
    val keepBurstKept: Boolean = false,
    /** 左右滑标记反馈：KEEP / TO_DELETE，短暂展示。 */
    val swipeFlash: MarkStatus? = null,
    val swipeFlashEpoch: Int = 0,
    val canUndo: Boolean = false,
    val undoHint: String? = null,
    val swipeMarkEnabled: Boolean = true,
    /** true = 左滑收藏 / 右滑待删（对调）。 */
    val swapSwipeMark: Boolean = false,
    val endOfListHint: String? = null,
)

private data class UndoEntry(
    val imageId: Long,
    val previous: MarkStatus,
    val applied: MarkStatus,
)

@HiltViewModel
class SwipeBrowseViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val playlistSession: PlaylistSession,
    private val imageRepository: ImageRepository,
    private val markImageUseCase: MarkImageUseCase,
    private val moveScheduler: MoveScheduler,
    private val folderPresetDao: FolderPresetDao,
    private val settingsRepository: SettingsRepository,
    private val rootFolderRepository: RootFolderRepository,
    private val browseStateStore: RootBrowseStateStore,
) : ViewModel() {

    private val startIndexArg: Int = savedStateHandle["startIndex"] ?: 0

    private val _uiState = MutableStateFlow(SwipeUiState())
    val uiState: StateFlow<SwipeUiState> = _uiState.asStateFlow()

    val folderPresets: StateFlow<List<FolderPresetEntity>> = folderPresetDao.observeRecent()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    private val _docUris = MutableStateFlow<Map<Int, String>>(emptyMap())
    val docUris: StateFlow<Map<Int, String>> = _docUris.asStateFlow()

    private var observeJob: Job? = null
    private val undoStack = ArrayDeque<UndoEntry>(16)

    init {
        viewModelScope.launch {
            settingsRepository.swipeMarkGesture.collect { enabled ->
                _uiState.update { it.copy(swipeMarkEnabled = enabled) }
            }
        }
        viewModelScope.launch {
            settingsRepository.swapSwipe.collect { swap ->
                _uiState.update { it.copy(swapSwipeMark = swap) }
            }
        }
        viewModelScope.launch {
            val snap = playlistSession.current()
            if (snap == null || snap.ids.isEmpty()) {
                _uiState.update { it.copy(error = "浏览列表为空") }
                return@launch
            }
            val index = startIndexArg.coerceIn(0, snap.ids.lastIndex)
            // 先同步拉当前张 URI，避免首屏空白要靠下滑才触发重组
            val image = withContext(Dispatchers.IO) {
                imageRepository.getById(snap.ids[index])
            }
            image?.docUri?.takeIf { it.isNotBlank() }?.let { uri ->
                _docUris.value = mapOf(index to uri)
            }
            _uiState.update {
                it.copy(
                    ready = true,
                    currentIndex = index,
                    total = snap.ids.size,
                    pageIndex = snap.pageIndex,
                    pageCount = snap.pageCount,
                    totalInLibrary = snap.totalCount,
                    playlistEpoch = 1,
                    image = image,
                    markStatus = image?.let { v -> markImageUseCase.statusOf(v) } ?: MarkStatus.TO_DELETE,
                )
            }
            onPageSettled(index)
        }
    }

    fun idAt(page: Int): Long? = playlistSession.idAt(page)

    fun onPageSettled(index: Int) {
        val snap = playlistSession.current() ?: return
        if (index !in snap.ids.indices) return

        observeJob?.cancel()
        prefetchPages(index)

        viewModelScope.launch {
            val imageId = snap.ids[index]
            val image = withContext(Dispatchers.IO) { imageRepository.getById(imageId) }
            image?.docUri?.takeIf { it.isNotBlank() }?.let { uri ->
                _docUris.update { it + (index to uri) }
            }
            _uiState.update {
                it.copy(
                    currentIndex = index,
                    total = snap.ids.size,
                    pageIndex = snap.pageIndex,
                    pageCount = snap.pageCount,
                    totalInLibrary = snap.totalCount,
                    image = image,
                    markStatus = image?.let { v -> markImageUseCase.statusOf(v) } ?: MarkStatus.TO_DELETE,
                    error = null,
                )
            }
            image?.let { persistBrowseCursor(it) }

            observeJob = viewModelScope.launch {
                imageRepository.observeById(imageId).collect { entity ->
                    if (entity != null && _uiState.value.currentIndex == index) {
                        entity.docUri.takeIf { it.isNotBlank() }?.let { uri ->
                            _docUris.update { it + (index to uri) }
                        }
                        _uiState.update {
                            it.copy(
                                image = entity,
                                markStatus = markImageUseCase.statusOf(entity),
                            )
                        }
                    }
                }
            }
        }
    }

    private fun prefetchPages(center: Int) {
        val snap = playlistSession.current() ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val extras = mutableMapOf<Int, String>()
            for (page in (center - 1)..(center + 1)) {
                if (page !in snap.ids.indices) continue
                if (_docUris.value.containsKey(page)) continue
                val id = snap.ids[page]
                imageRepository.getById(id)?.docUri?.takeIf { it.isNotBlank() }?.let { uri ->
                    extras[page] = uri
                }
            }
            if (extras.isNotEmpty()) {
                _docUris.update { it + extras }
            }
        }
    }

    private fun clearDocUris() {
        _docUris.value = emptyMap()
    }

    private fun persistBrowseCursor(image: ImageEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            val root = rootFolderRepository.getCurrent() ?: return@launch
            browseStateStore.updateLast(Uri.parse(root.treeUri), image.relativePath)
        }
    }

    fun toggleUi() {
        _uiState.update { it.copy(uiVisible = !it.uiVisible) }
    }

    fun onDoubleTap() {
        viewModelScope.launch {
            when (val action = settingsRepository.doubleTapAction.first()) {
                DoubleTapAction.KEEP, DoubleTapAction.TOGGLE_PLAY -> toggleKeep()
                DoubleTapAction.PENDING -> togglePending()
                DoubleTapAction.TO_DELETE -> applyMark(MarkStatus.TO_DELETE, flash = true)
            }
        }
    }

    fun toggleKeep() {
        val current = _uiState.value.markStatus
        val next = if (current == MarkStatus.KEEP) MarkStatus.TO_DELETE else MarkStatus.KEEP
        _uiState.update {
            it.copy(
                keepBurstEpoch = it.keepBurstEpoch + 1,
                keepBurstKept = next == MarkStatus.KEEP,
                markStatus = next,
                uiVisible = true,
            )
        }
        applyMark(next, flash = next == MarkStatus.KEEP)
    }

    fun togglePending() {
        val current = _uiState.value.markStatus
        val next = if (current == MarkStatus.PENDING) MarkStatus.TO_DELETE else MarkStatus.PENDING
        applyMark(next, flash = false)
    }

    /** 上滑/右滑标记入口（默认收藏；开启对调后为待删）。 */
    fun onHorizontalSwipeRight() {
        if (!_uiState.value.swipeMarkEnabled) return
        if (_uiState.value.swapSwipeMark) markReject() else markKeep()
    }

    /** 下滑/左滑标记入口（默认待删；开启对调后为收藏）。 */
    fun onHorizontalSwipeLeft() {
        if (!_uiState.value.swipeMarkEnabled) return
        if (_uiState.value.swapSwipeMark) markKeep() else markReject()
    }

    private fun markKeep() {
        _uiState.update {
            it.copy(
                keepBurstEpoch = it.keepBurstEpoch + 1,
                keepBurstKept = true,
                swipeFlash = MarkStatus.KEEP,
                swipeFlashEpoch = it.swipeFlashEpoch + 1,
                uiVisible = true,
            )
        }
        applyMark(MarkStatus.KEEP, flash = false)
    }

    private fun markReject() {
        _uiState.update {
            it.copy(
                swipeFlash = MarkStatus.TO_DELETE,
                swipeFlashEpoch = it.swipeFlashEpoch + 1,
                uiVisible = true,
            )
        }
        applyMark(MarkStatus.TO_DELETE, flash = false)
    }

    fun consumeSwipeFlash() {
        _uiState.update { it.copy(swipeFlash = null) }
    }

    fun clearEndOfListHint() {
        _uiState.update { it.copy(endOfListHint = null) }
    }

    fun undoLastMark() {
        val entry = synchronized(undoStack) {
            if (undoStack.isEmpty()) null else undoStack.removeLast()
        } ?: return
        viewModelScope.launch {
            markImageUseCase.setStatus(listOf(entry.imageId), entry.previous)
            markImageUseCase.setStar(
                listOf(entry.imageId),
                if (entry.previous == MarkStatus.KEEP) 1 else 0,
            )
            val root = rootFolderRepository.getCurrent()
            val image = imageRepository.getById(entry.imageId)
            if (root != null && image != null) {
                withContext(Dispatchers.IO) {
                    browseStateStore.upsertMark(
                        Uri.parse(root.treeUri),
                        image.relativePath,
                        entry.previous,
                    )
                }
            }
            val current = _uiState.value.image
            _uiState.update {
                it.copy(
                    canUndo = undoStack.isNotEmpty(),
                    undoHint = "已撤销「${statusLabel(entry.applied)}」",
                    markStatus = if (current?.id == entry.imageId) entry.previous else it.markStatus,
                    image = if (current?.id == entry.imageId) {
                        current.copy(markStatus = entry.previous.name)
                    } else {
                        it.image
                    },
                )
            }
        }
    }

    fun clearUndoHint() {
        _uiState.update { it.copy(undoHint = null) }
    }

    private fun applyMark(status: MarkStatus, flash: Boolean) {
        val image = _uiState.value.image ?: return
        val previous = _uiState.value.markStatus
        viewModelScope.launch {
            markImageUseCase.setStatus(listOf(image.id), status)
            markImageUseCase.setStar(
                listOf(image.id),
                if (status == MarkStatus.KEEP) 1 else 0,
            )
            val root = rootFolderRepository.getCurrent()
            if (root != null) {
                withContext(Dispatchers.IO) {
                    browseStateStore.upsertMark(
                        Uri.parse(root.treeUri),
                        image.relativePath,
                        status,
                    )
                }
            }
            if (previous != status) {
                synchronized(undoStack) {
                    undoStack.addLast(UndoEntry(image.id, previous, status))
                    while (undoStack.size > 30) undoStack.removeFirst()
                }
            }
            val updated = imageRepository.getById(image.id)
            var pendingScroll: Int? = null
            var jumpNextPage = false
            // 仅当设置开启「标记后自动下一张」时才跳转
            val autoNext = settingsRepository.markAutoNext.first()
            var reachedEnd = false
            if (autoNext) {
                val idx = _uiState.value.currentIndex
                when {
                    idx + 1 < _uiState.value.total -> pendingScroll = idx + 1
                    playlistSession.current()?.hasNextPage == true -> jumpNextPage = true
                    else -> reachedEnd = true
                }
            }
            _uiState.update {
                it.copy(
                    image = updated,
                    markStatus = status,
                    pendingScrollIndex = pendingScroll ?: it.pendingScrollIndex,
                    canUndo = undoStack.isNotEmpty(),
                    swipeFlash = if (flash) status else it.swipeFlash,
                    swipeFlashEpoch = if (flash) it.swipeFlashEpoch + 1 else it.swipeFlashEpoch,
                    endOfListHint = if (reachedEnd) "本批已过完，可返回图库继续整理" else it.endOfListHint,
                )
            }
            if (jumpNextPage) goNext()
        }
    }

    fun setStatus(status: MarkStatus) {
        applyMark(status, flash = false)
    }

    fun goPrevious() {
        val idx = _uiState.value.currentIndex
        if (idx > 0) {
            _uiState.update { it.copy(pendingScrollIndex = idx - 1, uiVisible = true) }
            return
        }
        val snap = playlistSession.current() ?: return
        if (!snap.hasPrevPage) return
        viewModelScope.launch {
            val result = playlistSession.openPage(snap.pageIndex - 1) ?: return@launch
            clearDocUris()
            val last = (result.snapshot.ids.size - 1).coerceAtLeast(0)
            _uiState.update {
                it.copy(
                    ready = true,
                    currentIndex = last,
                    total = result.snapshot.ids.size,
                    pageIndex = result.snapshot.pageIndex,
                    pageCount = result.snapshot.pageCount,
                    totalInLibrary = result.snapshot.totalCount,
                    playlistEpoch = it.playlistEpoch + 1,
                    pendingScrollIndex = last,
                    uiVisible = true,
                )
            }
            onPageSettled(last)
        }
    }

    fun goNext() {
        val idx = _uiState.value.currentIndex
        if (idx + 1 < _uiState.value.total) {
            _uiState.update { it.copy(pendingScrollIndex = idx + 1, uiVisible = true) }
            return
        }
        val snap = playlistSession.current() ?: return
        if (!snap.hasNextPage) return
        viewModelScope.launch {
            val result = playlistSession.openPage(snap.pageIndex + 1) ?: return@launch
            clearDocUris()
            _uiState.update {
                it.copy(
                    ready = true,
                    currentIndex = 0,
                    total = result.snapshot.ids.size,
                    pageIndex = result.snapshot.pageIndex,
                    pageCount = result.snapshot.pageCount,
                    totalInLibrary = result.snapshot.totalCount,
                    playlistEpoch = it.playlistEpoch + 1,
                    pendingScrollIndex = 0,
                    uiVisible = true,
                )
            }
            onPageSettled(0)
        }
    }

    fun consumePendingScroll() {
        _uiState.update { it.copy(pendingScrollIndex = null) }
    }

    fun openCutSheet() {
        if (_uiState.value.image == null) return
        _uiState.update { it.copy(showCutSheet = true) }
    }

    fun dismissCutSheet() {
        _uiState.update { it.copy(showCutSheet = false) }
    }

    fun setConflictPolicy(policy: ConflictPolicy) {
        _uiState.update { it.copy(conflictPolicy = policy) }
    }

    fun clearCutMessage() {
        _uiState.update { it.copy(cutMessage = null) }
    }

    fun enqueueCut(targetTreeUri: Uri) {
        val id = _uiState.value.image?.id ?: return
        val policy = _uiState.value.conflictPolicy
        viewModelScope.launch {
            val taskId = moveScheduler.enqueueMove(listOf(id), targetTreeUri, policy)
            _uiState.update {
                it.copy(
                    showCutSheet = false,
                    cutMessage = "已加入剪切任务 #$taskId",
                )
            }
        }
    }

    fun enqueueCutPreset(preset: FolderPresetEntity) {
        enqueueCut(Uri.parse(preset.uri))
    }

    override fun onCleared() {
        observeJob?.cancel()
        super.onCleared()
    }

    private fun statusLabel(status: MarkStatus): String = when (status) {
        MarkStatus.KEEP -> "收藏"
        MarkStatus.PENDING -> "待定"
        MarkStatus.TO_DELETE, MarkStatus.UNSORTED -> "待删除"
        MarkStatus.MOVED -> "已剪切"
    }
}
