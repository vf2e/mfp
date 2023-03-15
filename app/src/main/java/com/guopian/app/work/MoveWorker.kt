package com.guopian.app.work

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.guopian.app.data.db.FolderPresetDao
import com.guopian.app.data.db.FolderPresetEntity
import com.guopian.app.data.db.TaskDao
import com.guopian.app.data.db.VideoDao
import com.guopian.app.data.move.MoveExecutor
import com.guopian.app.domain.model.ConflictPolicy
import com.guopian.app.domain.model.MarkStatus
import com.guopian.app.domain.model.TaskItemState
import com.guopian.app.domain.model.TaskState
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber

@HiltWorker
class MoveWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val taskDao: TaskDao,
    private val videoDao: VideoDao,
    private val folderPresetDao: FolderPresetDao,
    private val moveExecutor: MoveExecutor,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val taskId = inputData.getLong(KEY_TASK_ID, -1L)
        if (taskId < 0) return Result.failure()
        val task = taskDao.getById(taskId) ?: return Result.failure()
        val targetUri = Uri.parse(task.targetUri ?: return Result.failure())
        val policy = runCatching { ConflictPolicy.valueOf(task.conflictPolicy) }
            .getOrDefault(ConflictPolicy.RENAME)

        try {
            applicationContext.contentResolver.takePersistableUriPermission(
                targetUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            )
        } catch (_: SecurityException) {
            try {
                applicationContext.contentResolver.takePersistableUriPermission(
                    targetUri,
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                )
            } catch (t: SecurityException) {
                Timber.w(t, "persist target permission")
            }
        }

        val presetName = targetUri.lastPathSegment ?: "目标文件夹"
        val existingPreset = folderPresetDao.getByUri(targetUri.toString())
        folderPresetDao.upsert(
            (existingPreset ?: FolderPresetEntity(name = presetName, uri = targetUri.toString()))
                .copy(name = presetName, lastUsedAt = System.currentTimeMillis()),
        )

        val items = taskDao.getItems(taskId)
        taskDao.updateProgress(
            id = taskId,
            state = TaskState.RUNNING.name,
            current = 0,
            total = items.size,
            error = null,
            finishedAt = null,
        )

        var success = 0
        var failed = 0
        var skipped = 0

        items.forEachIndexed { index, item ->
            if (isStopped) {
                taskDao.updateProgress(
                    taskId,
                    TaskState.CANCELLED.name,
                    index,
                    items.size,
                    "cancelled",
                    System.currentTimeMillis(),
                )
                return Result.failure()
            }
            val running = item.copy(state = TaskItemState.RUNNING.name)
            taskDao.updateItem(running)

            val video = item.videoId?.let { videoDao.getById(it) }
            val source = Uri.parse(item.sourceUri)
            val displayName = video?.fileName ?: source.lastPathSegment ?: "video"
            val size = video?.sizeBytes ?: 0L

            val result = moveExecutor.moveOne(
                sourceUri = source,
                targetTreeUri = targetUri,
                displayName = displayName,
                sizeBytes = size,
                policy = policy,
            )

            when {
                result.skipped -> {
                    skipped++
                    taskDao.updateItem(
                        item.copy(state = TaskItemState.SKIPPED.name, error = "SKIP"),
                    )
                }
                result.success && result.destUri != null -> {
                    success++
                    val destName = result.destDisplayName ?: displayName
                    val relative = destName
                    if (item.videoId != null) {
                        videoDao.rebindAfterMove(
                            id = item.videoId,
                            docUri = result.destUri.toString(),
                            documentId = result.destDocumentId,
                            relativePath = relative,
                            parentSubPath = "",
                            fileName = destName,
                            markStatus = MarkStatus.MOVED.name,
                            lastScannedAt = System.currentTimeMillis(),
                        )
                    }
                    taskDao.updateItem(
                        item.copy(
                            state = TaskItemState.SUCCESS.name,
                            destUri = result.destUri.toString(),
                            error = null,
                        ),
                    )
                }
                else -> {
                    failed++
                    taskDao.updateItem(
                        item.copy(
                            state = TaskItemState.FAILED.name,
                            error = result.error ?: "UNKNOWN",
                            destUri = result.destUri?.toString(),
                        ),
                    )
                }
            }

            setProgress(
                workDataOf(
                    KEY_CURRENT to (index + 1),
                    KEY_TOTAL to items.size,
                ),
            )
            taskDao.updateProgress(
                taskId,
                TaskState.RUNNING.name,
                index + 1,
                items.size,
                null,
                null,
            )
        }

        val finalState = when {
            failed == 0 && skipped == items.size -> TaskState.SUCCESS
            failed == 0 -> TaskState.SUCCESS
            success == 0 && skipped == 0 -> TaskState.FAILED
            else -> TaskState.FAILED
        }
        val summary = buildString {
            append("完成 $success")
            if (skipped > 0) append(" · 跳过 $skipped")
            if (failed > 0) append(" · 失败 $failed")
        }
        taskDao.updateProgress(
            taskId,
            finalState.name,
            items.size,
            items.size,
            summary,
            System.currentTimeMillis(),
        )
        return if (finalState == TaskState.SUCCESS) Result.success() else Result.failure()
    }

    companion object {
        const val UNIQUE_PREFIX = "guopian_move_"
        const val KEY_TASK_ID = "task_id"
        const val KEY_CURRENT = "current"
        const val KEY_TOTAL = "total"
    }
}
