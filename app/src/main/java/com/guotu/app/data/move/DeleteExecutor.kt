package com.guotu.app.data.move

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import com.guotu.app.core.util.StorageAccessHelper
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

data class DeleteResult(
    val success: Boolean,
    val error: String? = null,
)

@Singleton
class DeleteExecutor @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun deleteOne(sourceUri: Uri): DeleteResult {
        return try {
            when {
                StorageAccessHelper.isFileRootUri(sourceUri) -> {
                    val path = sourceUri.path
                    if (path.isNullOrBlank()) {
                        return DeleteResult(false, "empty_path")
                    }
                    val file = File(path)
                    if (!file.exists()) return DeleteResult(true)
                    if (file.delete()) {
                        DeleteResult(true)
                    } else {
                        DeleteResult(false, "file_delete_failed")
                    }
                }
                else -> {
                    val ok = DocumentsContract.deleteDocument(context.contentResolver, sourceUri)
                    if (ok) {
                        DeleteResult(true)
                    } else {
                        DeleteResult(false, "doc_delete_failed")
                    }
                }
            }
        } catch (t: Throwable) {
            Timber.w(t, "delete failed uri=%s", sourceUri)
            DeleteResult(false, t.message ?: "delete_error")
        }
    }
}
