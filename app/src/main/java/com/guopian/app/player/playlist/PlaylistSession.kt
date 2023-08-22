package com.guopian.app.player.playlist

import com.guopian.app.data.repo.VideoRepository
import com.guopian.app.domain.model.FilterSpec
import com.guopian.app.domain.model.SortSpec
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaylistSession @Inject constructor(
    private val videoRepository: VideoRepository,
) {
    data class Snapshot(
        val sessionId: String,
        val rootId: Long,
        val filter: FilterSpec,
        val sort: SortSpec,
        val ids: LongArray,
        val randomSeed: Long,
    )

    @Volatile
    private var snapshot: Snapshot? = null

    data class OpenResult(
        val snapshot: Snapshot,
        val startIndex: Int,
    )

    suspend fun open(
        rootId: Long,
        filter: FilterSpec,
        sort: SortSpec,
        startVideoId: Long,
        randomSeed: Long = System.currentTimeMillis(),
    ): OpenResult {
        val ids = videoRepository.queryIds(rootId, filter, sort, randomSeed)
        val index = ids.indexOf(startVideoId).let { if (it >= 0) it else 0 }
        val snap = Snapshot(
            sessionId = UUID.randomUUID().toString(),
            rootId = rootId,
            filter = filter,
            sort = sort,
            ids = ids.toLongArray(),
            randomSeed = randomSeed,
        )
        snapshot = snap
        return OpenResult(snap, index)
    }

    fun current(): Snapshot? = snapshot

    fun idAt(index: Int): Long? {
        val ids = snapshot?.ids ?: return null
        if (index !in ids.indices) return null
        return ids[index]
    }

    fun size(): Int = snapshot?.ids?.size ?: 0

    fun close() {
        snapshot = null
    }
}
