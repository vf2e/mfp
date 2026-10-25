package com.guotu.app.ui.media

import android.content.Context
import android.net.Uri
import android.os.Build
import coil.ImageLoader
import coil.decode.ImageDecoderDecoder
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.size.Precision
import coil.size.Scale

object GuotuImages {

    fun createImageLoader(context: Context): ImageLoader {
        val app = context.applicationContext
        return ImageLoader.Builder(app)
            .crossfade(120)
            .respectCacheHeaders(false)
            .memoryCache {
                MemoryCache.Builder(app)
                    .maxSizePercent(0.28)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(app.cacheDir.resolve("coil_image_cache"))
                    .maxSizeBytes(512L * 1024L * 1024L)
                    .build()
            }
            .components {
                // HEIC / HEIF / animated WebP：系统 ImageDecoder（API 28+）
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    add(ImageDecoderDecoder.Factory())
                }
            }
            .build()
    }

    fun thumbnailRequest(
        context: Context,
        data: Any,
        widthPx: Int,
        heightPx: Int,
    ): ImageRequest {
        return ImageRequest.Builder(context)
            .data(coerceData(data))
            .size(widthPx.coerceAtLeast(1), heightPx.coerceAtLeast(1))
            .scale(Scale.FILL)
            .precision(Precision.INEXACT)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .allowHardware(true)
            .build()
    }

    /**
     * 全屏看图：按屏幕像素解码，避免超大 JPEG 硬件纹理超限变黑、以及 HEIC 全幅过慢。
     */
    fun viewerRequest(
        context: Context,
        data: Any,
        widthPx: Int,
        heightPx: Int,
    ): ImageRequest {
        val w = widthPx.coerceIn(1, 4096)
        val h = heightPx.coerceIn(1, 4096)
        return ImageRequest.Builder(context)
            .data(coerceData(data))
            .size(w, h)
            .scale(Scale.FIT)
            .precision(Precision.INEXACT)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            // 超大图走硬件位图易超过 GL max texture → 全黑
            .allowHardware(false)
            .build()
    }

    private fun coerceData(data: Any): Any {
        if (data is String) {
            return runCatching { Uri.parse(data) }.getOrDefault(data)
        }
        return data
    }
}
