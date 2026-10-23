package com.guotu.app.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        RootFolderEntity::class,
        ImageEntity::class,
        TagEntity::class,
        ImageTagCrossRef::class,
        SmartFilterEntity::class,
        FolderPresetEntity::class,
        TaskEntity::class,
        TaskItemEntity::class,
        PlaybackProgressEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class GuotuDatabase : RoomDatabase() {
    abstract fun rootFolderDao(): RootFolderDao
    abstract fun imageDao(): ImageDao
    abstract fun tagDao(): TagDao
    abstract fun taskDao(): TaskDao
    abstract fun smartFilterDao(): SmartFilterDao
    abstract fun playbackProgressDao(): PlaybackProgressDao
    abstract fun folderPresetDao(): FolderPresetDao
}
