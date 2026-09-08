package com.rounds.test.to_dolist.data.tasks.api

import com.rounds.test.to_dolist.core.common.time.Clock
import com.rounds.test.to_dolist.data.tasks.api.model.TaskDto
import com.rounds.test.to_dolist.data.tasks.api.model.TaskPayload
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Stand-in for the backend. Not implemented yet — this is the seam the mock network layer drops into.
 *
 * Planned behaviour, so the states the UI must handle are real rather than simulated in the ViewModel:
 *  - a `MutableList<TaskDto>` guarded by a `Mutex`, since callers can hit it concurrently;
 *  - `delay(200..900 ms)` on every call, so "loading" is a state a user can actually see;
 *  - roughly a 1-in-7 chance of throwing `DataException(DataError.Network)` or `Timeout`, so the error
 *    path is exercised by simply using the app;
 *  - `DataError.NotFound` for an unknown id, which is a deterministic failure rather than a dice roll;
 *  - server-assigned ids and `createdAt` from the injected [Clock].
 *
 * The failure rate is intended to be a `var` so a debug switch can turn the dice off during a demo.
 */
@Singleton
class FakeTaskApi @Inject constructor(
    private val clock: Clock,
) : TaskApi {

    override suspend fun getTasks(): List<TaskDto> = TODO("Mock network layer: separate step")

    override suspend fun getTask(id: String): TaskDto = TODO("Mock network layer: separate step")

    override suspend fun createTask(payload: TaskPayload): TaskDto = TODO("Mock network layer: separate step")

    override suspend fun updateTask(id: String, payload: TaskPayload): TaskDto =
        TODO("Mock network layer: separate step")

    override suspend fun deleteTask(id: String) = TODO("Mock network layer: separate step")
}
