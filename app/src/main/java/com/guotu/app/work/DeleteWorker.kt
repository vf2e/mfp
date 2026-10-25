package com.guotu.app.work

import android.content.Context
import android.net.Uri
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.guotu.app.data.db.ImageDao
import com.guotu.app.data.db.TaskDao
import com.guotu.app.data.move.DeleteExecutor
import com.guotu.app.domain.model.TaskItemState
import com.guotu.app.domain.model.TaskState
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber

@HiltWorker
class DeleteWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val taskDao: TaskDao,
    private val imageDao: ImageDao,
    private val deleteExecutor: DeleteExecutor,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val taskId = inputData.getLong(KEY_TASK_ID, -1L)
        if (taskId < 0) return Result.failure()
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
        val deletedIds = ArrayList<Long>(items.size)

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
            taskDao.updateItem(item.copy(state = TaskItemState.RUNNING.name))
            val source = Uri.parse(item.sourceUri)
            val result = deleteExecutor.deleteOne(source)
            if (result.success) {
                success++
                item.imageId?.let { deletedIds += it }
                taskDao.updateItem(item.copy(state = TaskItemState.SUCCESS.name, error = null))
            } else {
                failed++
                Timber.w("delete failed id=%s err=%s", item.imageId, result.error)
                taskDao.updateItem(
                    item.copy(state = TaskItemState.FAILED.name, error = result.error ?: "UNKNOWN"),
                )
            }
            setProgress(workDataOf(KEY_CURRENT to (index + 1), KEY_TOTAL to items.size))
            taskDao.updateProgress(
                taskId,
                TaskState.RUNNING.name,
                index + 1,
                items.size,
                null,
                null,
            )
        }

        if (deletedIds.isNotEmpty()) {
            deletedIds.chunked(200).forEach { chunk ->
                imageDao.deleteByIds(chunk)
            }
        }

        val finalState = if (failed == 0) TaskState.SUCCESS else TaskState.FAILED
        val summary = buildString {
            append("已删除 $success")
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
        const val UNIQUE_PREFIX = "guotu_delete_"
        const val KEY_TASK_ID = "task_id"
        const val KEY_CURRENT = "current"
        const val KEY_TOTAL = "total"
    }
}
