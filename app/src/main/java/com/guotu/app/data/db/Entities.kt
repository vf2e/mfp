package com.guotu.app.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "root_folder")
data class RootFolderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "display_name") val displayName: String,
    @ColumnInfo(name = "tree_uri") val treeUri: String,
    @ColumnInfo(name = "display_path") val displayPath: String,
    @ColumnInfo(name = "last_scanned_at") val lastScannedAt: Long = 0L,
    @ColumnInfo(name = "image_count") val imageCount: Int = 0,
    @ColumnInfo(name = "persist_permission") val persistPermission: Boolean = true,
)

@Entity(
    tableName = "image",
    foreignKeys = [
        ForeignKey(
            entity = RootFolderEntity::class,
            parentColumns = ["id"],
            childColumns = ["root_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["doc_uri"], unique = true),
        Index(value = ["root_id", "date_modified"]),
        Index(value = ["root_id", "parent_sub_path"]),
        Index(value = ["root_id", "mark_status"]),
        Index(value = ["file_name"]),
    ],
)
data class ImageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "root_id") val rootId: Long,
    @ColumnInfo(name = "doc_uri") val docUri: String,
    @ColumnInfo(name = "document_id") val documentId: String? = null,
    @ColumnInfo(name = "media_store_id") val mediaStoreId: Long? = null,
    @ColumnInfo(name = "relative_path") val relativePath: String,
    @ColumnInfo(name = "parent_sub_path") val parentSubPath: String,
    @ColumnInfo(name = "depth") val depth: Int,
    @ColumnInfo(name = "file_name") val fileName: String,
    @ColumnInfo(name = "ext") val ext: String,
    @ColumnInfo(name = "mime") val mime: String? = null,
    @ColumnInfo(name = "size_bytes") val sizeBytes: Long = 0L,
    @ColumnInfo(name = "duration_ms") val durationMs: Long = -1L,
    @ColumnInfo(name = "width") val width: Int = 0,
    @ColumnInfo(name = "height") val height: Int = 0,
    @ColumnInfo(name = "rotation") val rotation: Int = 0,
    @ColumnInfo(name = "image_codec") val imageCodec: String? = null,
    @ColumnInfo(name = "audio_codec") val audioCodec: String? = null,
    @ColumnInfo(name = "container") val container: String? = null,
    @ColumnInfo(name = "date_taken") val dateTaken: Long = 0L,
    @ColumnInfo(name = "date_modified") val dateModified: Long = 0L,
    @ColumnInfo(name = "mark_status") val markStatus: String = "TO_DELETE",
    @ColumnInfo(name = "star") val star: Int = 0,
    @ColumnInfo(name = "color") val color: String? = null,
    @ColumnInfo(name = "note") val note: String? = null,
    @ColumnInfo(name = "is_missing") val isMissing: Boolean = false,
    @ColumnInfo(name = "last_scanned_at") val lastScannedAt: Long = 0L,
    @ColumnInfo(name = "play_engine_hint") val playEngineHint: String? = null,
)

@Entity(tableName = "tag")
data class TagEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val color: String,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "image_tag",
    primaryKeys = ["image_id", "tag_id"],
    foreignKeys = [
        ForeignKey(
            entity = ImageEntity::class,
            parentColumns = ["id"],
            childColumns = ["image_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = TagEntity::class,
            parentColumns = ["id"],
            childColumns = ["tag_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("image_id"),
        Index("tag_id"),
    ],
)
data class ImageTagCrossRef(
    @ColumnInfo(name = "image_id") val imageId: Long,
    @ColumnInfo(name = "tag_id") val tagId: Long,
)

@Entity(tableName = "smart_filter")
data class SmartFilterEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    @ColumnInfo(name = "criteria_json") val criteriaJson: String,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "folder_preset")
data class FolderPresetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val uri: String,
    @ColumnInfo(name = "last_used_at") val lastUsedAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "task")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val state: String,
    @ColumnInfo(name = "target_uri") val targetUri: String? = null,
    @ColumnInfo(name = "conflict_policy") val conflictPolicy: String = "RENAME",
    @ColumnInfo(name = "progress_current") val progressCurrent: Int = 0,
    @ColumnInfo(name = "progress_total") val progressTotal: Int = 0,
    @ColumnInfo(name = "error_summary") val errorSummary: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "finished_at") val finishedAt: Long? = null,
)

@Entity(
    tableName = "task_item",
    foreignKeys = [
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["task_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("task_id")],
)
data class TaskItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "task_id") val taskId: Long,
    @ColumnInfo(name = "image_id") val imageId: Long?,
    @ColumnInfo(name = "source_uri") val sourceUri: String,
    @ColumnInfo(name = "dest_uri") val destUri: String? = null,
    val state: String,
    val error: String? = null,
    @ColumnInfo(name = "bytes_copied") val bytesCopied: Long = 0L,
)

@Entity(tableName = "playback_progress")
data class PlaybackProgressEntity(
    @PrimaryKey @ColumnInfo(name = "image_id") val imageId: Long,
    @ColumnInfo(name = "position_ms") val positionMs: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis(),
)
