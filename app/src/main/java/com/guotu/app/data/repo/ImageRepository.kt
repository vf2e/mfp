package com.guotu.app.data.repo

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.guotu.app.data.db.ImageDao
import com.guotu.app.data.db.ImageEntity
import com.guotu.app.data.db.ImageQueryBuilder
import com.guotu.app.data.db.MarkedPathRow
import com.guotu.app.data.db.StatusCountRow
import com.guotu.app.domain.model.FilterSpec
import com.guotu.app.domain.model.MarkStatus
import com.guotu.app.domain.model.SortSpec
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

data class LibraryStats(
    val total: Int = 0,
    val keep: Int = 0,
    val pending: Int = 0,
    val toDelete: Int = 0,
    val moved: Int = 0,
) {
    val unreviewed: Int get() = (toDelete).coerceAtLeast(0)
    val progressDone: Int get() = keep + pending + moved
}

data class MarkPatch(
    val status: MarkStatus? = null,
    val star: Int? = null,
    val color: String? = null,
    val clearColor: Boolean = false,
    val note: String? = null,
    val clearNote: Boolean = false,
)

@Singleton
class ImageRepository @Inject constructor(
    private val imageDao: ImageDao,
) {
    fun observeCount(rootId: Long): Flow<Int> = imageDao.observeCount(rootId)

    fun observeStats(rootId: Long): Flow<LibraryStats> =
        imageDao.observeStatusCounts(rootId).map { rows -> rows.toStats() }

    suspend fun listIdsByStatus(rootId: Long, status: MarkStatus, limit: Int): List<Long> =
        imageDao.listIdsByStatus(rootId, status.name, limit)

    suspend fun getByIds(ids: List<Long>): List<ImageEntity> =
        if (ids.isEmpty()) emptyList() else imageDao.getByIds(ids)

    fun pagingImages(
        rootId: Long,
        filter: FilterSpec = FilterSpec(),
        sort: SortSpec = SortSpec(),
        randomSeed: Long = System.currentTimeMillis(),
        pageSize: Int = 100,
    ): Flow<PagingData<ImageEntity>> {
        val size = pageSize.coerceIn(20, 500)
        return Pager(
            config = PagingConfig(
                pageSize = size,
                enablePlaceholders = false,
                prefetchDistance = (size / 2).coerceAtLeast(10),
                maxSize = (size * 5).coerceAtLeast(200),
            ),
            pagingSourceFactory = {
                imageDao.pagingByQuery(
                    ImageQueryBuilder.buildPagingQuery(rootId, filter, sort, randomSeed),
                )
            },
        ).flow
    }

    suspend fun queryIds(
        rootId: Long,
        filter: FilterSpec,
        sort: SortSpec,
        randomSeed: Long,
        limit: Int? = null,
        offset: Int = 0,
    ): List<Long> {
        return imageDao.queryIds(
            ImageQueryBuilder.buildIdsQuery(rootId, filter, sort, randomSeed, limit, offset),
        )
    }

    suspend fun countFiltered(rootId: Long, filter: FilterSpec): Int {
        return imageDao.countByQuery(ImageQueryBuilder.buildCountQuery(rootId, filter))
    }

    suspend fun getById(id: Long): ImageEntity? = imageDao.getById(id)

    fun observeById(id: Long): Flow<ImageEntity?> = imageDao.observeById(id)

    suspend fun findIdByRelativePath(rootId: Long, relativePath: String): Long? =
        imageDao.findIdByRelativePath(rootId, relativePath)

    suspend fun listMarkedPaths(rootId: Long): List<MarkedPathRow> =
        imageDao.listMarkedPaths(rootId)

    suspend fun updateMarkByRelativePath(
        rootId: Long,
        relativePath: String,
        status: MarkStatus,
        star: Int = if (status == MarkStatus.KEEP) 1 else 0,
    ) {
        imageDao.updateMarkByRelativePath(rootId, relativePath, status.name, star)
    }

    suspend fun saveDisplaySizeIfEmpty(id: Long, width: Int, height: Int, rotation: Int) {
        if (width <= 0 || height <= 0) return
        imageDao.updateDisplaySizeIfEmpty(id, width, height, rotation)
    }

    suspend fun saveDurationIfEmpty(id: Long, durationMs: Long) {
        if (durationMs <= 0L) return
        imageDao.updateDurationIfEmpty(id, durationMs)
    }

    suspend fun applyMarkPatch(ids: List<Long>, patch: MarkPatch) {
        if (ids.isEmpty()) return
        imageDao.updateMarkPatch(
            ids = ids,
            status = patch.status?.name,
            star = patch.star,
            color = patch.color,
            clearColor = if (patch.clearColor) 1 else 0,
            note = patch.note,
            clearNote = if (patch.clearNote) 1 else 0,
        )
    }
}

private fun List<StatusCountRow>.toStats(): LibraryStats {
    var keep = 0
    var pending = 0
    var toDelete = 0
    var moved = 0
    var total = 0
    for (row in this) {
        total += row.cnt
        when (row.markStatus) {
            MarkStatus.KEEP.name -> keep += row.cnt
            MarkStatus.PENDING.name -> pending += row.cnt
            MarkStatus.MOVED.name -> moved += row.cnt
            MarkStatus.TO_DELETE.name, MarkStatus.UNSORTED.name -> toDelete += row.cnt
        }
    }
    return LibraryStats(
        total = total,
        keep = keep,
        pending = pending,
        toDelete = toDelete,
        moved = moved,
    )
}
