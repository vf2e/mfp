package com.guopian.app.domain.model

enum class MarkStatus {
    UNSORTED,
    KEEP,
    PENDING,
    TO_DELETE,
    MOVED,
}

enum class EngineId {
    EXO,
    VLC,
    EXTERNAL,
}

enum class SortKey {
    DATE_MODIFIED,
    NAME,
    SIZE,
    DURATION,
    RESOLUTION,
    PATH,
    RANDOM,
}

data class SortSpec(
    val key: SortKey = SortKey.DATE_MODIFIED,
    val ascending: Boolean = false,
)

data class FilterSpec(
    val pathContains: List<String> = emptyList(),
    val pathExcludes: List<String> = emptyList(),
    val parentSubPaths: List<String> = emptyList(),
    val markStatuses: Set<MarkStatus> = emptySet(),
    val extensions: Set<String> = emptySet(),
    val tagId: Long? = null,
    val onlyUnmarked: Boolean = false,
    val queryText: String? = null,
)
