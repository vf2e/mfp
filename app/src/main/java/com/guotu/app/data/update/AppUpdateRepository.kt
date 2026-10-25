package com.guotu.app.data.update

import android.content.Context
import com.guotu.app.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppUpdateRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    suspend fun checkForUpdate(): UpdateCheckResult = withContext(Dispatchers.IO) {
        runCatching {
            val raw = httpGetString(LATEST_JSON_URL)
            val info = json.decodeFromString<LatestUpdateInfo>(raw)
            if (info.versionCode <= BuildConfig.VERSION_CODE) {
                UpdateCheckResult.UpToDate
            } else {
                UpdateCheckResult.Available(
                    AvailableUpdate(
                        versionCode = info.versionCode,
                        versionName = info.versionName,
                        notes = info.notes,
                        apkUrl = "$RELEASE_DOWNLOAD_BASE/${info.apkFileName}",
                        apkFileName = info.apkFileName,
                    ),
                )
            }
        }.getOrElse { t ->
            Timber.w(t, "checkForUpdate failed")
            UpdateCheckResult.Failed(t.message ?: "网络错误")
        }
    }

    suspend fun downloadApk(
        update: AvailableUpdate,
        onProgress: (Int) -> Unit,
    ): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val dir = File(context.cacheDir, "updates").apply { mkdirs() }
            dir.listFiles()?.forEach { runCatching { it.delete() } }
            val outFile = File(dir, update.apkFileName)
            httpDownload(update.apkUrl, outFile, onProgress)
            outFile
        }
    }

    private fun httpGetString(url: String): String {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 12_000
            readTimeout = 20_000
            instanceFollowRedirects = true
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "Guotu/${BuildConfig.VERSION_NAME}")
        }
        try {
            val code = conn.responseCode
            if (code !in 200..299) {
                error("HTTP $code")
            }
            return conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }

    private fun httpDownload(url: String, dest: File, onProgress: (Int) -> Unit) {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 120_000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "Guotu/${BuildConfig.VERSION_NAME}")
        }
        try {
            val code = conn.responseCode
            if (code !in 200..299) error("下载失败 HTTP $code")
            val total = conn.contentLengthLong.coerceAtLeast(0L)
            conn.inputStream.use { input ->
                dest.outputStream().use { output ->
                    val buf = ByteArray(64 * 1024)
                    var readTotal = 0L
                    var lastPct = -1
                    while (true) {
                        val n = input.read(buf)
                        if (n <= 0) break
                        output.write(buf, 0, n)
                        readTotal += n
                        if (total > 0L) {
                            val pct = ((readTotal * 100L) / total).toInt().coerceIn(0, 100)
                            if (pct != lastPct) {
                                lastPct = pct
                                onProgress(pct)
                            }
                        }
                    }
                    output.flush()
                }
            }
            onProgress(100)
            if (!dest.exists() || dest.length() < 1024L) {
                error("APK 文件无效")
            }
        } finally {
            conn.disconnect()
        }
    }

    companion object {
        private const val RELEASE_DOWNLOAD_BASE =
            "https://github.com/vf2e/mfp/releases/latest/download"
        const val LATEST_JSON_URL = "$RELEASE_DOWNLOAD_BASE/latest.json"
    }
}
