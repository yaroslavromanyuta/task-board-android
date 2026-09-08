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
 * It keeps the real repository's shape rather than collapsing it: [source] stands in for the network
 * and the observed stream stands in for the cache, so it starts *empty* and only fills on [refresh].
 * A ViewModel test therefore sees the same loading-then-content sequence the app does.
 *
 * [nextError] makes the failure states testable without waiting for the mock network's dice.
 */
class FakeTaskRepository(
    source: List<Task> = TestData.tasks,
) : TaskRepository {

    private val source = source.toMutableList()
    private val cached = MutableStateFlow<List<Task>>(emptyList())
    private var lastId = source.size

    /** Set to make the next suspending call fail; cleared once consumed. */
    var nextError: DataError? = null

    override fun observeTasks(): Flow<List<Task>> = cached

    override suspend fun refresh(): Result<Unit> = attempt {
        cached.value = source.toList()
    }

    override suspend fun getTask(id: String): Result<Task> = attempt {
        source.requireTask(id)
    }

    override suspend fun createTask(draft: TaskDraft): Result<Task> = attempt {
        TestData.task(
            id = "task-${++lastId}",
            title = draft.title,
            notes = draft.notes,
            priority = draft.priority,
        ).also { created ->
            source += created
            cached.value = cached.value + created
        }
    }

    override suspend fun updateTask(id: String, draft: TaskDraft): Result<Task> = attempt {
        val updated = source.requireTask(id).copy(
            title = draft.title,
            notes = draft.notes,
            priority = draft.priority,
        )
        store(updated)
        updated
    }

    override suspend fun setCompleted(id: String, completed: Boolean): Result<Unit> = attempt {
        store(source.requireTask(id).copy(isCompleted = completed))
    }

    override suspend fun deleteTask(id: String): Result<Unit> = attempt {
        source.requireTask(id)
        source.removeAll { it.id == id }
        cached.value = cached.value.filterNot { it.id == id }
    }

    /** Writes reach the observed stream only after the "network" half succeeded, as in the real one. */
    private fun store(task: Task) {
        source.replaceTask(task)
        if (cached.value.any { it.id == task.id }) {
            cached.value = cached.value.map { if (it.id == task.id) task else it }
        }
    }

    private fun <T> attempt(block: () -> T): Result<T> {
        nextError?.let { error ->
            nextError = null
            return fail(error)
        }
        return try {
            Result.success(block())
        } catch (failure: DataException) {
            Result.failure(failure)
        }
    }

    private fun List<Task>.requireTask(id: String): Task =
        firstOrNull { it.id == id } ?: throw DataException(DataError.NotFound)

    private fun MutableList<Task>.replaceTask(task: Task) {
        val index = indexOfFirst { it.id == task.id }
        if (index >= 0) this[index] = task else this += task
    }

    private fun <T> fail(error: DataError): Result<T> = Result.failure(DataException(error))
}
