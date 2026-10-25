package com.guotu.app.ui.update

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.guotu.app.data.update.AppUpdateRepository
import com.guotu.app.data.update.AvailableUpdate
import com.guotu.app.data.update.DownloadState
import com.guotu.app.data.update.UpdateCheckResult
import com.guotu.app.data.update.UpdateNotifier
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AppUpdateUiState(
    val checking: Boolean = false,
    val available: AvailableUpdate? = null,
    val showDialog: Boolean = false,
    val download: DownloadState = DownloadState.Idle,
    val statusMessage: String? = null,
)

@HiltViewModel
class AppUpdateViewModel @Inject constructor(
    private val repository: AppUpdateRepository,
    private val notifier: UpdateNotifier,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppUpdateUiState())
    val uiState: StateFlow<AppUpdateUiState> = _uiState.asStateFlow()

    private var autoChecked = false

    fun checkOnLaunch() {
        if (autoChecked) return
        autoChecked = true
        check(silent = true, notify = true, openDialogIfAvailable = false)
    }

    /** 仅检查，有新版本时提示，不直接开始下载。 */
    fun checkManual() {
        check(silent = false, notify = false, openDialogIfAvailable = false)
    }

    /** 已确认有新版本后，打开更新对话框。 */
    fun openUpdateDialog() {
        val available = _uiState.value.available
        if (available != null) {
            _uiState.update {
                it.copy(
                    showDialog = true,
                    statusMessage = null,
                    download = DownloadState.Idle,
                )
            }
        } else {
            check(silent = false, notify = false, openDialogIfAvailable = true)
        }
    }

    fun dismissDialog() {
        _uiState.update {
            it.copy(
                showDialog = false,
                download = DownloadState.Idle,
            )
        }
    }

    fun startDownload() {
        val update = _uiState.value.available ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(download = DownloadState.Progress(0), showDialog = true) }
            val result = repository.downloadApk(update) { pct ->
                _uiState.update { state ->
                    state.copy(download = DownloadState.Progress(pct), showDialog = true)
                }
            }
            result.fold(
                onSuccess = { file ->
                    notifier.cancel()
                    _uiState.update {
                        it.copy(
                            download = DownloadState.Ready(file),
                            showDialog = true,
                        )
                    }
                },
                onFailure = { t ->
                    _uiState.update {
                        it.copy(
                            download = DownloadState.Failed(t.message ?: "下载失败"),
                            showDialog = true,
                        )
                    }
                },
            )
        }
    }

    private fun check(
        silent: Boolean,
        notify: Boolean,
        openDialogIfAvailable: Boolean,
    ) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    checking = true,
                    statusMessage = if (silent) it.statusMessage else null,
                )
            }
            when (val result = repository.checkForUpdate()) {
                is UpdateCheckResult.Available -> {
                    _uiState.update {
                        it.copy(
                            checking = false,
                            available = result.update,
                            showDialog = openDialogIfAvailable,
                            download = if (openDialogIfAvailable) DownloadState.Idle else it.download,
                            statusMessage = if (silent || openDialogIfAvailable) {
                                null
                            } else {
                                "发现新版本 v${result.update.versionName}，可点击更新"
                            },
                        )
                    }
                    if (notify) notifier.notifyUpdateAvailable(result.update)
                }
                UpdateCheckResult.UpToDate -> {
                    _uiState.update {
                        it.copy(
                            checking = false,
                            available = null,
                            showDialog = false,
                            statusMessage = if (silent) null else "已是最新版本",
                        )
                    }
                }
                is UpdateCheckResult.Failed -> {
                    _uiState.update {
                        it.copy(
                            checking = false,
                            statusMessage = if (silent) null else result.message,
                        )
                    }
                }
            }
        }
    }

    fun clearStatusMessage() {
        _uiState.update { it.copy(statusMessage = null) }
    }
}
