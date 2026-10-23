package com.guotu.app.ui.library

import android.net.Uri
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest

@Composable
fun ImageThumbnail(
    imageId: Long,
    docUri: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    onLoaded: (Boolean) -> Unit = {},
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    BoxWithConstraints(modifier = modifier) {
        val widthPx = remember(maxWidth, maxHeight) {
            with(density) { maxWidth.roundToPx().coerceAtLeast(1) }
        }
        val heightPx = remember(maxWidth, maxHeight) {
            with(density) { maxHeight.roundToPx().coerceAtLeast(1) }
        }
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(Uri.parse(docUri))
                .size(widthPx, heightPx)
                .crossfade(true)
                .listener(
                    onSuccess = { _, _ -> onLoaded(true) },
                    onError = { _, _ -> onLoaded(false) },
                )
                .build(),
            contentDescription = contentDescription,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
    }
}
