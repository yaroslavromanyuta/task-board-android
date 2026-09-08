package com.rounds.test.to_dolist.data.tasks.api

import com.rounds.test.to_dolist.data.tasks.api.model.TaskDto
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * The rows the mock source starts with, reproduced from the brief (REQUIREMENTS.md §8) rather than
 * invented here. The fourth row is deliberately long: it exists to stress the list row layout, so it
 * must not be shortened.
 *
 * Ids and timestamps are absent on purpose — the source assigns both, exactly as a backend would.
 *
 * [dueInDays] is an offset rather than a date, for the same reason: a fixed date would go stale, and
 * resolving it against the injected clock keeps the seed deterministic under test. Two rows carry one
 * and two do not, so the list shows both cases from launch (FR-13).
 */
internal data class SeedTask(
    val title: String,
    val notes: String,
    val priority: String,
    val completed: Boolean,
    val dueInDays: Long? = null,
) {
    fun toDto(id: String, createdAt: Instant): TaskDto = TaskDto(
        id = id,
        title = title,
        notes = notes,
        priority = priority,
        completed = completed,
        createdAtEpochMillis = createdAt.toEpochMilli(),
        dueDateEpochMillis = dueInDays?.let { createdAt.plus(it, ChronoUnit.DAYS).toEpochMilli() },
    )
}

internal val SEED_TASKS: List<SeedTask> = listOf(
    SeedTask(
        title = "Renew domain registration",
        notes = "Expires end of month",
        priority = "High",
        completed = false,
        dueInDays = 3,
    ),
    SeedTask(
        title = "Reply to design feedback",
        notes = "",
        priority = "Medium",
        completed = false,
    ),
    SeedTask(
        title = "Book dentist",
        notes = "",
        priority = "Low",
        completed = true,
    ),
    SeedTask(
        title = "Migrate the analytics pipeline to the new warehouse and validate dashboards",
        notes = "Long one \u2014 check layout",
        priority = "Medium",
        completed = false,
        dueInDays = -1,
    ),
)
