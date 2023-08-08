package com.guopian.app.core.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.Settings
import kotlin.math.roundToInt

object VolumeBrightnessController {
    fun getVolumeFraction(context: Context): Float {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        return am.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / max.toFloat()
    }

    /** deltaYPx：上滑为负（屏幕坐标向下为正），上滑应增大音量。 */
    fun adjustVolumeByDrag(context: Context, deltaYPx: Float, viewHeightPx: Float): Float {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        val current = am.getStreamVolume(AudioManager.STREAM_MUSIC)
        val span = viewHeightPx.coerceAtLeast(1f)
        val steps = ((-deltaYPx / span) * max * 1.35f)
        if (kotlin.math.abs(steps) < 0.08f) return current.toFloat() / max
        val next = (current + steps.roundToInt()).coerceIn(0, max)
        if (next != current) {
            am.setStreamVolume(AudioManager.STREAM_MUSIC, next, 0)
        }
        return next.toFloat() / max
    }

    fun getBrightnessFraction(activity: Activity): Float {
        val lp = activity.window.attributes
        val override = lp.screenBrightness
        return if (override >= 0f) {
            override.coerceIn(0.01f, 1f)
        } else {
            runCatching {
                Settings.System.getInt(activity.contentResolver, Settings.System.SCREEN_BRIGHTNESS) / 255f
            }.getOrDefault(0.5f).coerceIn(0.01f, 1f)
        }
    }

    fun adjustBrightnessByDrag(activity: Activity, deltaYPx: Float, viewHeightPx: Float): Float {
        val span = viewHeightPx.coerceAtLeast(1f)
        val current = getBrightnessFraction(activity)
        val next = (current + (-deltaYPx / span) * 1.1f).coerceIn(0.01f, 1f)
        val lp = activity.window.attributes
        lp.screenBrightness = next
        activity.window.attributes = lp
        return next
    }
}

object StorageAccessHelper {
    fun hasAllFilesAccess(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            true
        }
    }

    fun manageAllFilesIntent(context: Context): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                data = Uri.parse("package:${context.packageName}")
            }
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
            }
        }
    }

    fun primaryStorageDir(): java.io.File {
        return Environment.getExternalStorageDirectory()
    }

    fun androidDir(): java.io.File = java.io.File(primaryStorageDir(), "Android")

    fun shortcutDirs(): List<Pair<String, java.io.File>> {
        val root = primaryStorageDir()
        return listOf(
            "内部存储" to root,
            "Android" to java.io.File(root, "Android"),
            "Movies" to java.io.File(root, "Movies"),
            "Download" to java.io.File(root, "Download"),
            "DCIM" to java.io.File(root, "DCIM"),
        ).filter { it.second.exists() }
    }

    fun toFileUri(file: java.io.File): Uri = Uri.fromFile(file)

    fun isFileRootUri(uri: Uri): Boolean =
        uri.scheme.equals("file", ignoreCase = true)

    fun displayPathForFile(file: java.io.File): String {
        val root = primaryStorageDir().absolutePath
        val abs = file.absolutePath
        return when {
            abs == root -> "内部存储"
            abs.startsWith("$root/") -> abs.removePrefix("$root/")
            else -> abs
        }
    }

    /** 打开系统目录选择器时尽量落到外部存储 / Android 附近。 */
    fun initialTreeUri(): Uri {
        return DocumentsContract.buildDocumentUri(
            "com.android.externalstorage.documents",
            "primary:Android",
        )
    }
}
