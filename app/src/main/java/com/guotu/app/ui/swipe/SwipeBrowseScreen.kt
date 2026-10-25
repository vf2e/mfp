package com.guotu.app.ui.swipe

import android.app.Activity
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import com.guotu.app.R
import com.guotu.app.domain.model.MarkStatus
import com.guotu.app.ui.cut.CutTargetSheet
import com.guotu.app.ui.media.GuotuImages
import com.guotu.app.ui.util.OpenDocumentTreeContract
import com.guotu.app.ui.util.launchDocumentTreePreferAndroid
import kotlin.math.abs
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import android.view.KeyEvent
import android.view.View

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SwipeBrowseScreen(
    onBack: () -> Unit,
    viewModel: SwipeBrowseViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val presets by viewModel.folderPresets.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val total = uiState.total.coerceAtLeast(1)
    val activity = LocalContext.current as? Activity
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    var pagerScrollEnabled by remember { mutableStateOf(true) }

    DisposableEffect(Unit) {
        val previousOrientation = activity?.requestedOrientation
            ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            if (previousOrientation != ActivityInfo.SCREEN_ORIENTATION_PORTRAIT &&
                previousOrientation != ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            ) {
                activity?.requestedOrientation = previousOrientation
            }
        }
    }

    DisposableEffect(isLandscape) {
        val window = activity?.window
        val previousNavColor = window?.navigationBarColor
        val previousStatusColor = window?.statusBarColor
        val previousContrast = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window?.isNavigationBarContrastEnforced
        } else {
            null
        }
        val previousStatusContrast = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window?.isStatusBarContrastEnforced
        } else {
            null
        }
        val controller = window?.let { WindowCompat.getInsetsController(it, it.decorView) }
        val previousLightNav = controller?.isAppearanceLightNavigationBars
        val previousLightStatus = controller?.isAppearanceLightStatusBars

        if (window != null) {
            WindowCompat.setDecorFitsSystemWindows(window, false)
            window.statusBarColor = AndroidColor.TRANSPARENT
            window.navigationBarColor = AndroidColor.TRANSPARENT
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                window.isNavigationBarContrastEnforced = false
                window.isStatusBarContrastEnforced = false
            }
        }
        controller?.isAppearanceLightStatusBars = false
        controller?.isAppearanceLightNavigationBars = false
        controller?.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller?.hide(WindowInsetsCompat.Type.navigationBars())
        if (isLandscape) {
            controller?.hide(WindowInsetsCompat.Type.statusBars())
        } else {
            controller?.show(WindowInsetsCompat.Type.statusBars())
        }

        onDispose {
            controller?.show(WindowInsetsCompat.Type.systemBars())
            if (previousNavColor != null) window?.navigationBarColor = previousNavColor
            if (previousStatusColor != null) window?.statusBarColor = previousStatusColor
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                if (previousContrast != null) {
                    window?.isNavigationBarContrastEnforced = previousContrast
                }
                if (previousStatusContrast != null) {
                    window?.isStatusBarContrastEnforced = previousStatusContrast
                }
            }
            if (previousLightNav != null) {
                controller.isAppearanceLightNavigationBars = previousLightNav
            }
            if (previousLightStatus != null) {
                controller.isAppearanceLightStatusBars = previousLightStatus
            }
        }
    }

    val pickCutTarget = rememberLauncherForActivityResult(
        contract = OpenDocumentTreeContract(),
    ) { uri: Uri? ->
        if (uri != null) viewModel.enqueueCut(uri)
    }

    LaunchedEffect(uiState.cutMessage) {
        val msg = uiState.cutMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        viewModel.clearCutMessage()
    }

    LaunchedEffect(uiState.undoHint, uiState.canUndo) {
        val hint = uiState.undoHint ?: return@LaunchedEffect
        val result = snackbarHostState.showSnackbar(
            message = hint,
            actionLabel = if (uiState.canUndo) "撤销" else null,
            duration = SnackbarDuration.Short,
        )
        viewModel.clearUndoHint()
        if (result == SnackbarResult.ActionPerformed) {
            viewModel.undoLastMark()
        }
    }

    LaunchedEffect(uiState.endOfListHint) {
        val hint = uiState.endOfListHint ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(hint, duration = SnackbarDuration.Long)
        viewModel.clearEndOfListHint()
    }

    LaunchedEffect(uiState.swipeFlashEpoch, uiState.swipeFlash) {
        if (uiState.swipeFlash != null) {
            delay(420)
            viewModel.consumeSwipeFlash()
        }
    }

    val view = LocalView.current
    DisposableEffect(view) {
        view.isFocusableInTouchMode = true
        view.requestFocus()
        val listener = View.OnKeyListener { _, keyCode, event ->
            if (event.action != KeyEvent.ACTION_DOWN) return@OnKeyListener false
            when (keyCode) {
                KeyEvent.KEYCODE_VOLUME_UP -> {
                    viewModel.goPrevious()
                    true
                }
                KeyEvent.KEYCODE_VOLUME_DOWN -> {
                    viewModel.goNext()
                    true
                }
                else -> false
            }
        }
        view.setOnKeyListener(listener)
        onDispose { view.setOnKeyListener(null) }
    }

    if (uiState.showCutSheet) {
        CutTargetSheet(
            count = 1,
            presets = presets,
            policy = uiState.conflictPolicy,
            onPolicyChange = viewModel::setConflictPolicy,
            onPickFolder = { pickCutTarget.launchDocumentTreePreferAndroid() },
            onPresetClick = viewModel::enqueueCutPreset,
            onDismiss = viewModel::dismissCutSheet,
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        if (!uiState.ready) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = uiState.error ?: stringResource(R.string.swipe_loading),
                    color = Color.White,
                )
                Spacer(modifier = Modifier.height(12.dp))
                TextButton(onClick = onBack) {
                    Text(text = stringResource(R.string.action_back))
                }
            }
        } else {
            key(uiState.playlistEpoch) {
                val docUris by viewModel.docUris.collectAsStateWithLifecycle()
                val epochPager = rememberPagerState(
                    initialPage = uiState.currentIndex.coerceIn(0, (total - 1).coerceAtLeast(0)),
                    pageCount = { uiState.total.coerceAtLeast(1) },
                )
                LaunchedEffect(epochPager) {
                    snapshotFlow { epochPager.settledPage }
                        .distinctUntilChanged()
                        .collect { page -> viewModel.onPageSettled(page) }
                }
                LaunchedEffect(uiState.pendingScrollIndex, uiState.playlistEpoch) {
                    val target = uiState.pendingScrollIndex ?: return@LaunchedEffect
                    epochPager.scrollToPage(target.coerceIn(0, (uiState.total - 1).coerceAtLeast(0)))
                    viewModel.consumePendingScroll()
                }
                VerticalPager(
                    state = epochPager,
                    modifier = Modifier.fillMaxSize(),
                    beyondViewportPageCount = 1,
                    userScrollEnabled = pagerScrollEnabled,
                    key = { page -> "${uiState.playlistEpoch}-$page" },
                ) { page ->
                    ImagePage(
                        docUri = docUris[page],
                        isCurrent = page == epochPager.settledPage,
                        markGestureEnabled = uiState.swipeMarkEnabled && pagerScrollEnabled,
                        onTap = { viewModel.toggleUi() },
                        onDoubleTap = viewModel::onDoubleTap,
                        onSwipeRight = viewModel::onHorizontalSwipeRight,
                        onSwipeLeft = viewModel::onHorizontalSwipeLeft,
                        onZoomChanged = { zoomed ->
                            if (page == epochPager.settledPage) {
                                pagerScrollEnabled = !zoomed
                            }
                        },
                    )
                }
            }

            KeepBurstOverlay(
                epoch = uiState.keepBurstEpoch,
                kept = uiState.keepBurstKept,
                modifier = Modifier.align(Alignment.Center),
            )

            SwipeFlashOverlay(
                flash = uiState.swipeFlash,
                epoch = uiState.swipeFlashEpoch,
                modifier = Modifier.align(Alignment.Center),
            )

            if (uiState.uiVisible) {
                SwipeIndexBar(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .windowInsetsPadding(WindowInsets.safeDrawing),
                    indexLabel = if (uiState.pageCount > 1) {
                        stringResource(
                            R.string.swipe_page_indicator,
                            uiState.pageIndex + 1,
                            uiState.pageCount,
                            uiState.total,
                            uiState.totalInLibrary,
                        )
                    } else {
                        "${uiState.currentIndex + 1} / ${uiState.total}"
                    },
                )

                SwipeBottomChrome(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .windowInsetsPadding(WindowInsets.safeDrawing),
                    fileName = uiState.image?.fileName.orEmpty(),
                    markStatus = uiState.markStatus,
                    keepBurstEpoch = uiState.keepBurstEpoch,
                    canUndo = uiState.canUndo,
                    onBack = onBack,
                    onKeep = viewModel::toggleKeep,
                    onPending = viewModel::togglePending,
                    onCut = viewModel::openCutSheet,
                    onUndo = viewModel::undoLastMark,
                )
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(bottom = if (uiState.uiVisible) 118.dp else 24.dp),
            )

            val err = uiState.error
            if (err != null) {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(text = err, color = Color(0xFFFFB4A8))
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = onBack) {
                            Text(text = stringResource(R.string.action_back), color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ImagePage(
    docUri: String?,
    isCurrent: Boolean,
    markGestureEnabled: Boolean,
    onTap: () -> Unit,
    onDoubleTap: () -> Unit,
    onSwipeRight: () -> Unit,
    onSwipeLeft: () -> Unit,
    onZoomChanged: (Boolean) -> Unit,
) {
    var scale by remember(docUri) { mutableFloatStateOf(1f) }
    var offset by remember(docUri) { mutableStateOf(Offset.Zero) }
    var dragX by remember { mutableFloatStateOf(0f) }
    val zoomed = scale > 1.01f

    LaunchedEffect(isCurrent) {
        if (!isCurrent) {
            scale = 1f
            offset = Offset.Zero
            dragX = 0f
            onZoomChanged(false)
        }
    }

    LaunchedEffect(zoomed, isCurrent) {
        if (isCurrent) onZoomChanged(zoomed)
    }

    DisposableEffect(isCurrent) {
        onDispose {
            if (isCurrent) onZoomChanged(false)
        }
    }

    val transformableState = rememberTransformableState { zoomChange, panChange, _ ->
        if (!isCurrent) return@rememberTransformableState
        val newScale = (scale * zoomChange).coerceIn(1f, 5f)
        scale = newScale
        if (newScale > 1f) {
            offset += panChange
        } else {
            offset = Offset.Zero
        }
    }

    val context = LocalContext.current
    val density = LocalDensity.current
    val hapticView = LocalView.current
    val canMarkSwipe = isCurrent && markGestureEnabled && !zoomed

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            // 仅放大后启用单指拖移；未放大时 canPan=false，把单指留给 VerticalPager
            .then(
                if (isCurrent) {
                    Modifier.transformable(
                        state = transformableState,
                        canPan = { _ -> zoomed },
                    )
                } else {
                    Modifier
                },
            )
            .then(
                if (isCurrent) {
                    Modifier.pointerInput(docUri, zoomed) {
                        detectTapGestures(
                            onTap = { onTap() },
                            onDoubleTap = {
                                if (zoomed) {
                                    scale = 1f
                                    offset = Offset.Zero
                                } else {
                                    onDoubleTap()
                                }
                            },
                        )
                    }
                } else {
                    Modifier
                },
            )
            .then(
                if (canMarkSwipe) {
                    Modifier.pointerInput(canMarkSwipe, docUri) {
                        val touchSlop = viewConfiguration.touchSlop
                        val threshold = size.width * 0.22f
                        awaitEachGesture {
                            awaitFirstDown(requireUnconsumed = false)
                            var totalX = 0f
                            var totalY = 0f
                            var axis: Int? = null // 1=X, 2=Y
                            dragX = 0f
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull() ?: break
                                if (!change.pressed) break
                                val delta = change.positionChange()
                                totalX += delta.x
                                totalY += delta.y
                                if (axis == null) {
                                    val ax = abs(totalX)
                                    val ay = abs(totalY)
                                    if (ax > touchSlop || ay > touchSlop) {
                                        axis = if (ax > ay) 1 else 2
                                    }
                                }
                                if (axis == 1) {
                                    change.consume()
                                    dragX = totalX
                                }
                                // axis==Y：不 consume，交给 VerticalPager
                            }
                            if (axis == 1) {
                                when {
                                    dragX > threshold -> {
                                        hapticView.performHapticFeedback(
                                            if (Build.VERSION.SDK_INT >= 30) {
                                                android.view.HapticFeedbackConstants.CONFIRM
                                            } else {
                                                android.view.HapticFeedbackConstants.CONTEXT_CLICK
                                            },
                                        )
                                        onSwipeRight()
                                    }
                                    dragX < -threshold -> {
                                        hapticView.performHapticFeedback(
                                            if (Build.VERSION.SDK_INT >= 30) {
                                                android.view.HapticFeedbackConstants.REJECT
                                            } else {
                                                android.view.HapticFeedbackConstants.LONG_PRESS
                                            },
                                        )
                                        onSwipeLeft()
                                    }
                                }
                            }
                            dragX = 0f
                        }
                    }
                } else {
                    Modifier
                },
            )
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationX = offset.x + if (canMarkSwipe) dragX * 0.35f else 0f
                translationY = offset.y
            },
        contentAlignment = Alignment.Center,
    ) {
        if (!docUri.isNullOrBlank()) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val widthPx = with(density) { maxWidth.roundToPx().coerceAtLeast(1) }
                val heightPx = with(density) { maxHeight.roundToPx().coerceAtLeast(1) }
                val request = remember(docUri, widthPx, heightPx) {
                    GuotuImages.viewerRequest(context, docUri, widthPx, heightPx)
                }
                SubcomposeAsyncImage(
                    model = request,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                ) {
                    when (painter.state) {
                        is AsyncImagePainter.State.Loading -> {
                            CircularProgressIndicator(
                                modifier = Modifier.align(Alignment.Center),
                                color = Color.White.copy(alpha = 0.7f),
                                strokeWidth = 2.dp,
                            )
                        }
                        is AsyncImagePainter.State.Error -> {
                            Text(
                                text = "无法显示此图片",
                                color = Color(0xFFFFB4A8),
                                modifier = Modifier.align(Alignment.Center),
                            )
                        }
                        else -> SubcomposeAsyncImageContent()
                    }
                }
            }
        }
    }
}

@Composable
private fun SwipeFlashOverlay(
    flash: MarkStatus?,
    epoch: Int,
    modifier: Modifier = Modifier,
) {
    if (flash == null) return
    key(epoch) {
        val label = when (flash) {
            MarkStatus.KEEP -> "收藏"
            MarkStatus.TO_DELETE, MarkStatus.UNSORTED -> "待删除"
            MarkStatus.PENDING -> "待定"
            MarkStatus.MOVED -> "已剪切"
        }
        val color = when (flash) {
            MarkStatus.KEEP -> Color(0xFFFF2D55)
            else -> Color(0xFFFFB020)
        }
        Box(
            modifier = modifier
                .padding(24.dp)
                .background(Color.Black.copy(alpha = 0.55f))
                .padding(horizontal = 22.dp, vertical = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                color = color,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}
