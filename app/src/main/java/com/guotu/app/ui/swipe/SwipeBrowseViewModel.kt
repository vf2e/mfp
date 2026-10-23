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
import javax.inject.Inject

data class SwipeUiState(
    val ready: Boolean = false,
    val currentIndex: Int = 0,
    val total: Int = 0,
    val pageIndex: Int = 0,
    val pageCount: Int = 0,
    val totalInLibrary: Int = 0,
    /** 换页时递增，强制重建 Pager。 */
    val playlistEpoch: Int = 0,
    val image: ImageEntity? = null,
    val uiVisible: Boolean = true,
    val markStatus: MarkStatus = MarkStatus.TO_DELETE,
    val error: String? = null,
    val showCutSheet: Boolean = false,
    val conflictPolicy: ConflictPolicy = ConflictPolicy.RENAME,
    val cutMessage: String? = null,
    /** One-shot: UI scrolls pager then clears. */
    val pendingScrollIndex: Int? = null,
    /** 收藏/取消特效：epoch 递增触发一次动画；kept=是否变为收藏。 */
    val keepBurstEpoch: Int = 0,
    val keepBurstKept: Boolean = false,
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

    private val docUriByPage = mutableMapOf<Int, String>()
    private var observeJob: Job? = null

    init {
        val snap = playlistSession.current()
        if (snap == null || snap.ids.isEmpty()) {
            _uiState.update { it.copy(error = "浏览列表为空") }
        } else {
            val index = startIndexArg.coerceIn(0, snap.ids.lastIndex)
            _uiState.update {
                it.copy(
                    ready = true,
                    currentIndex = index,
                    total = snap.ids.size,
                    pageIndex = snap.pageIndex,
                    pageCount = snap.pageCount,
                    totalInLibrary = snap.totalCount,
                    playlistEpoch = 1,
                )
            }
            onPageSettled(index)
        }
    }

    fun idAt(page: Int): Long? = playlistSession.idAt(page)

    fun docUriAt(page: Int): String? = docUriByPage[page]

    fun onPageSettled(index: Int) {
        val snap = playlistSession.current() ?: return
        if (index !in snap.ids.indices) return

        observeJob?.cancel()
        prefetchPages(index)

        viewModelScope.launch {
            val imageId = snap.ids[index]
            val image = imageRepository.getById(imageId)
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
        viewModelScope.launch {
            for (page in (center - 1)..(center + 1)) {
                if (page !in snap.ids.indices) continue
                if (docUriByPage.containsKey(page)) continue
                val id = snap.ids[page]
                imageRepository.getById(id)?.let { docUriByPage[page] = it.docUri }
            }
        }
    }

    private fun persistBrowseCursor(image: ImageEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            val root = rootFolderRepository.getCurrent() ?: return@launch
            val treeUri = Uri.parse(root.treeUri)
            browseStateStore.updateLast(treeUri, image.relativePath)
        }
    }

    fun toggleUi() {
        _uiState.update { it.copy(uiVisible = !it.uiVisible) }
    }

    fun showUi() {
        _uiState.update { it.copy(uiVisible = true) }
    }

    fun hideUi() {
        _uiState.update { it.copy(uiVisible = false) }
    }

    fun onDoubleTap() {
        viewModelScope.launch {
            when (val action = settingsRepository.doubleTapAction.first()) {
                DoubleTapAction.KEEP, DoubleTapAction.TOGGLE_PLAY -> toggleKeep()
                DoubleTapAction.PENDING -> togglePending()
                DoubleTapAction.TO_DELETE -> setStatus(MarkStatus.TO_DELETE)
            }
        }
    }

    /** 收藏：点亮=收藏；再点取消 → 待删除。 */
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
        setStatus(next)
    }

    /** 待定：点亮=待定；再点取消 → 待删除。 */
    fun togglePending() {
        val current = _uiState.value.markStatus
        val next = if (current == MarkStatus.PENDING) MarkStatus.TO_DELETE else MarkStatus.PENDING
        setStatus(next)
    }

    fun setStatus(status: MarkStatus) {
        val image = _uiState.value.image ?: return
        viewModelScope.launch {
            markImageUseCase.setStatus(listOf(image.id), status)
            if (status == MarkStatus.KEEP) {
                markImageUseCase.setStar(listOf(image.id), 1)
            } else {
                markImageUseCase.setStar(listOf(image.id), 0)
            }
            val root = rootFolderRepository.getCurrent()
            if (root != null && (status == MarkStatus.KEEP || status == MarkStatus.PENDING)) {
                withContext(Dispatchers.IO) {
                    browseStateStore.upsertMark(
                        Uri.parse(root.treeUri),
                        image.relativePath,
                        status,
                    )
                }
            }
            val updated = imageRepository.getById(image.id)
            var pendingScroll: Int? = null
            var jumpNextPage = false
            if (settingsRepository.markAutoNext.first()) {
                val idx = _uiState.value.currentIndex
                when {
                    idx + 1 < _uiState.value.total -> pendingScroll = idx + 1
                    playlistSession.current()?.hasNextPage == true -> jumpNextPage = true
                }
            }
            _uiState.update {
                it.copy(
                    image = updated,
                    markStatus = status,
                    pendingScrollIndex = pendingScroll ?: it.pendingScrollIndex,
                )
            }
            if (jumpNextPage) goNext()
        }
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
            docUriByPage.clear()
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
            docUriByPage.clear()
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
}
