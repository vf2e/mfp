package com.guopian.app.media.router

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.guopian.app.media.engine.EngineId
import com.guopian.app.media.engine.MediaRef
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FailoverTracker @Inject constructor() {
    private val exoFailed = mutableSetOf<Long>()

    fun markExoFailed(videoId: Long) {
        exoFailed += videoId
    }

    fun shouldSkipExo(videoId: Long): Boolean = videoId in exoFailed

    fun clear() {
        exoFailed.clear()
    }
}

@Singleton
class ExternalPlayerLauncher @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun open(media: MediaRef): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(media.uri, media.mimeType ?: "video/*")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }
}

data class EngineChoice(val prefer: EngineId)
