package com.guopian.app.media.engine

import android.net.Uri
import android.view.SurfaceView
import kotlinx.coroutines.flow.SharedFlow

enum class EngineId { EXO, VLC, EXTERNAL }

data class MediaRef(
    val videoId: Long,
    val uri: Uri,
    val mimeType: String?,
    val fileName: String,
    val sizeBytes: Long,
    val durationMs: Long,
)

sealed interface PlayerEvent {
    data object Idle : PlayerEvent
    data object Buffering : PlayerEvent
    data object Ready : PlayerEvent
    data class Playing(val playing: Boolean) : PlayerEvent
    data class Position(val positionMs: Long, val durationMs: Long) : PlayerEvent
    /** 编码帧宽高；rotationDegrees 为 90/270 时显示需交换宽高。 */
    data class VideoSize(
        val width: Int,
        val height: Int,
        val rotationDegrees: Int = 0,
    ) : PlayerEvent
    data object Ended : PlayerEvent
    data class Error(val message: String, val cause: Throwable? = null) : PlayerEvent
}

interface PlayerEngine {
    val engineId: EngineId
    val events: SharedFlow<PlayerEvent>
    fun attachSurface(surfaceView: SurfaceView)
    fun detachSurface()
    fun load(media: MediaRef, startPositionMs: Long = 0L)
    fun play()
    fun pause()
    fun seekTo(positionMs: Long)
    fun setSpeed(speed: Float)
    fun isPlaying(): Boolean
    fun getPositionMs(): Long
    fun getDurationMs(): Long
    fun release()
}
