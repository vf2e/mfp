package com.guotu.app.domain.usecase

import com.guotu.app.data.db.ImageEntity
import com.guotu.app.data.repo.MarkPatch
import com.guotu.app.data.repo.ImageRepository
import com.guotu.app.domain.model.MarkStatus
import javax.inject.Inject
import javax.inject.Singleton

data class MarkSnapshot(
    val status: MarkStatus,
    val star: Int,
    val color: String?,
    val note: String?,
)

@Singleton
class MarkImageUseCase @Inject constructor(
    private val imageRepository: ImageRepository,
) {
    suspend fun snapshot(imageId: Long): MarkSnapshot? {
        val v = imageRepository.getById(imageId) ?: return null
        return MarkSnapshot(
            status = runCatching { MarkStatus.valueOf(v.markStatus) }.getOrDefault(MarkStatus.TO_DELETE),
            star = v.star,
            color = v.color,
            note = v.note,
        )
    }

    suspend fun setStatus(ids: List<Long>, status: MarkStatus) {
        imageRepository.applyMarkPatch(ids, MarkPatch(status = status))
    }

    suspend fun setStar(ids: List<Long>, star: Int) {
        imageRepository.applyMarkPatch(ids, MarkPatch(star = star.coerceIn(0, 5)))
    }

    suspend fun setColor(ids: List<Long>, color: String?) {
        if (color == null) {
            imageRepository.applyMarkPatch(ids, MarkPatch(clearColor = true))
        } else {
            imageRepository.applyMarkPatch(ids, MarkPatch(color = color))
        }
    }

    suspend fun setNote(ids: List<Long>, note: String?) {
        if (note.isNullOrBlank()) {
            imageRepository.applyMarkPatch(ids, MarkPatch(clearNote = true))
        } else {
            imageRepository.applyMarkPatch(ids, MarkPatch(note = note))
        }
    }

    suspend fun restore(imageId: Long, snapshot: MarkSnapshot) {
        imageRepository.applyMarkPatch(
            listOf(imageId),
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

    fun statusOf(entity: ImageEntity): MarkStatus =
        runCatching { MarkStatus.valueOf(entity.markStatus) }.getOrDefault(MarkStatus.TO_DELETE)
}
