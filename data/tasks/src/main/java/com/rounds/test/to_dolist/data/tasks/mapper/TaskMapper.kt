package com.rounds.test.to_dolist.data.tasks.mapper

import com.rounds.test.to_dolist.data.tasks.api.model.TaskDto
import com.rounds.test.to_dolist.data.tasks.api.model.TaskPayload
import com.rounds.test.to_dolist.tasks.model.Task
import com.rounds.test.to_dolist.tasks.model.TaskDraft

/**
 * The only place that knows both shapes. An unknown priority string must not crash the list, so the
 * mapper is responsible for falling back rather than the caller.
 */
fun TaskDto.toDomain(): Task = TODO("Skeleton: implemented with the mock network layer")

fun TaskDraft.toPayload(): TaskPayload = TODO("Skeleton: implemented with the mock network layer")
