package com.guotu.app.data.repo

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import com.guotu.app.core.util.StorageAccessHelper
import com.guotu.app.data.db.RootFolderDao
import com.guotu.app.data.db.RootFolderEntity
import com.guotu.app.data.db.ImageDao
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RootFolderRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val rootFolderDao: RootFolderDao,
    private val imageDao: ImageDao,
) {
    fun observeCurrent(): Flow<RootFolderEntity?> = rootFolderDao.observeCurrent()

    suspend fun getCurrent(): RootFolderEntity? = rootFolderDao.getCurrent()

    suspend fun setRootTreeUri(treeUri: Uri, displayName: String? = null) {
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        try {
            context.contentResolver.takePersistableUriPermission(treeUri, flags)
        } catch (_: SecurityException) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    treeUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            } catch (_: SecurityException) {
                // file:// roots or no persistable grant
            }
        }

        val name = displayName
            ?: queryTreeDisplayName(treeUri)
            ?: friendlyTreeLabel(treeUri)
            ?: "根文件夹"

        val pathLabel = friendlyTreeLabel(treeUri) ?: name
        replaceRoot(
            displayName = name,
            treeUri = treeUri.toString(),
            displayPath = pathLabel,
            persistPermission = !StorageAccessHelper.isFileRootUri(treeUri),
        )
    }

    /** 全张文件访问权限下，直接用本地绝对路径作为根目录。 */
    suspend fun setRootFilePath(absolutePath: String) {
        val dir = File(absolutePath)
        require(dir.isDirectory) { "not a directory: $absolutePath" }
        val uri = StorageAccessHelper.toFileUri(dir)
        replaceRoot(
            displayName = dir.name.ifBlank { "内张存储" },
            treeUri = uri.toString(),
            displayPath = StorageAccessHelper.displayPathForFile(dir),
            persistPermission = false,
        )
    }

    private suspend fun replaceRoot(
        displayName: String,
        treeUri: String,
        displayPath: String,
        persistPermission: Boolean,
    ) {
        val existing = rootFolderDao.getCurrent()
        if (existing != null) {
            imageDao.deleteByRoot(existing.id)
            rootFolderDao.clearAll()
        }
        rootFolderDao.upsert(
            RootFolderEntity(
                displayName = displayName,
                treeUri = treeUri,
                displayPath = displayPath,
                lastScannedAt = 0L,
                imageCount = 0,
                persistPermission = persistPermission,
            ),
        )
    }

    /** primary:Movies → Movies；primary:DCIM/Camera → DCIM/Camera */
    private fun friendlyTreeLabel(treeUri: Uri): String? {
        if (StorageAccessHelper.isFileRootUri(treeUri)) {
            return treeUri.path?.let { StorageAccessHelper.displayPathForFile(File(it)) }
        }
        return try {
            val docId = DocumentsContract.getTreeDocumentId(treeUri)
            val path = docId.substringAfter(':', missingDelimiterValue = docId)
            path.replace(':', '/').ifBlank { null }
        } catch (_: Throwable) {
            null
        }
    }

    private fun queryTreeDisplayName(treeUri: Uri): String? {
        if (StorageAccessHelper.isFileRootUri(treeUri)) {
            return treeUri.path?.let { File(it).name }
        }
        return try {
            val docId = DocumentsContract.getTreeDocumentId(treeUri)
            val docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
            context.contentResolver.query(
                docUri,
                arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME),
                null,
                null,
                null,
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    return cursor.getString(0)
                }
            }
            null
        } catch (_: Throwable) {
            null
        }
    }
}
