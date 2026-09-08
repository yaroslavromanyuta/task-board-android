package com.rounds.test.to_dolist.data.tasks.api.model

/**
 * Wire shape, kept separate from the domain [com.rounds.test.to_dolist.tasks.model.Task] on purpose:
 * the transport is free to use strings and epoch millis without those leaking into the UI.
 */
data class TaskDto(
    val id: String,
    val title: String,
    val notes: String?,
    val priority: String,
    val completed: Boolean,
    val createdAtEpochMillis: Long,
)

/**
 * Request body for create and update — the server owns id and timestamps.
 *
 * [completed] travels in the payload because `TaskApi` deliberately has no "set completed" operation:
 * five REST-shaped calls are the contract, so completing a task is an update like any other.
 */
data class TaskPayload(
    val title: String,
    val notes: String?,
    val priority: String,
    val completed: Boolean = false,
)
