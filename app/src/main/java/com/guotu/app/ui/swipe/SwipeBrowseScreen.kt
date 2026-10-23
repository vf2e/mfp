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
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.guotu.app.R
import com.guotu.app.ui.cut.CutTargetSheet
import com.guotu.app.ui.util.OpenDocumentTreeContract
import com.guotu.app.ui.util.launchDocumentTreePreferAndroid
import kotlinx.coroutines.flow.distinctUntilChanged

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
                        docUri = viewModel.docUriAt(page),
                        isCurrent = page == epochPager.settledPage,
                        onTap = { viewModel.toggleUi() },
                        onDoubleTap = viewModel::onDoubleTap,
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
                    onBack = onBack,
                    onKeep = viewModel::toggleKeep,
                    onPending = viewModel::togglePending,
                    onCut = viewModel::openCutSheet,
                )
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(bottom = 16.dp),
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

@Composable
private fun ImagePage(
    docUri: String?,
    isCurrent: Boolean,
    onTap: () -> Unit,
    onDoubleTap: () -> Unit,
    onZoomChanged: (Boolean) -> Unit,
) {
    var scale by remember(isCurrent) { mutableFloatStateOf(1f) }
    var offset by remember(isCurrent) { mutableStateOf(Offset.Zero) }

    LaunchedEffect(isCurrent) {
        if (!isCurrent) {
            scale = 1f
            offset = Offset.Zero
            onZoomChanged(false)
        }
    }

    LaunchedEffect(scale, isCurrent) {
        if (isCurrent) {
            onZoomChanged(scale > 1.01f)
        }
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .then(
                if (isCurrent && scale <= 1.01f) {
                    Modifier.pointerInput(isCurrent) {
                        detectTapGestures(
                            onTap = { onTap() },
                            onDoubleTap = { onDoubleTap() },
                        )
                    }
                } else {
                    Modifier
                },
            )
            .then(
                if (isCurrent) {
                    Modifier.transformable(state = transformableState)
                } else {
                    Modifier
                },
            )
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationX = offset.x
                translationY = offset.y
            },
        contentAlignment = Alignment.Center,
    ) {
        if (!docUri.isNullOrBlank()) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(Uri.parse(docUri))
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
            )
        }
    }
}
