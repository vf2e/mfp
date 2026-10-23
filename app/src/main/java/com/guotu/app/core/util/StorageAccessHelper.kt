package com.guotu.app.core.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.Settings

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

    fun primaryStorageDir(): java.io.File = Environment.getExternalStorageDirectory()

    fun androidDir(): java.io.File = java.io.File(primaryStorageDir(), "Android")

    fun shortcutDirs(): List<Pair<String, java.io.File>> {
        val root = primaryStorageDir()
        return listOf(
            "内部存储" to root,
            "Android" to java.io.File(root, "Android"),
            "Pictures" to java.io.File(root, "Pictures"),
            "DCIM" to java.io.File(root, "DCIM"),
            "Download" to java.io.File(root, "Download"),
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
            abs.startsWith("$root/") || abs.startsWith("$root\\") ->
                abs.removePrefix(root).trimStart('/', '\\')
            else -> abs
        }
    }

    /** 打开系统目录选择器时尽量落到外部存储 / Pictures 附近。 */
    fun initialTreeUri(): Uri {
        return DocumentsContract.buildDocumentUri(
            "com.android.externalstorage.documents",
            "primary:Pictures",
        )
    }
}
