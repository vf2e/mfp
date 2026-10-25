package com.guotu.app.work

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.guotu.app.data.db.ImageDao
import com.guotu.app.data.db.TaskDao
import com.guotu.app.data.db.TaskEntity
import com.guotu.app.data.db.TaskItemEntity
import com.guotu.app.domain.model.TaskItemState
import com.guotu.app.domain.model.TaskState
import com.guotu.app.domain.model.TaskType
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeleteScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val taskDao: TaskDao,
    private val imageDao: ImageDao,
) {
    suspend fun enqueueDelete(imageIds: List<Long>): Long {
        require(imageIds.isNotEmpty())
        val taskId = taskDao.insert(
            TaskEntity(
                type = TaskType.DELETE.name,
                state = TaskState.PENDING.name,
                targetUri = null,
                conflictPolicy = "N/A",
                progressCurrent = 0,
                progressTotal = imageIds.size,
            ),
        )
        val items = imageIds.mapNotNull { id ->
            val image = imageDao.getById(id) ?: return@mapNotNull null
            TaskItemEntity(
                taskId = taskId,
                imageId = id,
                sourceUri = image.docUri,
                state = TaskItemState.PENDING.name,
            )
        }
        if (items.isEmpty()) {
            taskDao.updateProgress(
                taskId,
                TaskState.FAILED.name,
                0,
                0,
                "没有可删除的文件",
                System.currentTimeMillis(),
            )
            return taskId
        }
        taskDao.insertItems(items)
        val request = OneTimeWorkRequestBuilder<DeleteWorker>()
            .setInputData(workDataOf(DeleteWorker.KEY_TASK_ID to taskId))
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            DeleteWorker.UNIQUE_PREFIX + taskId,
            ExistingWorkPolicy.KEEP,
            request,
        )
        return taskId
    }
}
