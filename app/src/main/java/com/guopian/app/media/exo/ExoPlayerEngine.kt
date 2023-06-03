package com.guopian.app.media.exo

import android.content.Context
import android.net.Uri
import android.view.SurfaceView
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.SeekParameters
import com.guopian.app.media.engine.MediaRef
import com.guopian.app.media.engine.PlayerEngine
import com.guopian.app.media.engine.PlayerEvent
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class ExoPlayerEngine(
    context: Context,
) : PlayerEngine {

    override val engineId = com.guopian.app.media.engine.EngineId.EXO

    private val appContext = context.applicationContext
    private var player: ExoPlayer? = null
    private var attachedSurface: SurfaceView? = null
    private var loadedUri: Uri? = null

    private val _events = MutableSharedFlow<PlayerEvent>(extraBufferCapacity = 32)
    override val events: SharedFlow<PlayerEvent> = _events.asSharedFlow()

    private val listener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                Player.STATE_IDLE -> _events.tryEmit(PlayerEvent.Idle)
                Player.STATE_BUFFERING -> _events.tryEmit(PlayerEvent.Buffering)
                Player.STATE_READY -> {
                    _events.tryEmit(PlayerEvent.Ready)
                    val p = player ?: return
                    _events.tryEmit(PlayerEvent.Playing(p.isPlaying))
                    emitVideoSize(p.videoSize)
                }
                Player.STATE_ENDED -> _events.tryEmit(PlayerEvent.Ended)
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _events.tryEmit(PlayerEvent.Playing(isPlaying))
        }

        override fun onVideoSizeChanged(videoSize: VideoSize) {
            emitVideoSize(videoSize)
        }

        override fun onPlayerError(error: PlaybackException) {
            _events.tryEmit(PlayerEvent.Error(error.message ?: "playback error", error))
        }
    }

    private fun emitVideoSize(videoSize: VideoSize) {
        if (videoSize.width <= 0 || videoSize.height <= 0) return
        _events.tryEmit(
            PlayerEvent.VideoSize(
                width = videoSize.width,
                height = videoSize.height,
                rotationDegrees = 0,
            ),
        )
    }

    private fun ensurePlayer(): ExoPlayer {
        val existing = player
        if (existing != null) return existing
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                /* minBufferMs */ 2_500,
                /* maxBufferMs */ 10_000,
                /* bufferForPlaybackMs */ 750,
                /* bufferForPlaybackAfterRebufferMs */ 1_500,
            )
            .build()
        return ExoPlayer.Builder(appContext)
            .setLoadControl(loadControl)
            .build()
            .also {
                it.addListener(listener)
                it.videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT
                it.setSeekParameters(SeekParameters.CLOSEST_SYNC)
                player = it
                attachedSurface?.let { surface -> it.setVideoSurfaceView(surface) }
            }
    }

    override fun attachSurface(surfaceView: SurfaceView) {
        if (attachedSurface === surfaceView && player != null) return
        attachedSurface = surfaceView
        player?.let {
            it.videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT
            it.setVideoSurfaceView(surfaceView)
        }
    }

    override fun detachSurface() {
        player?.clearVideoSurface()
        attachedSurface = null
    }

    override fun load(media: MediaRef, startPositionMs: Long) {
        val p = ensurePlayer()
        if (loadedUri == media.uri && p.playbackState != Player.STATE_IDLE) {
            if (startPositionMs > 0L && kotlin.math.abs(p.currentPosition - startPositionMs) > 400L) {
                p.seekTo(startPositionMs)
            }
            return
        }
        loadedUri = media.uri
        p.setMediaItem(MediaItem.fromUri(media.uri))
        p.prepare()
        if (startPositionMs > 0) {
            p.seekTo(startPositionMs)
        }
    }

    override fun play() {
        val p = ensurePlayer()
        if (p.playbackState == Player.STATE_ENDED) {
            p.seekTo(0)
        }
        p.play()
    }

    override fun pause() {
        player?.pause()
    }

    override fun seekTo(positionMs: Long) {
        val p = player ?: return
        val dur = p.duration
        val target = if (dur > 0) {
            positionMs.coerceIn(0L, dur)
        } else {
            positionMs.coerceAtLeast(0L)
        }
        p.setSeekParameters(SeekParameters.CLOSEST_SYNC)
        p.seekTo(target)
    }

    /** 松手时更精确地落到目标点。 */
    fun seekExact(positionMs: Long) {
        val p = player ?: return
        val dur = p.duration
        val target = if (dur > 0) {
            positionMs.coerceIn(0L, dur)
        } else {
            positionMs.coerceAtLeast(0L)
        }
        p.setSeekParameters(SeekParameters.EXACT)
        p.seekTo(target)
        p.setSeekParameters(SeekParameters.CLOSEST_SYNC)
    }

    override fun setSpeed(speed: Float) {
        player?.setPlaybackSpeed(speed)
    }

    fun currentPosition(): Long = player?.currentPosition ?: 0L

    fun duration(): Long = player?.duration?.takeIf { it > 0 } ?: 0L

    override fun getPositionMs(): Long = currentPosition()

    override fun getDurationMs(): Long = duration()

    override fun isPlaying(): Boolean = player?.isPlaying == true

    override fun release() {
        player?.removeListener(listener)
        player?.release()
        player = null
        attachedSurface = null
        loadedUri = null
    }
}
