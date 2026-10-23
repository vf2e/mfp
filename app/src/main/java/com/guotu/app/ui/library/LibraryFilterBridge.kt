package com.guotu.app.ui.library

import com.guotu.app.domain.model.MarkStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** 从分类/标签跳到图库时携带的筛选意图。 */
sealed interface LibraryBrowseRequest {
    data class ByMarks(
        val statuses: Set<MarkStatus>,
        val label: String,
    ) : LibraryBrowseRequest

    data class ByExtension(
        val ext: String,
        val label: String,
    ) : LibraryBrowseRequest

    data class ByFolder(
        val parentSubPath: String,
        val label: String,
    ) : LibraryBrowseRequest

    data class ByTag(
        val tagId: Long,
        val label: String,
    ) : LibraryBrowseRequest
}

@Singleton
class LibraryFilterBridge @Inject constructor() {
    private val _pending = MutableStateFlow<LibraryBrowseRequest?>(null)
    val pending: StateFlow<LibraryBrowseRequest?> = _pending.asStateFlow()

    fun open(request: LibraryBrowseRequest) {
        _pending.value = request
    }

    fun consume() {
        _pending.value = null
    }
}
