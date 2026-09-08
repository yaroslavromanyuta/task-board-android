package com.rounds.test.to_dolist.core.testing

import com.rounds.test.to_dolist.tasks.model.Task
import com.rounds.test.to_dolist.tasks.model.TaskPriority
import java.time.Instant

/** Fixed timestamps: tests that assert on ordering must not depend on how fast they run. */
object TestData {

    val epoch: Instant = Instant.parse("2026-01-01T00:00:00Z")

    fun task(
        id: String = "1",
        title: String = "Task $id",
        notes: String? = null,
        priority: TaskPriority = TaskPriority.MEDIUM,
        isCompleted: Boolean = false,
        createdAt: Instant = epoch,
        dueDate: Instant? = null,
    ) = Task(
        id = id,
        title = title,
        notes = notes,
        priority = priority,
        isCompleted = isCompleted,
        createdAt = createdAt,
        dueDate = dueDate,
    )

    val tasks: List<Task> = listOf(
        task(id = "1", title = "Buy milk", priority = TaskPriority.LOW),
        task(id = "2", title = "Renew passport", priority = TaskPriority.HIGH, notes = "Book a slot first"),
        task(id = "3", title = "Water the plants", priority = TaskPriority.MEDIUM, isCompleted = true),
    )
}
