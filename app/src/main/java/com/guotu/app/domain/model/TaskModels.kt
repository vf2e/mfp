package com.guotu.app.domain.model

enum class ConflictPolicy {
    RENAME,
    SKIP,
    OVERWRITE,
}

enum class TaskType {
    MOVE,
    DELETE,
}

enum class TaskState {
    PENDING,
    RUNNING,
    SUCCESS,
    FAILED,
    CANCELLED,
}

enum class TaskItemState {
    PENDING,
    RUNNING,
    SUCCESS,
    FAILED,
    SKIPPED,
    CANCELLED,
}
