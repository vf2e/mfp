package com.guotu.app.ui.tags

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.guotu.app.data.db.TagDao
import com.guotu.app.data.db.TagEntity
import com.guotu.app.data.db.TagWithCount
import com.guotu.app.ui.library.LibraryBrowseRequest
import com.guotu.app.ui.library.LibraryFilterBridge
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

val TAG_COLOR_PALETTE = listOf(
    "#3B82F6",
    "#10B981",
    "#F59E0B",
    "#EF4444",
    "#06B6D4",
    "#64748B",
    "#EC4899",
    "#8B5CF6",
)

data class TagsUiState(
    val draftName: String = "",
    val draftColor: String = TAG_COLOR_PALETTE.first(),
    val showCreate: Boolean = false,
)

@HiltViewModel
class TagsViewModel @Inject constructor(
    private val tagDao: TagDao,
    private val filterBridge: LibraryFilterBridge,
) : ViewModel() {

    val tags: StateFlow<List<TagWithCount>> = tagDao.observeAllWithCount()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    private val _ui = MutableStateFlow(TagsUiState())
    val ui: StateFlow<TagsUiState> = _ui.asStateFlow()

    fun openCreate() {
        _ui.update {
            it.copy(
                showCreate = true,
                draftName = "",
                draftColor = TAG_COLOR_PALETTE.random(),
            )
        }
    }

    fun dismissCreate() {
        _ui.update { it.copy(showCreate = false, draftName = "") }
    }

    fun setDraftName(name: String) {
        _ui.update { it.copy(draftName = name) }
    }

    fun setDraftColor(color: String) {
        _ui.update { it.copy(draftColor = color) }
    }

    fun createTag() {
        val name = _ui.value.draftName.trim()
        if (name.isEmpty()) return
        val color = _ui.value.draftColor
        viewModelScope.launch {
            tagDao.upsert(
                TagEntity(
                    name = name,
                    color = color,
                ),
            )
            dismissCreate()
        }
    }

    fun deleteTag(id: Long) {
        viewModelScope.launch {
            tagDao.unlinkAllForTag(id)
            tagDao.delete(id)
        }
    }

    fun openTag(tag: TagWithCount) {
        filterBridge.open(
            LibraryBrowseRequest.ByTag(
                tagId = tag.id,
                label = tag.name,
            ),
        )
    }
}
