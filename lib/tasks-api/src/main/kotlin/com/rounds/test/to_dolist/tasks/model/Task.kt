package com.rounds.test.to_dolist.tasks.model

import java.time.Instant

/**
 * A task as the rest of the app sees it. Ids are opaque strings: the mock API owns id generation and
 * the UI never parses them.
 *
 * [dueDate] is an [Instant] rather than a `LocalDate` because it crosses the transport, where a point
 * in time is unambiguous and a calendar day is not. Turning it into "tomorrow" is the UI's job, in the
 * viewer's own time zone.
 */
data class Task(
    val id: String,
    val title: String,
    val notes: String?,
    val priority: TaskPriority,
    val isCompleted: Boolean,
    val createdAt: Instant,
    val dueDate: Instant? = null,
)
