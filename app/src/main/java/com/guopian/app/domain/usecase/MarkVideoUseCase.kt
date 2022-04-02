package com.guopian.app.domain.usecase

import com.guopian.app.data.db.VideoEntity
import com.guopian.app.data.repo.MarkPatch
import com.guopian.app.data.repo.VideoRepository
import com.guopian.app.domain.model.MarkStatus
import javax.inject.Inject
import javax.inject.Singleton

data class MarkSnapshot(
    val status: MarkStatus,
    val star: Int,
    val color: String?,
    val note: String?,
)

@Singleton
class MarkVideoUseCase @Inject constructor(
    private val videoRepository: VideoRepository,
) {
    suspend fun snapshot(videoId: Long): MarkSnapshot? {
        val v = videoRepository.getById(videoId) ?: return null
        return MarkSnapshot(
            status = runCatching { MarkStatus.valueOf(v.markStatus) }.getOrDefault(MarkStatus.TO_DELETE),
            star = v.star,
            color = v.color,
            note = v.note,
        )
    }

    suspend fun setStatus(ids: List<Long>, status: MarkStatus) {
        videoRepository.applyMarkPatch(ids, MarkPatch(status = status))
    }

    suspend fun setStar(ids: List<Long>, star: Int) {
        videoRepository.applyMarkPatch(ids, MarkPatch(star = star.coerceIn(0, 5)))
    }

    suspend fun setColor(ids: List<Long>, color: String?) {
        if (color == null) {
            videoRepository.applyMarkPatch(ids, MarkPatch(clearColor = true))
        } else {
            videoRepository.applyMarkPatch(ids, MarkPatch(color = color))
        }
    }

    suspend fun setNote(ids: List<Long>, note: String?) {
        if (note.isNullOrBlank()) {
            videoRepository.applyMarkPatch(ids, MarkPatch(clearNote = true))
        } else {
            videoRepository.applyMarkPatch(ids, MarkPatch(note = note))
        }
    }

    suspend fun restore(videoId: Long, snapshot: MarkSnapshot) {
        videoRepository.applyMarkPatch(
            listOf(videoId),
            MarkPatch(
                status = snapshot.status,
                star = snapshot.star,
                color = snapshot.color,
                clearColor = snapshot.color == null,
                note = snapshot.note,
                clearNote = snapshot.note == null,
            ),
        )
    }

    fun statusOf(entity: VideoEntity): MarkStatus =
        runCatching { MarkStatus.valueOf(entity.markStatus) }.getOrDefault(MarkStatus.TO_DELETE)
}
