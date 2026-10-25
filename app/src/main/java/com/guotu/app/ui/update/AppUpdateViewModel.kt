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
import java.io.File
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
        check(silent = true, notify = true)
    }

    fun checkManual() {
        check(silent = false, notify = false)
    }

    fun openUpdateDialog() {
        if (_uiState.value.available != null) {
            _uiState.update { it.copy(showDialog = true, statusMessage = null) }
        } else {
            check(silent = false, notify = false)
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
            _uiState.update { it.copy(download = DownloadState.Progress(0)) }
            val result = repository.downloadApk(update) { pct ->
                _uiState.update { state ->
                    state.copy(download = DownloadState.Progress(pct))
                }
            }
            result.fold(
                onSuccess = { file ->
                    notifier.cancel()
                    _uiState.update { it.copy(download = DownloadState.Ready(file)) }
                },
                onFailure = { t ->
                    _uiState.update {
                        it.copy(download = DownloadState.Failed(t.message ?: "下载失败"))
                    }
                },
            )
        }
    }

    fun consumeReadyApk(): File? {
        val ready = _uiState.value.download as? DownloadState.Ready ?: return null
        return ready.apkFile
    }

    private fun check(silent: Boolean, notify: Boolean) {
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
                            showDialog = true,
                            statusMessage = null,
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
