package com.guopian.app.media.vlc

import android.content.Context
import android.net.Uri
import android.view.SurfaceView
import com.guopian.app.media.engine.EngineId
import com.guopian.app.media.engine.MediaRef
import com.guopian.app.media.engine.PlayerEngine
import com.guopian.app.media.engine.PlayerEvent
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.interfaces.IVLCVout
import timber.log.Timber

class VlcPlayerEngine(
    context: Context,
    private val libVlcHolder: LibVlcHolder,
) : PlayerEngine {

    override val engineId: EngineId = EngineId.VLC

    private val appContext = context.applicationContext
    private var mediaPlayer: MediaPlayer? = null
    private var attachedSurface: SurfaceView? = null
    private var currentUri: Uri? = null
    private var reachedEnd: Boolean = false

    private val _events = MutableSharedFlow<PlayerEvent>(extraBufferCapacity = 32)
    override val events: SharedFlow<PlayerEvent> = _events.asSharedFlow()

    private val eventListener = MediaPlayer.EventListener { event ->
        when (event.type) {
            MediaPlayer.Event.Opening, MediaPlayer.Event.Buffering ->
                _events.tryEmit(PlayerEvent.Buffering)
            MediaPlayer.Event.Playing -> {
                reachedEnd = false
                _events.tryEmit(PlayerEvent.Ready)
                _events.tryEmit(PlayerEvent.Playing(true))
                emitVideoSizeIfKnown()
            }
            MediaPlayer.Event.Paused -> _events.tryEmit(PlayerEvent.Playing(false))
            MediaPlayer.Event.Vout -> emitVideoSizeIfKnown()
            MediaPlayer.Event.EndReached -> {
                reachedEnd = true
                _events.tryEmit(PlayerEvent.Ended)
            }
            MediaPlayer.Event.EncounteredError ->
                _events.tryEmit(PlayerEvent.Error("VLC playback failed"))
            else -> Unit
        }
    }

    private fun emitVideoSizeIfKnown() {
        val track = mediaPlayer?.currentVideoTrack ?: return
        val w = track.width
        val h = track.height
        if (w <= 0 || h <= 0) return
        _events.tryEmit(
            PlayerEvent.VideoSize(
                width = w,
                height = h,
                rotationDegrees = track.orientation,
            ),
        )
    }

    private fun ensurePlayer(): MediaPlayer {
        mediaPlayer?.let { return it }
        val lib = libVlcHolder.get(appContext)
        return MediaPlayer(lib).also { player ->
            player.setEventListener(eventListener)
            // 0 = 按窗口等比适应，不强制铺满拉伸
            player.scale = 0f
            player.aspectRatio = null
            mediaPlayer = player
            attachedSurface?.let { attachInternal(it, player) }
        }
    }

    private fun attachInternal(surfaceView: SurfaceView, player: MediaPlayer) {
        try {
            val vout: IVLCVout = player.vlcVout
            vout.detachViews()
            vout.setVideoView(surfaceView)
            vout.attachViews()
            surfaceView.addOnLayoutChangeListener { v, _, _, _, _, _, _, _, _ ->
                if (v.width > 0 && v.height > 0) {
                    runCatching { player.vlcVout.setWindowSize(v.width, v.height) }
                }
            }
            if (surfaceView.width > 0 && surfaceView.height > 0) {
                vout.setWindowSize(surfaceView.width, surfaceView.height)
            }
        } catch (t: Throwable) {
            Timber.e(t, "VLC attachSurface failed")
            _events.tryEmit(PlayerEvent.Error("VLC surface attach failed", t))
        }
    }

    override fun attachSurface(surfaceView: SurfaceView) {
        attachedSurface = surfaceView
        mediaPlayer?.let { attachInternal(surfaceView, it) }
    }

    override fun detachSurface() {
        try {
            mediaPlayer?.vlcVout?.detachViews()
        } catch (_: Throwable) {
        }
        attachedSurface = null
    }

    override fun load(media: MediaRef, startPositionMs: Long) {
        val player = ensurePlayer()
        currentUri = media.uri
        reachedEnd = false
        try {
            val lib = libVlcHolder.get(appContext)
            val vlcMedia = Media(lib, media.uri)
            player.media = vlcMedia
            vlcMedia.release()
            if (startPositionMs > 0) {
                player.time = startPositionMs
            }
            _events.tryEmit(PlayerEvent.Ready)
        } catch (t: Throwable) {
            Timber.e(t, "VLC load failed")
            _events.tryEmit(PlayerEvent.Error("VLC load failed", t))
        }
    }

    override fun play() {
        val player = ensurePlayer()
        // EndReached 后需先回起点再 play，否则无效
        if (reachedEnd) {
            reachedEnd = false
            player.time = 0L
        }
        player.play()
    }

    override fun pause() {
        mediaPlayer?.pause()
    }

    override fun seekTo(positionMs: Long) {
        reachedEnd = false
        mediaPlayer?.time = positionMs
    }

    override fun setSpeed(speed: Float) {
        mediaPlayer?.rate = speed
    }

    override fun isPlaying(): Boolean = mediaPlayer?.isPlaying == true

    override fun getPositionMs(): Long = mediaPlayer?.time?.coerceAtLeast(0L) ?: 0L

    override fun getDurationMs(): Long = mediaPlayer?.length?.takeIf { it > 0 } ?: 0L

    override fun release() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.vlcVout?.detachViews()
            mediaPlayer?.setEventListener(null)
            mediaPlayer?.release()
        } catch (t: Throwable) {
            Timber.w(t, "VLC release")
        }
        mediaPlayer = null
        attachedSurface = null
    }
}
