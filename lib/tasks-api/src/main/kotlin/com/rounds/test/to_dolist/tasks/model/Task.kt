package com.rounds.test.to_dolist.tasks.model

import java.time.Instant

/**
 * A task as the rest of the app sees it. Ids are opaque strings: the mock API owns id generation and
 * the UI never parses them.
 */
data class Task(
    val id: String,
    val title: String,
    val notes: String?,
    val priority: TaskPriority,
    val isCompleted: Boolean,
    val createdAt: Instant,
)
