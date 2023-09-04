package com.guopian.app.media.thumb

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import timber.log.Timber

data class VideoProbeMeta(
    val width: Int,
    val height: Int,
    val rotation: Int,
    val durationMs: Long,
)

/** 轻量探测视频元数据（宽高/旋转/时长），扫描与切页定框共用。 */
object VideoAspectProbe {
    fun probe(context: Context, uri: Uri): VideoProbeMeta? {
        val retriever = MediaMetadataRetriever()
        return try {
            if (uri.scheme.equals("file", ignoreCase = true)) {
                val path = uri.path
                if (path.isNullOrBlank()) return null
                retriever.setDataSource(path)
            } else {
                retriever.setDataSource(context, uri)
            }
            val w = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                ?.toIntOrNull() ?: 0
            val h = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                ?.toIntOrNull() ?: 0
            val rot = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)
                ?.toIntOrNull() ?: 0
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull() ?: -1L
            if (w <= 0 && h <= 0 && duration <= 0L) return null
            VideoProbeMeta(
                width = w.coerceAtLeast(0),
                height = h.coerceAtLeast(0),
                rotation = rot,
                durationMs = duration,
            )
        } catch (t: Throwable) {
            Timber.w(t, "video meta probe failed uri=%s", uri)
            null
        } finally {
            runCatching { retriever.release() }
        }
    }

    fun probeDisplaySize(context: Context, uri: Uri): Triple<Int, Int, Int>? {
        val meta = probe(context, uri) ?: return null
        if (meta.width <= 0 || meta.height <= 0) return null
        return Triple(meta.width, meta.height, meta.rotation)
    }

    fun toDisplayPair(width: Int, height: Int, rotationDegrees: Int): Pair<Int, Int> {
        if (width <= 0 || height <= 0) return 0 to 0
        return if (rotationDegrees % 180 != 0) height to width else width to height
    }
}
