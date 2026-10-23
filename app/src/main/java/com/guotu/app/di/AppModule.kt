package com.guotu.app.di

import android.content.Context
import androidx.room.Room
import com.guotu.app.data.db.FolderPresetDao
import com.guotu.app.data.db.GuotuDatabase
import com.guotu.app.data.db.PlaybackProgressDao
import com.guotu.app.data.db.RootFolderDao
import com.guotu.app.data.db.SmartFilterDao
import com.guotu.app.data.db.TagDao
import com.guotu.app.data.db.TaskDao
import com.guotu.app.data.db.ImageDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): GuotuDatabase {
        return Room.databaseBuilder(
            context,
            GuotuDatabase::class.java,
            "guotu.db",
        ).fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideRootFolderDao(db: GuotuDatabase): RootFolderDao = db.rootFolderDao()

    @Provides
    fun provideImageDao(db: GuotuDatabase): ImageDao = db.imageDao()

    @Provides
    fun provideTagDao(db: GuotuDatabase): TagDao = db.tagDao()

    @Provides
    fun provideTaskDao(db: GuotuDatabase): TaskDao = db.taskDao()

    @Provides
    fun provideSmartFilterDao(db: GuotuDatabase): SmartFilterDao = db.smartFilterDao()

    @Provides
    fun providePlaybackProgressDao(db: GuotuDatabase): PlaybackProgressDao = db.playbackProgressDao()

    @Provides
    fun provideFolderPresetDao(db: GuotuDatabase): FolderPresetDao = db.folderPresetDao()
}
