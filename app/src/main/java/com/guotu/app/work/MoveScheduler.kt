package com.guotu.app.work

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.guotu.app.data.db.TaskDao
import com.guotu.app.data.db.TaskEntity
import com.guotu.app.data.db.TaskItemEntity
import com.guotu.app.data.db.ImageDao
import com.guotu.app.domain.model.ConflictPolicy
import com.guotu.app.domain.model.TaskItemState
import com.guotu.app.domain.model.TaskState
import com.guotu.app.domain.model.TaskType
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MoveScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val taskDao: TaskDao,
    private val imageDao: ImageDao,
) {
    suspend fun enqueueMove(
        imageIds: List<Long>,
        targetTreeUri: Uri,
        policy: ConflictPolicy = ConflictPolicy.RENAME,
    ): Long {
        require(imageIds.isNotEmpty())

        try {
            context.contentResolver.takePersistableUriPermission(
                targetTreeUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            )
        } catch (_: SecurityException) {
        }

        val taskId = taskDao.insert(
            TaskEntity(
                type = TaskType.MOVE.name,
                state = TaskState.PENDING.name,
                targetUri = targetTreeUri.toString(),
                conflictPolicy = policy.name,
                progressCurrent = 0,
                progressTotal = imageIds.size,
            ),
        )

        val items = imageIds.mapNotNull { id ->
            val v = imageDao.getById(id) ?: return@mapNotNull null
            TaskItemEntity(
                taskId = taskId,
                imageId = id,
                sourceUri = v.docUri,
                state = TaskItemState.PENDING.name,
            )
        }
        taskDao.insertItems(items)

        val request = OneTimeWorkRequestBuilder<MoveWorker>()
            .setInputData(workDataOf(MoveWorker.KEY_TASK_ID to taskId))
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            MoveWorker.UNIQUE_PREFIX + taskId,
            ExistingWorkPolicy.KEEP,
            request,
        )
        return taskId
    }
}
