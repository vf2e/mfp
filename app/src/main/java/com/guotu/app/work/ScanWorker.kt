package com.guotu.app.work

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.guotu.app.core.util.ImageExtensions
import com.guotu.app.core.util.StorageAccessHelper
import com.guotu.app.data.db.ImageDao
import com.guotu.app.data.db.ImageEntity
import com.guotu.app.data.db.RootFolderDao
import com.guotu.app.data.repo.RootBrowseStateStore
import com.guotu.app.domain.model.MarkStatus
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber

/**
 * 递归扫描根目录下图片并扁平入库。
 * 面向几十万张图：边扫边分批 upsert，不把整库 Entity 堆在内存。
 */
@HiltWorker
class ScanWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val rootFolderDao: RootFolderDao,
    private val imageDao: ImageDao,
    private val browseStateStore: RootBrowseStateStore,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val root = rootFolderDao.getCurrent() ?: return Result.failure()
        val treeUri = Uri.parse(root.treeUri)
        val now = System.currentTimeMillis()

        try {
            val dbMarks = imageDao.listMarkedPaths(root.id)
                .associate { it.relativePath to (it.markStatus to it.star) }
            val fileState = browseStateStore.read(treeUri)
            val mergedMarks = LinkedHashMap<String, Pair<String, Int>>()
            fileState.marks.forEach { (path, status) ->
                mergedMarks[path] = status.name to if (status == MarkStatus.KEEP) 1 else 0
            }
            dbMarks.forEach { (path, mark) -> mergedMarks[path] = mark }

            imageDao.deleteByRoot(root.id)

            val sink = StreamingSink(
                marks = mergedMarks,
                keepPaths = linkedMapOf(),
            )

            if (StorageAccessHelper.isFileRootUri(treeUri)) {
                val path = treeUri.path
                val dir = if (path.isNullOrBlank()) null else java.io.File(path)
                if (dir == null || !dir.isDirectory || !dir.canRead()) {
                    Timber.e("Cannot read file root path=%s", path)
                    return Result.failure(
                        workDataOf(KEY_ERROR to "无法读取本地目录，请确认已开启全部文件访问"),
                    )
                }
                traverseLocalFile(dir, "", 0, root.id, now, sink)
            } else {
                val treeRoot = DocumentFile.fromTreeUri(applicationContext, treeUri)
                if (treeRoot == null || !treeRoot.canRead()) {
                    Timber.e("Cannot read tree uri=%s canRead=%s", treeUri, treeRoot?.canRead())
                    return Result.failure(
                        workDataOf(KEY_ERROR to "无法读取所选文件夹，请重新授权"),
                    )
                }
                traverseDocumentFile(treeUri, treeRoot, "", 0, root.id, now, sink)
                if (sink.accepted == 0) {
                    Timber.w("DocumentFile found 0, trying DocumentsContract cursor scan")
                    val rootDocId = DocumentsContract.getTreeDocumentId(treeUri)
                    traverseWithDocId(treeUri, rootDocId, "", 0, root.id, now, sink)
                }
            }

            sink.flush()
            val stored = imageDao.countVisibleByRoot(root.id)

            browseStateStore.write(
                treeUri,
                RootBrowseStateStore.BrowseState(
                    lastRelativePath = fileState.lastRelativePath,
                    marks = sink.keepPaths,
                ),
            )
            rootFolderDao.updateScanStats(
                id = root.id,
                lastScannedAt = now,
                imageCount = stored,
            )
            Timber.i(
                "Scan finished: accepted=%d stored=%d marks=%d from %s",
                sink.accepted,
                stored,
                sink.keepPaths.size,
                treeUri,
            )
            val folderHint = when {
                StorageAccessHelper.isFileRootUri(treeUri) ->
                    treeUri.path?.let { StorageAccessHelper.displayPathForFile(java.io.File(it)) }.orEmpty()
                else -> try {
                    DocumentsContract.getTreeDocumentId(treeUri).substringAfter(':', "")
                        .replace(':', '/')
                } catch (_: Throwable) {
                    ""
                }
            }
            return Result.success(
                workDataOf(
                    KEY_IMAGE_COUNT to stored,
                    KEY_FOLDER to folderHint,
                ),
            )
        } catch (e: Exception) {
            Timber.e(e, "Scan failed")
            return Result.failure(workDataOf(KEY_ERROR to (e.message ?: "scan_failed")))
        }
    }

    private suspend fun StreamingSink.offer(entity: ImageEntity) {
        val key = entity.documentId ?: entity.docUri
        if (!seen.add(key)) return
        val mark = marks[entity.relativePath]
        val withMark = if (mark == null) {
            entity
        } else {
            entity.copy(markStatus = mark.first, star = mark.second)
        }
        if (withMark.markStatus == MarkStatus.KEEP.name ||
            withMark.markStatus == MarkStatus.PENDING.name
        ) {
            keepPaths[withMark.relativePath] = runCatching {
                MarkStatus.valueOf(withMark.markStatus)
            }.getOrDefault(MarkStatus.TO_DELETE)
        }
        buffer += withMark
        accepted++
        if (buffer.size >= BATCH) {
            flush()
        }
        if (accepted % PROGRESS_EVERY == 0) {
            setProgressAsync(workDataOf(KEY_FOUND to accepted))
        }
    }

    private suspend fun StreamingSink.flush() {
        if (buffer.isEmpty()) return
        imageDao.upsertAll(buffer.toList())
        buffer.clear()
        if (isStopped) error("scan_stopped")
    }

    private inner class StreamingSink(
        val marks: Map<String, Pair<String, Int>>,
        val keepPaths: LinkedHashMap<String, MarkStatus>,
    ) {
        val seen = HashSet<String>(16_384)
        val buffer = ArrayList<ImageEntity>(BATCH)
        var accepted = 0
    }

    private suspend fun traverseLocalFile(
        folder: java.io.File,
        parentRelative: String,
        depth: Int,
        rootId: Long,
        now: Long,
        sink: StreamingSink,
    ) {
        if (isStopped || depth > MAX_DEPTH) return
        val children = try {
            folder.listFiles()
        } catch (t: Throwable) {
            Timber.w(t, "listFiles failed at %s", folder.absolutePath)
            null
        } ?: return
        for (child in children) {
            if (isStopped) return
            val name = child.name ?: continue
            if (name.startsWith(".")) continue
            if (name.equals(RootBrowseStateStore.FILE_NAME, ignoreCase = true)) continue
            val childRelative = if (parentRelative.isEmpty()) name else "$parentRelative/$name"
            when {
                child.isDirectory -> traverseLocalFile(
                    child, childRelative, depth + 1, rootId, now, sink,
                )
                ImageExtensions.isImageFileName(name) -> {
                    sink.offer(
                        ImageEntity(
                            rootId = rootId,
                            docUri = Uri.fromFile(child).toString(),
                            documentId = child.absolutePath,
                            relativePath = childRelative,
                            parentSubPath = parentRelative,
                            depth = depth,
                            fileName = name,
                            ext = ImageExtensions.extensionOf(name),
                            mime = null,
                            sizeBytes = child.length(),
                            dateModified = child.lastModified(),
                            lastScannedAt = now,
                        ),
                    )
                }
            }
        }
    }

    private suspend fun traverseDocumentFile(
        treeUri: Uri,
        folder: DocumentFile,
        parentRelative: String,
        depth: Int,
        rootId: Long,
        now: Long,
        sink: StreamingSink,
    ) {
        if (isStopped || depth > MAX_DEPTH) return
        val children = try {
            folder.listFiles()
        } catch (t: Throwable) {
            Timber.w(t, "listFiles failed at %s", folder.uri)
            emptyArray()
        }
        for (child in children) {
            if (isStopped) return
            val name = child.name ?: continue
            if (name.startsWith(".")) continue
            if (name.equals(RootBrowseStateStore.FILE_NAME, ignoreCase = true)) continue
            val childRelative = if (parentRelative.isEmpty()) name else "$parentRelative/$name"
            val isDir = child.isDirectory
            when {
                isDir -> traverseDocumentFile(
                    treeUri, child, childRelative, depth + 1, rootId, now, sink,
                )
                isImage(name, child.type) -> {
                    sink.offer(
                        ImageEntity(
                            rootId = rootId,
                            docUri = child.uri.toString(),
                            documentId = runCatching {
                                DocumentsContract.getDocumentId(child.uri)
                            }.getOrNull(),
                            relativePath = childRelative,
                            parentSubPath = parentRelative,
                            depth = depth,
                            fileName = name,
                            ext = ImageExtensions.extensionOf(name),
                            mime = child.type,
                            sizeBytes = child.length(),
                            dateModified = child.lastModified(),
                            lastScannedAt = now,
                        ),
                    )
                }
            }
        }
    }

    private suspend fun traverseWithDocId(
        treeUri: Uri,
        parentDocId: String,
        parentRelative: String,
        depth: Int,
        rootId: Long,
        now: Long,
        sink: StreamingSink,
    ) {
        if (isStopped || depth > MAX_DEPTH) return
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentDocId)
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_SIZE,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED,
        )
        val cursor = try {
            applicationContext.contentResolver.query(childrenUri, projection, null, null, null)
        } catch (t: Throwable) {
            Timber.w(t, "query children failed docId=%s", parentDocId)
            null
        } ?: return
        cursor.use { c ->
            val idIdx = c.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
            val nameIdx = c.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            val mimeIdx = c.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)
            val sizeIdx = c.getColumnIndex(DocumentsContract.Document.COLUMN_SIZE)
            val modIdx = c.getColumnIndex(DocumentsContract.Document.COLUMN_LAST_MODIFIED)
            if (idIdx < 0 || nameIdx < 0) return

            while (c.moveToNext()) {
                if (isStopped) return
                val docId = c.getString(idIdx) ?: continue
                val name = c.getString(nameIdx) ?: continue
                if (name.startsWith(".")) continue
                if (name.equals(RootBrowseStateStore.FILE_NAME, ignoreCase = true)) continue
                val mime = if (mimeIdx >= 0) c.getString(mimeIdx) else null
                val size = if (sizeIdx >= 0 && !c.isNull(sizeIdx)) c.getLong(sizeIdx) else 0L
                val modified = if (modIdx >= 0 && !c.isNull(modIdx)) c.getLong(modIdx) else 0L
                val childRelative = if (parentRelative.isEmpty()) name else "$parentRelative/$name"
                val isDir = mime == DocumentsContract.Document.MIME_TYPE_DIR ||
                    mime == "vnd.android.document/directory"
                if (isDir) {
                    traverseWithDocId(treeUri, docId, childRelative, depth + 1, rootId, now, sink)
                } else if (isImage(name, mime)) {
                    val docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
                    sink.offer(
                        ImageEntity(
                            rootId = rootId,
                            docUri = docUri.toString(),
                            documentId = docId,
                            relativePath = childRelative,
                            parentSubPath = parentRelative,
                            depth = depth,
                            fileName = name,
                            ext = ImageExtensions.extensionOf(name),
                            mime = mime,
                            sizeBytes = size,
                            dateModified = modified,
                            lastScannedAt = now,
                        ),
                    )
                }
            }
        }
    }

    private fun isImage(name: String, mime: String?): Boolean {
        if (mime?.startsWith("image/") == true) return true
        if (mime == DocumentsContract.Document.MIME_TYPE_DIR) return false
        return ImageExtensions.isImageFileName(name)
    }

    companion object {
        const val UNIQUE_WORK = "guotu_scan"
        const val KEY_IMAGE_COUNT = "image_count"
        const val KEY_FOUND = "found"
        const val KEY_ERROR = "error"
        const val KEY_FOLDER = "folder"
        private const val MAX_DEPTH = 32
        private const val BATCH = 200
        private const val PROGRESS_EVERY = 50
    }
}
