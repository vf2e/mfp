package com.guopian.app.data.move

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import com.guopian.app.domain.model.ConflictPolicy
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

data class MoveResult(
    val success: Boolean,
    val skipped: Boolean = false,
    val destUri: Uri? = null,
    val destDocumentId: String? = null,
    val destDisplayName: String? = null,
    val error: String? = null,
)

@Singleton
class MoveExecutor @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun moveOne(
        sourceUri: Uri,
        targetTreeUri: Uri,
        displayName: String,
        sizeBytes: Long,
        policy: ConflictPolicy,
    ): MoveResult {
        val resolver = context.contentResolver
        val targetDocId = DocumentsContract.getTreeDocumentId(targetTreeUri)
        val targetDirUri = DocumentsContract.buildDocumentUriUsingTree(targetTreeUri, targetDocId)
        val mime = resolver.getType(sourceUri) ?: "video/*"

        val finalName = when (policy) {
            ConflictPolicy.RENAME -> uniqueName(targetTreeUri, displayName)
            ConflictPolicy.SKIP -> {
                if (childExists(targetTreeUri, displayName)) {
                    return MoveResult(success = false, skipped = true, error = "SKIP")
                }
                displayName
            }
            ConflictPolicy.OVERWRITE -> {
                deleteChildIfExists(targetTreeUri, displayName)
                displayName
            }
        }

        // Try atomic move within same provider
        try {
            val moved = DocumentsContract.moveDocument(
                resolver,
                sourceUri,
                parentDocumentUri(sourceUri) ?: sourceUri,
                targetDirUri,
            )
            if (moved != null) {
                // moveDocument may keep original name; rename if needed
                val renamed = maybeRename(moved, finalName)
                return MoveResult(
                    success = true,
                    destUri = renamed,
                    destDocumentId = DocumentsContract.getDocumentId(renamed),
                    destDisplayName = finalName,
                )
            }
        } catch (t: Throwable) {
            Timber.d(t, "moveDocument not available, fallback copy+delete")
        }

        return copyThenDelete(sourceUri, targetDirUri, finalName, mime, sizeBytes)
    }

    private fun copyThenDelete(
        sourceUri: Uri,
        targetDirUri: Uri,
        finalName: String,
        mime: String,
        expectedSize: Long,
    ): MoveResult {
        val resolver = context.contentResolver
        val destUri = try {
            DocumentsContract.createDocument(resolver, targetDirUri, mime, finalName)
        } catch (t: Throwable) {
            return MoveResult(success = false, error = "CREATE_DEST: ${t.message}")
        } ?: return MoveResult(success = false, error = "CREATE_DEST_NULL")

        try {
            resolver.openInputStream(sourceUri).use { input ->
                resolver.openOutputStream(destUri).use { output ->
                    if (input == null || output == null) {
                        throw IOException("stream null")
                    }
                    input.copyTo(output, DEFAULT_BUFFER_SIZE)
                    output.flush()
                }
            }
            val copiedSize = querySize(destUri)
            if (expectedSize > 0 && copiedSize >= 0 && copiedSize != expectedSize) {
                DocumentsContract.deleteDocument(resolver, destUri)
                return MoveResult(success = false, error = "MOVE_VERIFY")
            }
            val deleted = try {
                DocumentsContract.deleteDocument(resolver, sourceUri)
            } catch (t: Throwable) {
                Timber.e(t, "delete source failed after copy")
                false
            }
            if (!deleted) {
                // Keep both; mark as failure so user can retry carefully
                return MoveResult(
                    success = false,
                    destUri = destUri,
                    error = "MOVE_DELETE_SOURCE",
                )
            }
            return MoveResult(
                success = true,
                destUri = destUri,
                destDocumentId = DocumentsContract.getDocumentId(destUri),
                destDisplayName = finalName,
            )
        } catch (t: Throwable) {
            try {
                DocumentsContract.deleteDocument(resolver, destUri)
            } catch (_: Throwable) {
            }
            return MoveResult(success = false, error = "MOVE_COPY: ${t.message}")
        }
    }

    private fun uniqueName(treeUri: Uri, displayName: String): String {
        if (!childExists(treeUri, displayName)) return displayName
        val dot = displayName.lastIndexOf('.')
        val base = if (dot > 0) displayName.substring(0, dot) else displayName
        val ext = if (dot > 0) displayName.substring(dot) else ""
        var i = 1
        while (i < 10_000) {
            val candidate = "$base ($i)$ext"
            if (!childExists(treeUri, candidate)) return candidate
            i++
        }
        return "$base (${System.currentTimeMillis()})$ext"
    }

    private fun childExists(treeUri: Uri, displayName: String): Boolean {
        val parentId = DocumentsContract.getTreeDocumentId(treeUri)
        val children = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentId)
        context.contentResolver.query(
            children,
            arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME),
            null,
            null,
            null,
        )?.use { c ->
            val idx = c.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            while (c.moveToNext()) {
                if (idx >= 0 && c.getString(idx) == displayName) return true
            }
        }
        return false
    }

    private fun deleteChildIfExists(treeUri: Uri, displayName: String) {
        val parentId = DocumentsContract.getTreeDocumentId(treeUri)
        val children = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentId)
        context.contentResolver.query(
            children,
            arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            ),
            null,
            null,
            null,
        )?.use { c ->
            val idIdx = c.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
            val nameIdx = c.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            while (c.moveToNext()) {
                if (nameIdx >= 0 && c.getString(nameIdx) == displayName && idIdx >= 0) {
                    val docId = c.getString(idIdx)
                    val uri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
                    try {
                        DocumentsContract.deleteDocument(context.contentResolver, uri)
                    } catch (_: Throwable) {
                    }
                }
            }
        }
    }

    private fun parentDocumentUri(docUri: Uri): Uri? {
        return try {
            val docId = DocumentsContract.getDocumentId(docUri)
            val parent = docId.substringBeforeLast(':', missingDelimiterValue = "")
            if (parent.isEmpty() || parent == docId) null
            else DocumentsContract.buildDocumentUriUsingTree(docUri, parent)
        } catch (_: Throwable) {
            null
        }
    }

    private fun maybeRename(uri: Uri, desiredName: String): Uri {
        return try {
            DocumentsContract.renameDocument(context.contentResolver, uri, desiredName) ?: uri
        } catch (_: Throwable) {
            uri
        }
    }

    private fun querySize(uri: Uri): Long {
        context.contentResolver.query(
            uri,
            arrayOf(DocumentsContract.Document.COLUMN_SIZE),
            null,
            null,
            null,
        )?.use { c ->
            if (c.moveToFirst()) {
                val idx = c.getColumnIndex(DocumentsContract.Document.COLUMN_SIZE)
                if (idx >= 0) return c.getLong(idx)
            }
        }
        return -1L
    }
}
