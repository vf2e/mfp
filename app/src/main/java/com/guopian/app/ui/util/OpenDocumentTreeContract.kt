package com.guopian.app.ui.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.guopian.app.core.util.StorageAccessHelper

/** 带初始目录的文档树选择，便于落到外部存储 / Android。 */
class OpenDocumentTreeContract : ActivityResultContracts.OpenDocumentTree() {
    override fun createIntent(context: Context, input: Uri?): Intent {
        return super.createIntent(context, input).apply {
            val initial = input ?: StorageAccessHelper.initialTreeUri()
            putExtra(DocumentsContract.EXTRA_INITIAL_URI, initial)
            addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                    Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or
                    Intent.FLAG_GRANT_PREFIX_URI_PERMISSION,
            )
        }
    }
}

fun ManagedActivityResultLauncher<Uri?, Uri?>.launchDocumentTreePreferAndroid() {
    launch(StorageAccessHelper.initialTreeUri())
}
