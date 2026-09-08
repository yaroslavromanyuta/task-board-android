package com.rounds.test.to_dolist.data.tasks.mapper

import com.rounds.test.to_dolist.data.tasks.api.model.TaskDto
import com.rounds.test.to_dolist.data.tasks.api.model.TaskPayload
import com.rounds.test.to_dolist.tasks.model.Task
import com.rounds.test.to_dolist.tasks.model.TaskDraft
import com.rounds.test.to_dolist.tasks.model.TaskPriority
import java.time.Instant

/**
 * The only place that knows both shapes. An unknown priority string must not crash the list, so the
 * mapper is responsible for falling back rather than the caller.
 */
fun TaskDto.toDomain(): Task = Task(
    id = id,
    title = title,
    notes = notes,
    priority = priority.toPriority(),
    isCompleted = completed,
    createdAt = Instant.ofEpochMilli(createdAtEpochMillis),
    dueDate = dueDateEpochMillis?.let(Instant::ofEpochMilli),
)

fun TaskDraft.toPayload(completed: Boolean = false): TaskPayload = TaskPayload(
    title = title,
    notes = notes,
    priority = priority.name,
    completed = completed,
    dueDateEpochMillis = dueDate?.toEpochMilli(),
)

/**
 * Case-insensitive because the seed rows come from the brief spelled `"High"`, while anything this
 * app writes back is [TaskPriority.name]. A value from neither set is a source the app does not
 * understand yet — it degrades to the middle of the scale instead of taking the list down.
 */
private fun String.toPriority(): TaskPriority =
    TaskPriority.entries.firstOrNull { it.name.equals(this, ignoreCase = true) } ?: TaskPriority.MEDIUM
