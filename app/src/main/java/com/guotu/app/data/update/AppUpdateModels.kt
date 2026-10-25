package com.guotu.app.data.update

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LatestUpdateInfo(
    @SerialName("versionCode") val versionCode: Int,
    @SerialName("versionName") val versionName: String,
    @SerialName("apkFileName") val apkFileName: String,
    @SerialName("notes") val notes: String = "",
    @SerialName("minSupportedCode") val minSupportedCode: Int = 1,
)

data class AvailableUpdate(
    val versionCode: Int,
    val versionName: String,
    val notes: String,
    val apkUrl: String,
    val apkFileName: String,
)

sealed class UpdateCheckResult {
    data object UpToDate : UpdateCheckResult()
    data class Available(val update: AvailableUpdate) : UpdateCheckResult()
    data class Failed(val message: String) : UpdateCheckResult()
}

sealed class DownloadState {
    data object Idle : DownloadState()
    data class Progress(val percent: Int) : DownloadState()
    data class Ready(val apkFile: java.io.File) : DownloadState()
    data class Failed(val message: String) : DownloadState()
}
