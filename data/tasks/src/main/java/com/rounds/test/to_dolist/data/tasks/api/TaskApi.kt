package com.rounds.test.to_dolist.data.tasks.api

import com.rounds.test.to_dolist.data.tasks.api.model.TaskDto
import com.rounds.test.to_dolist.data.tasks.api.model.TaskPayload

/**
 * The app's view of "the backend". Deliberately shaped like a real remote API:
 *  - every call suspends and may take a while;
 *  - failures arrive as thrown exceptions, not as a nullable return;
 *  - it speaks DTOs, never domain models.
 *
 * Keeping this an interface means the mock implementation is swappable for a real Retrofit service
 * without touching the repository.
 */
interface TaskApi {

    suspend fun getTasks(): List<TaskDto>

    suspend fun getTask(id: String): TaskDto

    suspend fun createTask(payload: TaskPayload): TaskDto

    suspend fun updateTask(id: String, payload: TaskPayload): TaskDto

    suspend fun deleteTask(id: String)
}
