package com.guotu.app.ui.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.guotu.app.data.db.ExtCountRow
import com.guotu.app.data.db.FolderCountRow
import com.guotu.app.data.db.StatusCountRow
import com.guotu.app.data.db.ImageDao
import com.guotu.app.data.repo.RootFolderRepository
import com.guotu.app.domain.model.MarkStatus
import com.guotu.app.ui.library.LibraryBrowseRequest
import com.guotu.app.ui.library.LibraryFilterBridge
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class CategoryBucket(
    val title: String,
    val count: Int,
    val request: LibraryBrowseRequest,
)

data class CategoriesUiState(
    val hasRoot: Boolean = false,
    val total: Int = 0,
    val statusBuckets: List<CategoryBucket> = emptyList(),
    val extBuckets: List<CategoryBucket> = emptyList(),
    val folderBuckets: List<CategoryBucket> = emptyList(),
)

@HiltViewModel
class CategoriesViewModel @Inject constructor(
    private val rootFolderRepository: RootFolderRepository,
    private val imageDao: ImageDao,
    private val filterBridge: LibraryFilterBridge,
) : ViewModel() {

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<CategoriesUiState> = rootFolderRepository.observeCurrent()
        .flatMapLatest { root ->
            if (root == null) {
                flowOf(CategoriesUiState(hasRoot = false))
            } else {
                kotlinx.coroutines.flow.combine(
                    imageDao.observeCount(root.id),
                    imageDao.observeStatusCounts(root.id),
                    imageDao.observeExtCounts(root.id),
                    imageDao.observeFolderCounts(root.id),
                ) { total, statuses, exts, folders ->
                    CategoriesUiState(
                        hasRoot = true,
                        total = total,
                        statusBuckets = buildStatusBuckets(statuses),
                        extBuckets = exts.map { row ->
                            val ext = row.ext.ifBlank { "unknown" }
                            CategoryBucket(
                                title = ".$ext",
                                count = row.cnt,
                                request = LibraryBrowseRequest.ByExtension(
                                    ext = ext,
                                    label = ".$ext",
                                ),
                            )
                        },
                        folderBuckets = folders.map { row ->
                            val path = row.parentSubPath
                            val title = when {
                                path.isBlank() || path == "." -> "根目录"
                                else -> path
                            }
                            CategoryBucket(
                                title = title,
                                count = row.cnt,
                                request = LibraryBrowseRequest.ByFolder(
                                    parentSubPath = path,
                                    label = title,
                                ),
                            )
                        },
                    )
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = CategoriesUiState(),
        )

    fun openBucket(bucket: CategoryBucket) {
        filterBridge.open(bucket.request)
    }

    private fun buildStatusBuckets(rows: List<StatusCountRow>): List<CategoryBucket> {
        val map = rows.associate { it.markStatus to it.cnt }
        val toDelete = (map[MarkStatus.TO_DELETE.name] ?: 0) + (map[MarkStatus.UNSORTED.name] ?: 0)
        return listOf(
            Triple("收藏", setOf(MarkStatus.KEEP), map[MarkStatus.KEEP.name] ?: 0),
            Triple("待定", setOf(MarkStatus.PENDING), map[MarkStatus.PENDING.name] ?: 0),
            Triple("待删除", setOf(MarkStatus.TO_DELETE, MarkStatus.UNSORTED), toDelete),
            Triple("已剪切", setOf(MarkStatus.MOVED), map[MarkStatus.MOVED.name] ?: 0),
        ).map { (title, statuses, count) ->
            CategoryBucket(
                title = title,
                count = count,
                request = LibraryBrowseRequest.ByMarks(statuses = statuses, label = title),
            )
        }
    }
}
