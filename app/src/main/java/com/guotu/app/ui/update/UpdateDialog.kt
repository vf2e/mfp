package com.guotu.app.ui.update

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.guotu.app.R
import com.guotu.app.core.util.ApkInstaller
import com.guotu.app.data.update.DownloadState
import com.guotu.app.ui.theme.GuotuColors

@Composable
fun AppUpdateHost(
    uiState: AppUpdateUiState,
    onDismiss: () -> Unit,
    onDownload: () -> Unit,
    onClearStatus: () -> Unit,
    onOpenDialogFromIntent: () -> Unit,
    openFromIntent: Boolean,
) {
    val context = LocalContext.current

    val notifPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* best-effort */ }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    LaunchedEffect(openFromIntent) {
        if (openFromIntent) onOpenDialogFromIntent()
    }

    LaunchedEffect(uiState.statusMessage) {
        // status shown via dialog/snack — cleared by parent snackbar host if needed
        if (uiState.statusMessage != null && !uiState.showDialog) {
            // keep until parent consumes
        }
    }

    if (uiState.showDialog && uiState.available != null) {
        val update = uiState.available
        val download = uiState.download
        AlertDialog(
            onDismissRequest = {
                if (download !is DownloadState.Progress) onDismiss()
            },
            title = {
                Text(stringResource(R.string.update_dialog_title, update.versionName))
            },
            text = {
                Column {
                    if (update.notes.isNotBlank()) {
                        Text(text = update.notes)
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                    when (download) {
                        is DownloadState.Progress -> {
                            Text(stringResource(R.string.update_downloading, download.percent))
                            Spacer(modifier = Modifier.height(10.dp))
                            LinearProgressIndicator(
                                progress = { download.percent / 100f },
                                modifier = Modifier.fillMaxWidth(),
                                color = GuotuColors.Signal,
                                trackColor = GuotuColors.Rule,
                            )
                        }
                        is DownloadState.Failed -> {
                            Text(
                                text = download.message,
                                color = GuotuColors.Danger,
                            )
                        }
                        is DownloadState.Ready -> {
                            Text(stringResource(R.string.update_ready))
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { 1f },
                                modifier = Modifier.fillMaxWidth(),
                                color = GuotuColors.Signal,
                                trackColor = GuotuColors.Rule,
                            )
                        }
                        DownloadState.Idle -> {
                            Text(stringResource(R.string.update_dialog_body))
                        }
                    }
                }
            },
            confirmButton = {
                when (download) {
                    is DownloadState.Ready -> {
                        TextButton(
                            onClick = {
                                if (!ApkInstaller.canRequestPackageInstalls(context)) {
                                    context.startActivity(
                                        ApkInstaller.installPermissionSettingsIntent(context),
                                    )
                                } else {
                                    ApkInstaller.installApk(context, download.apkFile)
                                }
                            },
                        ) {
                            Text(stringResource(R.string.update_install))
                        }
                    }
                    is DownloadState.Progress -> {
                        TextButton(onClick = {}, enabled = false) {
                            Text(stringResource(R.string.update_downloading_short))
                        }
                    }
                    else -> {
                        TextButton(onClick = onDownload) {
                            Text(
                                if (download is DownloadState.Failed) {
                                    stringResource(R.string.update_retry)
                                } else {
                                    stringResource(R.string.update_download)
                                },
                            )
                        }
                    }
                }
            },
            dismissButton = {
                if (download !is DownloadState.Progress) {
                    TextButton(onClick = {
                        onDismiss()
                        onClearStatus()
                    }) {
                        Text(stringResource(R.string.action_cancel))
                    }
                }
            },
        )
    }
}
