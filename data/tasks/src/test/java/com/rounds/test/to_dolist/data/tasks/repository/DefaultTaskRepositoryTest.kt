package com.rounds.test.to_dolist.data.tasks.repository

import com.rounds.test.to_dolist.data.tasks.api.TaskApi
import com.rounds.test.to_dolist.data.tasks.api.model.TaskDto
import com.rounds.test.to_dolist.data.tasks.api.model.TaskPayload
import com.rounds.test.to_dolist.data.tasks.cache.InMemoryTaskCache
import com.rounds.test.to_dolist.tasks.error.DataError
import com.rounds.test.to_dolist.tasks.error.DataException
import com.rounds.test.to_dolist.tasks.model.TaskDraft
import com.rounds.test.to_dolist.tasks.model.TaskPriority
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The repository's contract is two promises: the cache is only ever updated by a call that succeeded,
 * and a caller never sees a failure that is not a [DataException]. Both are tested against a hand-
 * written API double, so nothing here depends on the mock source's latency or dice.
 */
class DefaultTaskRepositoryTest {

    private val api = RecordingTaskApi()
    private val cache = InMemoryTaskCache()

    @Test
    fun `refresh publishes the source into the observed stream`() = runTest {
        val repository = repository()

        val result = repository.refresh()

        assertTrue(result.isSuccess)
        assertEquals(listOf("1", "2"), repository.observeTasks().first().map { it.id })
    }

    @Test
    fun `an unknown priority string falls back instead of crashing`() = runTest {
        api.store["1"] = dto(id = "1", priority = "URGENT")
        val repository = repository()

        repository.refresh()

        assertEquals(TaskPriority.MEDIUM, repository.observeTasks().first().first().priority)
    }

    @Test
    fun `a failed refresh leaves the cache untouched`() = runTest {
        val repository = repository()
        repository.refresh()
        api.nextError = DataError.Network

        val result = repository.refresh()

        assertEquals(DataError.Network, result.dataError())
        assertEquals(listOf("1", "2"), repository.observeTasks().first().map { it.id })
    }

    @Test
    fun `a failed write leaves the cache untouched`() = runTest {
        val repository = repository()
        repository.refresh()
        api.nextError = DataError.Network

        val result = repository.deleteTask("1")

        assertEquals(DataError.Network, result.dataError())
        assertEquals(listOf("1", "2"), repository.observeTasks().first().map { it.id })
    }

    @Test
    fun `a successful delete removes the row`() = runTest {
        val repository = repository()
        repository.refresh()

        assertTrue(repository.deleteTask("1").isSuccess)
        assertEquals(listOf("2"), repository.observeTasks().first().map { it.id })
    }

    @Test
    fun `setCompleted keeps the other fields and updates the stream`() = runTest {
        val repository = repository()
        repository.refresh()

        assertTrue(repository.setCompleted("1", completed = true).isSuccess)

        val task = repository.observeTasks().first().first { it.id == "1" }
        assertEquals(true, task.isCompleted)
        assertEquals("Task 1", task.title)
        assertEquals(TaskPriority.HIGH, task.priority)
    }

    @Test
    fun `create adds exactly one row and update replaces in place`() = runTest {
        val repository = repository()
        repository.refresh()

        val created = repository.createTask(draft("Third")).getOrThrow()
        assertEquals(3, repository.observeTasks().first().size)

        repository.updateTask(created.id, draft("Renamed")).getOrThrow()
        val tasks = repository.observeTasks().first()
        assertEquals(3, tasks.size)
        assertEquals("Renamed", tasks.first { it.id == created.id }.title)
    }

    @Test
    fun `editing a completed task does not silently un-complete it`() = runTest {
        val repository = repository()
        repository.refresh()
        repository.setCompleted("1", completed = true).getOrThrow()

        val updated = repository.updateTask("1", draft("Renamed")).getOrThrow()

        assertEquals(true, updated.isCompleted)
        assertEquals(true, repository.observeTasks().first().first { it.id == "1" }.isCompleted)
    }

    @Test
    fun `an untyped transport failure is wrapped as a DataException`() = runTest {
        api.nextThrowable = IllegalStateException("socket closed")
        val repository = repository()

        val result = repository.refresh()

        assertEquals(DataError.Unknown, result.dataError())
    }

    @Test
    fun `getTask does not insert a row the list never loaded`() = runTest {
        val repository = repository()

        assertTrue(repository.getTask("1").isSuccess)
        assertEquals(emptyList<String>(), repository.observeTasks().first().map { it.id })
    }

    private fun TestScope.repository() =
        DefaultTaskRepository(api, cache, StandardTestDispatcher(testScheduler))

    private fun Result<*>.dataError(): DataError {
        val failure = requireNotNull(exceptionOrNull()) { "expected the call to fail" }
        assertTrue("expected a DataException, got $failure", failure is DataException)
        return (failure as DataException).error
    }

    private fun draft(title: String) = TaskDraft(title = title, notes = null, priority = TaskPriority.LOW)

    private fun dto(
        id: String,
        title: String = "Task $id",
        priority: String = "HIGH",
        completed: Boolean = false,
    ) = TaskDto(
        id = id,
        title = title,
        notes = null,
        priority = priority,
        completed = completed,
        createdAtEpochMillis = 0L,
    )

    /** No latency, no dice: the repository's rules are what is under test, not the source's realism. */
    private inner class RecordingTaskApi : TaskApi {

        val store = linkedMapOf(
            "1" to dto(id = "1", priority = "HIGH"),
            "2" to dto(id = "2", priority = "LOW"),
        )

        var nextError: DataError? = null
        var nextThrowable: Throwable? = null
        private var lastId = 2

        override suspend fun getTasks(): List<TaskDto> = guard { store.values.toList() }

        override suspend fun getTask(id: String): TaskDto = guard { store.require(id) }

        override suspend fun createTask(payload: TaskPayload): TaskDto = guard {
            val created = TaskDto(
                id = "${++lastId}",
                title = payload.title,
                notes = payload.notes,
                priority = payload.priority,
                completed = payload.completed,
                createdAtEpochMillis = 0L,
            )
            store[created.id] = created
            created
        }

        override suspend fun updateTask(id: String, payload: TaskPayload): TaskDto = guard {
            val updated = store.require(id).copy(
                title = payload.title,
                notes = payload.notes,
                priority = payload.priority,
                completed = payload.completed,
            )
            store[id] = updated
            updated
        }

        override suspend fun deleteTask(id: String) = guard {
            store.require(id)
            store.remove(id)
            Unit
        }

        private fun <T> guard(block: () -> T): T {
            nextThrowable?.let { throwable ->
                nextThrowable = null
                throw throwable
            }
            nextError?.let { error ->
                nextError = null
                throw DataException(error)
            }
            return block()
        }

        private fun Map<String, TaskDto>.require(id: String): TaskDto =
            this[id] ?: throw DataException(DataError.NotFound)
    }
}
