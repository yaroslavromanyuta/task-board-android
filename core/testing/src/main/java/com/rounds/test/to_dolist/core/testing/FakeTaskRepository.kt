package com.rounds.test.to_dolist.core.testing

import com.rounds.test.to_dolist.tasks.error.DataError
import com.rounds.test.to_dolist.tasks.error.DataException
import com.rounds.test.to_dolist.tasks.model.Task
import com.rounds.test.to_dolist.tasks.model.TaskDraft
import com.rounds.test.to_dolist.tasks.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * In-memory [TaskRepository] for feature tests. Its existence is the point of the boundary rules: a
 * feature module can be tested end to end without `:data:tasks` on the classpath.
 *
 * [nextError] makes the failure states testable without waiting for the mock network's dice.
 */
class FakeTaskRepository(
    initial: List<Task> = TestData.tasks,
) : TaskRepository {

    private val tasks = MutableStateFlow(initial)

    /** Set to make the next suspending call fail; cleared once consumed. */
    var nextError: DataError? = null

    override fun observeTasks(): Flow<List<Task>> = tasks

    override suspend fun refresh(): Result<Unit> = TODO("Skeleton: filled in with the feature tests")

    override suspend fun getTask(id: String): Result<Task> = TODO("Skeleton: filled in with the feature tests")

    override suspend fun createTask(draft: TaskDraft): Result<Task> =
        TODO("Skeleton: filled in with the feature tests")

    override suspend fun updateTask(id: String, draft: TaskDraft): Result<Task> =
        TODO("Skeleton: filled in with the feature tests")

    override suspend fun setCompleted(id: String, completed: Boolean): Result<Unit> =
        TODO("Skeleton: filled in with the feature tests")

    override suspend fun deleteTask(id: String): Result<Unit> =
        TODO("Skeleton: filled in with the feature tests")

    private fun <T> fail(error: DataError): Result<T> = Result.failure(DataException(error))
}
