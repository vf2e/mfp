package com.guotu.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM task ORDER BY created_at DESC")
    fun observeAll(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM task WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): TaskEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: TaskEntity): Long

    @Update
    suspend fun update(task: TaskEntity)

    @Query(
        """
        UPDATE task SET state = :state, progress_current = :current, progress_total = :total,
        error_summary = :error, finished_at = :finishedAt WHERE id = :id
        """,
    )
    suspend fun updateProgress(
        id: Long,
        state: String,
        current: Int,
        total: Int,
        error: String?,
        finishedAt: Long?,
    )

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<TaskItemEntity>)

    @Query("SELECT * FROM task_item WHERE task_id = :taskId ORDER BY id ASC")
    suspend fun getItems(taskId: Long): List<TaskItemEntity>

    @Update
    suspend fun updateItem(item: TaskItemEntity)

    @Query("DELETE FROM task WHERE state IN ('SUCCESS','FAILED','CANCELLED')")
    suspend fun clearFinished()

    @Query("DELETE FROM task")
    suspend fun clearAll()
}

@Dao
interface FolderPresetDao {
    @Query("SELECT * FROM folder_preset ORDER BY last_used_at DESC LIMIT 20")
    fun observeRecent(): Flow<List<FolderPresetEntity>>

    @Query("SELECT * FROM folder_preset WHERE uri = :uri LIMIT 1")
    suspend fun getByUri(uri: String): FolderPresetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(preset: FolderPresetEntity): Long
}
