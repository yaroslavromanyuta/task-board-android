package com.rounds.test.to_dolist.tasks.model

/**
 * The editable half of a [Task]. Create and update take the same payload, which is why one editor
 * screen serves both: the presence of an id decides the mode, not the shape of the data.
 */
data class TaskDraft(
    val title: String,
    val notes: String?,
    val priority: TaskPriority,
)

/** Convenience for the edit path, where the form is seeded from an existing task. */
fun Task.toDraft(): TaskDraft = TaskDraft(
    title = title,
    notes = notes,
    priority = priority,
)
