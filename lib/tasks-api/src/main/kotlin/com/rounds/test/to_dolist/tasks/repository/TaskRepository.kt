package com.rounds.test.to_dolist.tasks.repository

import com.rounds.test.to_dolist.tasks.model.Task
import com.rounds.test.to_dolist.tasks.model.TaskDraft
import kotlinx.coroutines.flow.Flow

/**
 * The single boundary the presentation layer talks to, and the reason features never depend on
 * `:data:tasks`. Failures are always
 * [com.rounds.test.to_dolist.tasks.error.DataException], so ViewModels see typed errors and never a
 * transport exception.
 *
 * [observeTasks] is a cold stream over the in-memory cache; [refresh] is the only call that hits the
 * network. Splitting them lets the list render cached content while a refresh is in flight.
 */
interface TaskRepository {

    fun observeTasks(): Flow<List<Task>>

    suspend fun refresh(): Result<Unit>

    suspend fun getTask(id: String): Result<Task>

    suspend fun createTask(draft: TaskDraft): Result<Task>

    suspend fun updateTask(id: String, draft: TaskDraft): Result<Task>

    suspend fun setCompleted(id: String, completed: Boolean): Result<Unit>

    suspend fun deleteTask(id: String): Result<Unit>
}
