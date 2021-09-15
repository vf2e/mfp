package com.guopian.app.di

import android.content.Context
import androidx.room.Room
import com.guopian.app.data.db.FolderPresetDao
import com.guopian.app.data.db.GuopianDatabase
import com.guopian.app.data.db.PlaybackProgressDao
import com.guopian.app.data.db.RootFolderDao
import com.guopian.app.data.db.SmartFilterDao
import com.guopian.app.data.db.TagDao
import com.guopian.app.data.db.TaskDao
import com.guopian.app.data.db.VideoDao
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
    fun provideDatabase(@ApplicationContext context: Context): GuopianDatabase {
        return Room.databaseBuilder(
            context,
            GuopianDatabase::class.java,
            "guopian.db",
        ).fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideRootFolderDao(db: GuopianDatabase): RootFolderDao = db.rootFolderDao()

    @Provides
    fun provideVideoDao(db: GuopianDatabase): VideoDao = db.videoDao()

    @Provides
    fun provideTagDao(db: GuopianDatabase): TagDao = db.tagDao()

    @Provides
    fun provideTaskDao(db: GuopianDatabase): TaskDao = db.taskDao()

    @Provides
    fun provideSmartFilterDao(db: GuopianDatabase): SmartFilterDao = db.smartFilterDao()

    @Provides
    fun providePlaybackProgressDao(db: GuopianDatabase): PlaybackProgressDao = db.playbackProgressDao()

    @Provides
    fun provideFolderPresetDao(db: GuopianDatabase): FolderPresetDao = db.folderPresetDao()
}
