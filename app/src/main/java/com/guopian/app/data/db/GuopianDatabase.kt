package com.guopian.app.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        RootFolderEntity::class,
        VideoEntity::class,
        TagEntity::class,
        VideoTagCrossRef::class,
        SmartFilterEntity::class,
        FolderPresetEntity::class,
        TaskEntity::class,
        TaskItemEntity::class,
        PlaybackProgressEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class GuopianDatabase : RoomDatabase() {
    abstract fun rootFolderDao(): RootFolderDao
    abstract fun videoDao(): VideoDao
    abstract fun tagDao(): TagDao
    abstract fun taskDao(): TaskDao
    abstract fun smartFilterDao(): SmartFilterDao
    abstract fun playbackProgressDao(): PlaybackProgressDao
    abstract fun folderPresetDao(): FolderPresetDao
}
