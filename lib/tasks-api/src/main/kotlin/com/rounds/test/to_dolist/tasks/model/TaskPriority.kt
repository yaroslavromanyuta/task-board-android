package com.rounds.test.to_dolist.tasks.model

/**
 * Declared low-to-high so the natural [Comparable] order of the enum is also the sort order the list
 * screen wants; nothing downstream has to hardcode a ranking.
 */
enum class TaskPriority {
    LOW,
    MEDIUM,
    HIGH,
}
