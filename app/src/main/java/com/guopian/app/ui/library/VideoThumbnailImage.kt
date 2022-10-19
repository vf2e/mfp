package com.guopian.app.ui.library

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import com.guopian.app.media.thumb.VideoThumbnailEntryPoint
import dagger.hilt.android.EntryPointAccessors

@Composable
fun VideoThumbnailImage(
    videoId: Long,
    docUri: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    onLoaded: (Boolean) -> Unit = {},
) {
    val context = LocalContext.current
    val store = remember {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            VideoThumbnailEntryPoint::class.java,
        ).videoThumbnailStore()
    }
    var bitmap by remember(videoId) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(videoId, docUri) {
        bitmap = null
        onLoaded(false)
        val bmp = store.getOrCreate(videoId, Uri.parse(docUri))
        bitmap = bmp
        onLoaded(bmp != null)
    }
    val bmp = bitmap
    if (bmp != null && !bmp.isRecycled) {
        Image(
            bitmap = bmp.asImageBitmap(),
            contentDescription = contentDescription,
            modifier = modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
    }
}
