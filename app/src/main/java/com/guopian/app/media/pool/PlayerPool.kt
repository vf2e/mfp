package com.guopian.app.media.pool

import android.content.Context
import android.view.SurfaceView
import com.guopian.app.media.engine.EngineId
import com.guopian.app.media.engine.MediaRef
import com.guopian.app.media.engine.PlayerEngine
import com.guopian.app.media.exo.ExoPlayerEngine
import com.guopian.app.media.vlc.LibVlcHolder
import com.guopian.app.media.vlc.VlcPlayerEngine
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

@Singleton
class PlayerPool @Inject constructor(
    @ApplicationContext private val context: Context,
    private val libVlcHolder: LibVlcHolder,
) {
    private val map = linkedMapOf<Int, PlayerEngine>()
    private var vlcIndex: Int? = null

    fun get(index: Int): PlayerEngine? = map[index]

    /** 仅返回已有引擎，禁止在 UI 侧偷偷新建（避免快滑时解码器爆炸）。 */
    fun getOrNull(index: Int): PlayerEngine? = map[index]

    /**
     * Preload / default path: Exo only.
     * Hard-cap players to avoid MediaCodec OOM when flinging a long playlist.
     */
    fun acquireExo(index: Int, pivotIndex: Int = index): PlayerEngine {
        val existing = map[index]
        if (existing != null && existing.engineId == EngineId.EXO) return existing
        existing?.release()
        if (vlcIndex == index) vlcIndex = null
        evictUntilRoom(pivotIndex, keep = index)
        val engine = ExoPlayerEngine(context)
        map[index] = engine
        return engine
    }

    /**
     * Replace current page engine with VLC. Ensures at most one VLC instance.
     */
    fun failoverToVlc(
        index: Int,
        media: MediaRef,
        surface: SurfaceView?,
        startPositionMs: Long = 0L,
    ): PlayerEngine {
        vlcIndex?.let { old ->
            if (old != index) {
                map.remove(old)?.release()
            }
        }
        map.remove(index)?.release()
        evictUntilRoom(index, keep = index)
        val engine = VlcPlayerEngine(context, libVlcHolder)
        map[index] = engine
        vlcIndex = index
        surface?.let { engine.attachSurface(it) }
        engine.load(media, startPositionMs.coerceAtLeast(0L))
        engine.play()
        return engine
    }

    fun retainOnly(keep: Set<Int>) {
        val toRemove = map.keys.filter { it !in keep }.toList()
        toRemove.forEach { idx ->
            map.remove(idx)?.release()
            if (vlcIndex == idx) vlcIndex = null
        }
    }

    fun releaseAll() {
        map.values.forEach { it.release() }
        map.clear()
        vlcIndex = null
    }

    private fun evictUntilRoom(pivotIndex: Int, keep: Int) {
        while (map.size >= MAX_PLAYERS && keep !in map) {
            val victim = map.keys
                .filter { it != keep }
                .maxByOrNull { abs(it - pivotIndex) }
                ?: break
            map.remove(victim)?.release()
            if (vlcIndex == victim) vlcIndex = null
        }
        // Also trim if already at cap but keep is present and we're replacing
        while (map.size > MAX_PLAYERS) {
            val victim = map.keys
                .filter { it != keep }
                .maxByOrNull { abs(it - pivotIndex) }
                ?: break
            map.remove(victim)?.release()
            if (vlcIndex == victim) vlcIndex = null
        }
    }

    companion object {
        /** Current ±1 is enough; more ExoPlayers → MediaCodec / OOM crashes on long lists. */
        const val MAX_PLAYERS = 3
    }
}
