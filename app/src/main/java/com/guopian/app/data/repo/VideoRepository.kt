package com.guopian.app.data.repo

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.guopian.app.data.db.VideoDao
import com.guopian.app.data.db.VideoEntity
import com.guopian.app.data.db.VideoQueryBuilder
import com.guopian.app.domain.model.FilterSpec
import com.guopian.app.domain.model.MarkStatus
import com.guopian.app.domain.model.SortSpec
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

data class MarkPatch(
    val status: MarkStatus? = null,
    val star: Int? = null,
    val color: String? = null,
    val clearColor: Boolean = false,
    val note: String? = null,
    val clearNote: Boolean = false,
)

@Singleton
class VideoRepository @Inject constructor(
    private val videoDao: VideoDao,
) {
    fun observeCount(rootId: Long): Flow<Int> = videoDao.observeCount(rootId)

    fun pagingVideos(
        rootId: Long,
        filter: FilterSpec = FilterSpec(),
        sort: SortSpec = SortSpec(),
        randomSeed: Long = System.currentTimeMillis(),
    ): Flow<PagingData<VideoEntity>> {
        return Pager(
            config = PagingConfig(
                pageSize = 40,
                enablePlaceholders = false,
                prefetchDistance = 20,
            ),
            pagingSourceFactory = {
                videoDao.pagingByQuery(
                    VideoQueryBuilder.buildPagingQuery(rootId, filter, sort, randomSeed),
                )
            },
        ).flow
    }

    suspend fun queryIds(
        rootId: Long,
        filter: FilterSpec,
        sort: SortSpec,
        randomSeed: Long,
    ): List<Long> {
        return videoDao.queryIds(
            VideoQueryBuilder.buildIdsQuery(rootId, filter, sort, randomSeed),
        )
    }

    suspend fun countFiltered(rootId: Long, filter: FilterSpec): Int {
        return videoDao.countByQuery(VideoQueryBuilder.buildCountQuery(rootId, filter))
    }

    suspend fun getById(id: Long): VideoEntity? = videoDao.getById(id)

    fun observeById(id: Long): Flow<VideoEntity?> = videoDao.observeById(id)

    suspend fun saveDisplaySizeIfEmpty(id: Long, width: Int, height: Int, rotation: Int) {
        if (width <= 0 || height <= 0) return
        videoDao.updateDisplaySizeIfEmpty(id, width, height, rotation)
    }

    suspend fun saveDurationIfEmpty(id: Long, durationMs: Long) {
        if (durationMs <= 0L) return
        videoDao.updateDurationIfEmpty(id, durationMs)
    }

    suspend fun applyMarkPatch(ids: List<Long>, patch: MarkPatch) {
        if (ids.isEmpty()) return
        videoDao.updateMarkPatch(
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
