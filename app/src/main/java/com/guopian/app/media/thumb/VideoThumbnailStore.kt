package com.guopian.app.media.thumb

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface VideoThumbnailEntryPoint {
    fun videoThumbnailStore(): VideoThumbnailStore
}

@Singleton
class VideoThumbnailStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val dir by lazy { File(context.cacheDir, "video_thumbs").also { it.mkdirs() } }
    private val mutex = Mutex()

    fun cacheFile(videoId: Long): File = File(dir, "$videoId.jpg")

    suspend fun getOrCreate(videoId: Long, uri: Uri): Bitmap? = withContext(Dispatchers.IO) {
        mutex.withLock {
            val file = cacheFile(videoId)
            if (file.exists() && file.length() > 0L) {
                return@withLock BitmapFactory.decodeFile(file.absolutePath)
            }
            extract(uri)?.also { bmp ->
                runCatching {
                    FileOutputStream(file).use { out ->
                        bmp.compress(Bitmap.CompressFormat.JPEG, 78, out)
                    }
                }.onFailure { Timber.w(it, "thumb save failed id=%s", videoId) }
            }
        }
    }

    private fun extract(uri: Uri): Bitmap? {
        val retriever = MediaMetadataRetriever()
        return try {
            if (uri.scheme.equals("file", ignoreCase = true)) {
                val path = uri.path
                if (!path.isNullOrBlank()) retriever.setDataSource(path)
                else retriever.setDataSource(context, uri)
            } else {
                retriever.setDataSource(context, uri)
            }
            val frame = retriever.getFrameAtTime(1_000_000L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                ?: retriever.frameAtTime
            if (frame == null) return null
            scaleDown(frame, maxSide = 480)
        } catch (t: Throwable) {
            Timber.w(t, "thumb extract failed uri=%s", uri)
            null
        } finally {
            runCatching { retriever.release() }
        }
    }

    private fun scaleDown(src: Bitmap, maxSide: Int): Bitmap {
        val w = src.width
        val h = src.height
        val longest = maxOf(w, h)
        if (longest <= maxSide) return src
        val scale = maxSide.toFloat() / longest.toFloat()
        val nw = (w * scale).toInt().coerceAtLeast(1)
        val nh = (h * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(src, nw, nh, true).also {
            if (it !== src) src.recycle()
        }
    }
}
