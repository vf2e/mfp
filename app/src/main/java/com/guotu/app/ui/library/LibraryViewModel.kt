package com.guotu.app.ui.library

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.guotu.app.data.db.ImageEntity
import com.guotu.app.data.prefs.SettingsRepository
import com.guotu.app.data.repo.RootBrowseStateStore
import com.guotu.app.data.repo.RootFolderRepository
import com.guotu.app.data.repo.ImageRepository
import com.guotu.app.data.repo.LibraryStats
import com.guotu.app.data.db.FolderPresetDao
import com.guotu.app.data.db.FolderPresetEntity
import com.guotu.app.domain.model.ConflictPolicy
import com.guotu.app.domain.model.FilterSpec
import com.guotu.app.domain.model.MarkStatus
import com.guotu.app.domain.model.SortKey
import com.guotu.app.domain.model.SortSpec
import com.guotu.app.domain.usecase.MarkImageUseCase
import com.guotu.app.player.playlist.PlaylistSession
import com.guotu.app.work.DeleteScheduler
import com.guotu.app.work.MoveScheduler
import com.guotu.app.work.ScanScheduler
import com.guotu.app.work.ScanWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

enum class QuickFilter {
    ALL,
    UNSORTED,
    KEEP,
    PENDING,
    TO_DELETE,
    MOVED,
}

data class LibraryUiState(
    val rootResolved: Boolean = false,
    val hasRoot: Boolean = false,
    val rootId: Long? = null,
    val rootPath: String? = null,
    val imageCount: Int = 0,
    val isScanning: Boolean = false,
    val scanFound: Int = 0,
    val quickFilter: QuickFilter = QuickFilter.ALL,
    val pathKeyword: String = "",
    val queryText: String = "",
    val filterExtensions: Set<String> = emptySet(),
    val filterParentPath: String? = null,
    val filterTagId: Long? = null,
    val markOverride: Set<MarkStatus>? = null,
    val externalFilterLabel: String? = null,
    val selectionMode: Boolean = false,
    val selectedIds: Set<Long> = emptySet(),
    val sort: SortSpec = SortSpec(),
    val showCutSheet: Boolean = false,
    val conflictPolicy: ConflictPolicy = ConflictPolicy.RENAME,
    val cutMessage: String? = null,
    val scanError: String? = null,
    val scanResultMessage: String? = null,
    val lastRelativePath: String? = null,
    val pageSize: Int = 100,
    val pageIndex: Int = 0,
    val pageCount: Int = 0,
    val pageLoading: Boolean = true,
    val gridColumns: Int = 0,
    val stats: LibraryStats = LibraryStats(),
    val actionMessage: String? = null,
)

@HiltViewModel
class LibraryViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val rootFolderRepository: RootFolderRepository,
    private val imageRepository: ImageRepository,
    private val scanScheduler: ScanScheduler,
    private val markImageUseCase: MarkImageUseCase,
    private val playlistSession: PlaylistSession,
    private val moveScheduler: MoveScheduler,
    private val deleteScheduler: DeleteScheduler,
    private val folderPresetDao: FolderPresetDao,
    private val filterBridge: LibraryFilterBridge,
    private val tagDao: com.guotu.app.data.db.TagDao,
    private val settingsRepository: SettingsRepository,
    private val browseStateStore: RootBrowseStateStore,
) : ViewModel() {

    private val rootFlow = rootFolderRepository.observeCurrent()
    private val rootState = MutableStateFlow<com.guotu.app.data.db.RootFolderEntity?>(null)
    private val rootResolved = MutableStateFlow(false)
    private val filterState = MutableStateFlow(LibraryUiState())

    val folderPresets: StateFlow<List<FolderPresetEntity>> = folderPresetDao.observeRecent()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList(),
        )

    val tags: StateFlow<List<com.guotu.app.data.db.TagEntity>> = tagDao.observeAll()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList(),
        )

    private val scanInfoState: StateFlow<WorkInfo?> = WorkManager.getInstance(context)
        .getWorkInfosForUniqueWorkFlow(ScanWorker.UNIQUE_WORK)
        .map { infos -> infos.firstOrNull() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val randomSeed = System.currentTimeMillis()
    private val lastRelativePath = MutableStateFlow<String?>(null)
    private val pageSizeFlow = settingsRepository.pageSize
        .stateIn(viewModelScope, SharingStarted.Eagerly, 100)
    private val gridColumnsFlow = settingsRepository.gridColumns
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)
    private val pageIndex = MutableStateFlow(0)
    private val pageReload = MutableStateFlow(0)
    private val pageLoading = MutableStateFlow(true)
    private val actionMessage = MutableStateFlow<String?>(null)
    private var appliedDefaultFilter = false

    @OptIn(ExperimentalCoroutinesApi::class)
    private val statsFlow: Flow<LibraryStats> = rootState.flatMapLatest { root ->
        if (root == null) flowOf(LibraryStats()) else imageRepository.observeStats(root.id)
    }

    private val _pageImages = MutableStateFlow<List<ImageEntity>>(emptyList())
    val pageImages: StateFlow<List<ImageEntity>> = _pageImages.asStateFlow()

    private val filteredCount = MutableStateFlow(0)
    private val scanResultMessage = MutableStateFlow<String?>(null)
    private var lastHandledScanId: UUID? = null

    init {
        viewModelScope.launch {
            if (!appliedDefaultFilter && settingsRepository.defaultUnreviewed.first()) {
                appliedDefaultFilter = true
                filterState.update { it.copy(quickFilter = QuickFilter.UNSORTED) }
            }
            bootstrapLibrary()
            observePageQueries()
        }
        viewModelScope.launch {
            rootFlow.collect { root ->
                if (root?.id != rootState.value?.id || root?.treeUri != rootState.value?.treeUri) {
                    rootState.value = root
                }
                if (root == null) {
                    lastRelativePath.value = null
                } else if (lastRelativePath.value == null) {
                    lastRelativePath.value = withContext(Dispatchers.IO) {
                        browseStateStore.read(Uri.parse(root.treeUri)).lastRelativePath
                    }
                }
            }
        }
        viewModelScope.launch {
            rootResolved.first { it }
            combine(rootState, filterState) { root, ui -> root to ui }.collect { (root, ui) ->
                if (root == null) {
                    filteredCount.value = 0
                } else {
                    filteredCount.value = withContext(Dispatchers.IO) {
                        imageRepository.countFiltered(root.id, ui.toFilterSpec())
                    }
                }
            }
        }
        viewModelScope.launch {
            combine(filterState, pageSizeFlow) { _, _ -> }.collect {
                if (pageIndex.value != 0) pageIndex.value = 0
            }
        }
        viewModelScope.launch {
            combine(filteredCount, pageSizeFlow, pageIndex) { count, size, index ->
                Triple(count, size, index)
            }.collect { (count, size, index) ->
                val pages = if (count <= 0 || size <= 0) 0 else (count + size - 1) / size
                if (pages > 0 && index >= pages) {
                    pageIndex.value = pages - 1
                }
            }
        }
        viewModelScope.launch {
            filterBridge.pending.collect { request ->
                if (request != null) {
                    applyBrowseRequest(request)
                    filterBridge.consume()
                }
            }
        }
        viewModelScope.launch {
            var ignoreHistorical = true
            scanInfoState.collect { work ->
                if (ignoreHistorical) {
                    ignoreHistorical = false
                    lastHandledScanId = work?.takeIf {
                        it.state == WorkInfo.State.SUCCEEDED || it.state == WorkInfo.State.FAILED
                    }?.id
                    return@collect
                }
                if (work == null) return@collect
                if (work.state == WorkInfo.State.SUCCEEDED && work.id != lastHandledScanId) {
                    lastHandledScanId = work.id
                    val n = work.outputData.getInt(ScanWorker.KEY_IMAGE_COUNT, 0)
                    val folder = work.outputData.getString(ScanWorker.KEY_FOLDER).orEmpty().ifBlank { "所选文件夹" }
                    scanResultMessage.value = if (n <= 0) {
                        "「$folder」内未发现图片。请改选实际有图片的目录（如 DCIM / Pictures）。"
                    } else {
                        "扫描完成：在「$folder」发现 $n 张图片"
                    }
                    val root = rootFolderRepository.getCurrent()
                    if (root != null) {
                        filteredCount.value = withContext(Dispatchers.IO) {
                            imageRepository.countFiltered(root.id, filterState.value.toFilterSpec())
                        }
                    }
                    bumpPageReload()
                } else if (work.state == WorkInfo.State.FAILED && work.id != lastHandledScanId) {
                    lastHandledScanId = work.id
                    scanResultMessage.value = work.outputData.getString(ScanWorker.KEY_ERROR)
                        ?: "扫描失败，请重新选择文件夹"
                }
            }
        }
    }

    private suspend fun bootstrapLibrary() {
        pageLoading.value = true
        val (root, pageSize) = withContext(Dispatchers.IO) {
            rootFolderRepository.getCurrent() to settingsRepository.pageSize.first()
        }
        rootState.value = root
        if (root == null) {
            _pageImages.value = emptyList()
            filteredCount.value = 0
            pageLoading.value = false
            rootResolved.value = true
            return
        }
        val ui = filterState.value
        val (count, page) = withContext(Dispatchers.IO) {
            val filter = ui.toFilterSpec()
            val c = imageRepository.countFiltered(root.id, filter)
            val p = imageRepository.queryPage(
                rootId = root.id,
                filter = filter,
                sort = ui.sort,
                randomSeed = randomSeed,
                pageSize = pageSize,
                pageIndex = 0,
            )
            c to p
        }
        filteredCount.value = count
        _pageImages.value = page
        lastRelativePath.value = withContext(Dispatchers.IO) {
            browseStateStore.read(Uri.parse(root.treeUri)).lastRelativePath
        }
        pageLoading.value = false
        rootResolved.value = true
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun observePageQueries() {
        combine(
            rootState,
            filterState,
            pageSizeFlow,
            pageIndex,
            pageReload,
        ) { root, ui, pageSize, index, _ ->
            PageQuery(root?.id, ui, pageSize, index)
        }.mapLatest { query ->
            val rootId = query.rootId
            if (rootId == null) {
                pageLoading.value = false
                emptyList()
            } else {
                pageLoading.value = true
                try {
                    withContext(Dispatchers.IO) {
                        imageRepository.queryPage(
                            rootId = rootId,
                            filter = query.ui.toFilterSpec(),
                            sort = query.ui.sort,
                            randomSeed = randomSeed,
                            pageSize = query.pageSize,
                            pageIndex = query.pageIndex,
                        )
                    }
                } finally {
                    pageLoading.value = false
                }
            }
        }.collect { list ->
            _pageImages.value = list
        }
    }

    private fun bumpPageReload() {
        pageReload.update { it + 1 }
    }

    fun nextPage() {
        val count = filteredCount.value
        val size = pageSizeFlow.value
        val pages = if (count <= 0 || size <= 0) 0 else (count + size - 1) / size
        if (pageIndex.value + 1 < pages) {
            pageIndex.value = pageIndex.value + 1
        }
    }

    fun previousPage() {
        if (pageIndex.value > 0) {
            pageIndex.value = pageIndex.value - 1
        }
    }

    private fun applyBrowseRequest(request: LibraryBrowseRequest) {
        pageIndex.value = 0
        filterState.update {
            when (request) {
                is LibraryBrowseRequest.ByMarks -> it.copy(
                    quickFilter = QuickFilter.ALL,
                    pathKeyword = "",
                    queryText = "",
                    filterExtensions = emptySet(),
                    filterParentPath = null,
                    filterTagId = null,
                    externalFilterLabel = request.label,
                    markOverride = request.statuses,
                )
                is LibraryBrowseRequest.ByExtension -> it.copy(
                    quickFilter = QuickFilter.ALL,
                    pathKeyword = "",
                    queryText = "",
                    filterExtensions = setOf(request.ext.lowercase()),
                    filterParentPath = null,
                    filterTagId = null,
                    externalFilterLabel = request.label,
                    markOverride = null,
                )
                is LibraryBrowseRequest.ByFolder -> it.copy(
                    quickFilter = QuickFilter.ALL,
                    pathKeyword = "",
                    queryText = "",
                    filterExtensions = emptySet(),
                    filterParentPath = request.parentSubPath,
                    filterTagId = null,
                    externalFilterLabel = request.label,
                    markOverride = null,
                )
                is LibraryBrowseRequest.ByTag -> it.copy(
                    quickFilter = QuickFilter.ALL,
                    pathKeyword = "",
                    queryText = "",
                    filterExtensions = emptySet(),
                    filterParentPath = null,
                    filterTagId = request.tagId,
                    externalFilterLabel = request.label,
                    markOverride = null,
                )
            }
        }
    }

    fun clearExternalFilter() {
        pageIndex.value = 0
        filterState.update {
            it.copy(
                filterExtensions = emptySet(),
                filterParentPath = null,
                filterTagId = null,
                externalFilterLabel = null,
                markOverride = null,
            )
        }
    }

    val uiState: StateFlow<LibraryUiState> = combine(
        combine(rootState, filteredCount, scanInfoState, rootResolved) { root, count, work, resolved ->
            RootBundle(root, count, work, resolved)
        },
        combine(
            combine(filterState, scanResultMessage, lastRelativePath) { filter, scanMsg, last ->
                Triple(filter, scanMsg, last)
            },
            combine(pageSizeFlow, gridColumnsFlow, actionMessage) { pageSize, gridColumns, action ->
                Triple(pageSize, gridColumns, action)
            },
            combine(pageIndex, pageLoading, statsFlow) { index, loading, stats ->
                Triple(index, loading, stats)
            },
        ) { filterTriple, pageTriple, pageMeta ->
            LibraryExtras(
                filter = filterTriple.first,
                scanMsg = filterTriple.second,
                lastPath = filterTriple.third,
                pageSize = pageTriple.first,
                gridColumns = pageTriple.second,
                action = pageTriple.third,
                pageIndex = pageMeta.first,
                pageLoading = pageMeta.second,
                stats = pageMeta.third,
            )
        },
    ) { rootBundle, extras ->
        val running = rootBundle.work?.state == WorkInfo.State.RUNNING ||
            rootBundle.work?.state == WorkInfo.State.ENQUEUED
        val found = rootBundle.work?.progress?.getInt(ScanWorker.KEY_FOUND, 0) ?: 0
        val scanError = if (rootBundle.work?.state == WorkInfo.State.FAILED) {
            rootBundle.work.outputData.getString(ScanWorker.KEY_ERROR) ?: "扫描失败"
        } else {
            null
        }
        val pages = if (rootBundle.count <= 0 || extras.pageSize <= 0) {
            0
        } else {
            (rootBundle.count + extras.pageSize - 1) / extras.pageSize
        }
        extras.filter.copy(
            rootResolved = rootBundle.resolved,
            hasRoot = rootBundle.root != null,
            rootId = rootBundle.root?.id,
            rootPath = rootBundle.root?.displayPath,
            imageCount = rootBundle.count,
            isScanning = running,
            scanFound = found,
            scanError = scanError,
            scanResultMessage = extras.scanMsg,
            lastRelativePath = extras.lastPath,
            pageSize = extras.pageSize,
            pageIndex = extras.pageIndex,
            pageCount = pages,
            pageLoading = extras.pageLoading,
            gridColumns = extras.gridColumns,
            stats = extras.stats,
            actionMessage = extras.action,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = LibraryUiState(),
    )

    private data class RootBundle(
        val root: com.guotu.app.data.db.RootFolderEntity?,
        val count: Int,
        val work: WorkInfo?,
        val resolved: Boolean,
    )

    private data class LibraryExtras(
        val filter: LibraryUiState,
        val scanMsg: String?,
        val lastPath: String?,
        val pageSize: Int,
        val gridColumns: Int,
        val action: String?,
        val pageIndex: Int,
        val pageLoading: Boolean,
        val stats: LibraryStats,
    )

    private data class PageQuery(
        val rootId: Long?,
        val ui: LibraryUiState,
        val pageSize: Int,
        val pageIndex: Int,
    )

    fun onRootSelected(uri: Uri) {
        viewModelScope.launch {
            rootFolderRepository.setRootTreeUri(uri)
            scanScheduler.enqueueFullScan()
        }
    }

    fun onRootFilePathSelected(absolutePath: String) {
        viewModelScope.launch {
            rootFolderRepository.setRootFilePath(absolutePath)
            scanScheduler.enqueueFullScan()
        }
    }

    fun scanNow() {
        scanScheduler.enqueueFullScan()
    }

    fun setQuickFilter(filter: QuickFilter) {
        pageIndex.value = 0
        filterState.update {
            it.copy(
                quickFilter = filter,
                markOverride = null,
                filterExtensions = emptySet(),
                filterParentPath = null,
                filterTagId = null,
                externalFilterLabel = null,
            )
        }
    }

    fun tagSelected(tagId: Long) {
        val ids = filterState.value.selectedIds.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            ids.forEach { imageId ->
                tagDao.link(com.guotu.app.data.db.ImageTagCrossRef(imageId = imageId, tagId = tagId))
            }
            clearSelection()
        }
    }

    fun setPathKeyword(keyword: String) {
        pageIndex.value = 0
        filterState.update { it.copy(pathKeyword = keyword) }
    }

    fun setQueryText(text: String) {
        pageIndex.value = 0
        filterState.update { it.copy(queryText = text) }
    }

    fun setSort(key: SortKey) {
        pageIndex.value = 0
        filterState.update {
            val asc = if (it.sort.key == key) !it.sort.ascending else false
            it.copy(sort = SortSpec(key, asc))
        }
    }

    fun enterSelection(imageId: Long) {
        filterState.update {
            it.copy(selectionMode = true, selectedIds = setOf(imageId))
        }
    }

    fun toggleSelect(imageId: Long) {
        filterState.update { state ->
            if (!state.selectionMode) {
                state.copy(selectionMode = true, selectedIds = setOf(imageId))
            } else {
                val next = state.selectedIds.toMutableSet()
                if (!next.add(imageId)) next.remove(imageId)
                state.copy(selectedIds = next, selectionMode = next.isNotEmpty())
            }
        }
    }

    fun clearSelection() {
        filterState.update { it.copy(selectionMode = false, selectedIds = emptySet()) }
    }

    fun markSelected(status: MarkStatus) {
        val ids = filterState.value.selectedIds.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            markImageUseCase.setStatus(ids, status)
            val root = rootFolderRepository.getCurrent()
            if (root != null) {
                val treeUri = Uri.parse(root.treeUri)
                ids.forEach { id ->
                    val image = imageRepository.getById(id) ?: return@forEach
                    browseStateStore.upsertMark(treeUri, image.relativePath, status)
                }
            }
            clearSelection()
        }
    }

    fun openCutSheet() {
        if (filterState.value.selectedIds.isEmpty()) return
        filterState.update { it.copy(showCutSheet = true) }
    }

    fun dismissCutSheet() {
        filterState.update { it.copy(showCutSheet = false) }
    }

    fun setConflictPolicy(policy: ConflictPolicy) {
        filterState.update { it.copy(conflictPolicy = policy) }
    }

    fun clearCutMessage() {
        filterState.update { it.copy(cutMessage = null) }
    }

    fun clearScanResultMessage() {
        scanResultMessage.value = null
    }

    fun clearActionMessage() {
        actionMessage.value = null
    }

    /** 全选当前筛选结果（最多 MAX_SELECT 张，避免 OOM）。 */
    fun selectAllFiltered() {
        viewModelScope.launch {
            val rootId = rootFolderRepository.getCurrent()?.id ?: return@launch
            val ui = filterState.value
            val ids = imageRepository.queryIds(
                rootId = rootId,
                filter = ui.toFilterSpec(),
                sort = ui.sort,
                randomSeed = randomSeed,
                limit = MAX_SELECT,
            )
            if (ids.isEmpty()) {
                actionMessage.value = "当前筛选下没有图片"
                return@launch
            }
            filterState.update {
                it.copy(selectionMode = true, selectedIds = ids.toSet())
            }
            if (ids.size >= MAX_SELECT) {
                actionMessage.value = "已选前 $MAX_SELECT 张（单次上限）"
            }
        }
    }

    /** 物理删除「待删除」标记的文件（最多 MAX_DELETE 张）。 */
    fun purgeToDelete() {
        viewModelScope.launch {
            val rootId = rootFolderRepository.getCurrent()?.id ?: return@launch
            val ids = imageRepository.listIdsByStatus(
                rootId = rootId,
                status = MarkStatus.TO_DELETE,
                limit = MAX_DELETE,
            )
            if (ids.isEmpty()) {
                actionMessage.value = "没有标记为「待删除」的图片"
                return@launch
            }
            val taskId = deleteScheduler.enqueueDelete(ids)
            actionMessage.value = "已加入删除任务 #$taskId（${ids.size} 张）"
        }
    }

    /** 导出收藏清单到应用 Documents，并返回绝对路径。 */
    fun exportKeepList() {
        viewModelScope.launch {
            val root = rootFolderRepository.getCurrent() ?: return@launch
            val rows = imageRepository.listMarkedPaths(root.id)
                .filter { it.markStatus == MarkStatus.KEEP.name }
            if (rows.isEmpty()) {
                actionMessage.value = "还没有收藏图片可导出"
                return@launch
            }
            val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val dir = File(context.getExternalFilesDir(null), "exports").apply { mkdirs() }
            val out = File(dir, "guotu_keep_$stamp.txt")
            out.writeText(
                buildString {
                    appendLine("# 过图收藏清单 · ${rows.size} 张")
                    appendLine("# 根目录: ${root.displayPath}")
                    appendLine("# 导出时间: $stamp")
                    appendLine()
                    rows.forEach { appendLine(it.relativePath) }
                },
                Charsets.UTF_8,
            )
            actionMessage.value = "已导出 ${rows.size} 张到 ${out.absolutePath}"
        }
    }

    fun enqueueCut(targetTreeUri: Uri) {
        val ids = filterState.value.selectedIds.toList()
        val policy = filterState.value.conflictPolicy
        if (ids.isEmpty()) return
        viewModelScope.launch {
            val taskId = moveScheduler.enqueueMove(ids, targetTreeUri, policy)
            filterState.update {
                it.copy(
                    showCutSheet = false,
                    selectionMode = false,
                    selectedIds = emptySet(),
                    cutMessage = "已加入剪切任务 #$taskId",
                )
            }
        }
    }

    fun enqueueCutPreset(preset: FolderPresetEntity) {
        enqueueCut(Uri.parse(preset.uri))
    }

    /**
     * 按当前筛选结果进入过图。
     * 优先从「未整理」起步；若当前筛选本身就是未整理/待删除，则从列表第一张开始。
     */
    suspend fun startBrowse(): Int? {
        val rootId = rootFolderRepository.getCurrent()?.id ?: return null
        val pageSize = settingsRepository.pageSize.first()
        val ui = filterState.value
        val filter = ui.toFilterSpec()
        val preferUnreviewed = filter.markStatuses.isEmpty() ||
            filter.onlyUnmarked ||
            filter.markStatuses.any {
                it == MarkStatus.TO_DELETE || it == MarkStatus.UNSORTED
            }
        val startId = if (preferUnreviewed && filter.markStatuses.isEmpty()) {
            imageRepository.queryIds(
                rootId = rootId,
                filter = filter.copy(
                    markStatuses = setOf(MarkStatus.UNSORTED, MarkStatus.TO_DELETE),
                ),
                sort = ui.sort,
                randomSeed = randomSeed,
                limit = 1,
            ).firstOrNull()
                ?: imageRepository.queryIds(
                    rootId = rootId,
                    filter = filter,
                    sort = ui.sort,
                    randomSeed = randomSeed,
                    limit = 1,
                ).firstOrNull()
        } else {
            imageRepository.queryIds(
                rootId = rootId,
                filter = filter,
                sort = ui.sort,
                randomSeed = randomSeed,
                limit = 1,
            ).firstOrNull()
        } ?: return null
        val result = playlistSession.open(
            rootId = rootId,
            filter = filter,
            sort = ui.sort,
            startImageId = startId,
            randomSeed = randomSeed,
            pageSize = pageSize,
        )
        return result.startIndex
    }

    /** 一键跳到目录 txt 记录的上次浏览位置。 */
    suspend fun resumeLastBrowse(): Int? {
        val root = rootFolderRepository.getCurrent() ?: return null
        val last = browseStateStore.read(Uri.parse(root.treeUri)).lastRelativePath
            ?: lastRelativePath.value
            ?: return null
        val imageId = imageRepository.findIdByRelativePath(root.id, last) ?: return null
        lastRelativePath.value = last
        return openSwipe(imageId)
    }

    /** 选中当前筛选列表图片并打开剪切面板（上限 MAX_SELECT）。 */
    fun prepareCutCurrentFilter() {
        viewModelScope.launch {
            val rootId = rootFolderRepository.getCurrent()?.id ?: return@launch
            val ui = filterState.value
            val ids = imageRepository.queryIds(
                rootId = rootId,
                filter = ui.toFilterSpec(),
                sort = ui.sort,
                randomSeed = randomSeed,
                limit = MAX_SELECT,
            )
            if (ids.isEmpty()) {
                filterState.update { it.copy(cutMessage = "当前筛选下没有可剪切的图片") }
                return@launch
            }
            filterState.update {
                it.copy(
                    selectionMode = true,
                    selectedIds = ids.toSet(),
                    showCutSheet = true,
                )
            }
            if (ids.size >= MAX_SELECT) {
                actionMessage.value = "本批最多剪切 $MAX_SELECT 张，可分多次处理"
            }
        }
    }

    /** 一键选中「收藏」并打开剪切（上限 MAX_SELECT；不改动当前筛选芯片）。 */
    fun prepareCutKept() {
        viewModelScope.launch {
            val rootId = rootFolderRepository.getCurrent()?.id ?: return@launch
            val ids = imageRepository.queryIds(
                rootId = rootId,
                filter = FilterSpec(markStatuses = setOf(MarkStatus.KEEP)),
                sort = filterState.value.sort,
                randomSeed = randomSeed,
                limit = MAX_SELECT,
            )
            if (ids.isEmpty()) {
                actionMessage.value = "还没有标记为「收藏」的图片"
                return@launch
            }
            filterState.update {
                it.copy(
                    selectionMode = true,
                    selectedIds = ids.toSet(),
                    showCutSheet = true,
                )
            }
            if (ids.size >= MAX_SELECT) {
                actionMessage.value = "本批最多剪切 $MAX_SELECT 张，可分多次处理"
            }
        }
    }

    suspend fun openSwipe(startImageId: Long): Int? {
        val rootId = rootFolderRepository.getCurrent()?.id ?: return null
        val ui = filterState.value
        val pageSize = settingsRepository.pageSize.first()
        val result = playlistSession.open(
            rootId = rootId,
            filter = ui.toFilterSpec(),
            sort = ui.sort,
            startImageId = startImageId,
            randomSeed = randomSeed,
            pageSize = pageSize,
        )
        return result.startIndex
    }

    fun refreshLastCursor() {
        viewModelScope.launch {
            val root = rootFolderRepository.getCurrent() ?: return@launch
            lastRelativePath.value = browseStateStore.read(Uri.parse(root.treeUri)).lastRelativePath
        }
    }
}

private fun LibraryUiState.toFilterSpec(): FilterSpec {
    val statuses = markOverride ?: when (quickFilter) {
        QuickFilter.ALL -> emptySet()
        // 未整理 = 待删除 + 旧 UNSORTED
        QuickFilter.UNSORTED -> setOf(MarkStatus.UNSORTED, MarkStatus.TO_DELETE)
        QuickFilter.KEEP -> setOf(MarkStatus.KEEP)
        QuickFilter.PENDING -> setOf(MarkStatus.PENDING)
        QuickFilter.TO_DELETE -> setOf(MarkStatus.TO_DELETE, MarkStatus.UNSORTED)
        QuickFilter.MOVED -> setOf(MarkStatus.MOVED)
    }
    return FilterSpec(
        pathContains = pathKeyword.trim().takeIf { it.isNotEmpty() }?.let { listOf(it) }.orEmpty(),
        parentSubPaths = filterParentPath?.let { listOf(it) }.orEmpty(),
        markStatuses = statuses,
        extensions = filterExtensions,
        tagId = filterTagId,
        onlyUnmarked = false,
        queryText = queryText.trim().takeIf { it.isNotEmpty() },
    )
}

private const val MAX_SELECT = 500
private const val MAX_DELETE = 2_000
