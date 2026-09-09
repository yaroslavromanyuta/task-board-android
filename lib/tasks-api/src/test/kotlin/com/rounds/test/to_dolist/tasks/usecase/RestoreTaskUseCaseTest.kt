package com.rounds.test.to_dolist.tasks.usecase

import com.rounds.test.to_dolist.core.testing.FakeTaskRepository
import com.rounds.test.to_dolist.core.testing.TestData
import com.rounds.test.to_dolist.tasks.error.DataError
import com.rounds.test.to_dolist.tasks.error.DataException
import com.rounds.test.to_dolist.tasks.model.TaskPriority
import com.rounds.test.to_dolist.tasks.repository.TaskRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * "Restore" is two calls, not one, because the source has no restore operation and `TaskDraft` cannot
 * carry `isCompleted`. These tests are what stop that second call being dropped as redundant — and
 * what pin the third answer the pair can produce: the task is back, and a field of it is not.
 */
class RestoreTaskUseCaseTest {

    private val repository = FakeTaskRepository()
    private val restoreTask = RestoreTaskUseCase(repository)

    @Test
    fun `an unfinished task comes back with its fields`() = runTest {
        val deleted = TestData.task(
            id = "gone",
            title = "Book dentist",
            notes = "Before Friday",
            priority = TaskPriority.HIGH,
        )

        val outcome = restoreTask(deleted).getOrThrow()

        assertTrue(outcome is RestoreOutcome.Restored)
        assertEquals("Book dentist", outcome.task.title)
        assertEquals("Before Friday", outcome.task.notes)
        assertEquals(TaskPriority.HIGH, outcome.task.priority)
        assertTrue(repository.observeTasks().first().any { it.id == outcome.task.id })
    }

    @Test
    fun `the source assigns a new id, because it never knew the old one came back`() = runTest {
        val deleted = TestData.task(id = "gone")

        val outcome = restoreTask(deleted).getOrThrow()

        assertNotEquals("gone", outcome.task.id)
    }

    @Test
    fun `a completed task comes back completed`() = runTest {
        val deleted = TestData.task(id = "gone", isCompleted = true)

        val outcome = restoreTask(deleted).getOrThrow()

        assertTrue(outcome is RestoreOutcome.Restored)
        assertTrue(outcome.task.isCompleted)
        assertTrue(repository.observeTasks().first().first { it.id == outcome.task.id }.isCompleted)
    }

    @Test
    fun `a failure to re-create is reported and nothing is stored`() = runTest {
        repository.nextError = DataError.Network

        val result = restoreTask(TestData.task(id = "gone"))

        assertEquals(DataError.Network, (result.exceptionOrNull() as DataException).error)
        assertTrue(repository.observeTasks().first().isEmpty())
    }

    /**
     * The completion half can fail on its own, and the two calls cannot be made atomic without a
     * restore operation on the source. So the task is back either way — reporting that as a plain
     * failure would contradict the row the user can see, and would say nothing about which field went
     * missing. The caller is handed both facts and decides how to word them.
     */
    @Test
    fun `a task whose completion could not be re-applied comes back as a partial restore`() = runTest {
        val restore = RestoreTaskUseCase(SetCompletedAlwaysFails(repository))

        val outcome = restore(TestData.task(id = "gone", isCompleted = true)).getOrThrow()

        assertEquals(
            RestoreOutcome.CompletionLost(outcome.task, DataError.Network),
            outcome,
        )
        val stored = repository.observeTasks().first().single()
        assertEquals(outcome.task.id, stored.id)
        assertFalse(stored.isCompleted)
    }

    private class SetCompletedAlwaysFails(
        delegate: TaskRepository,
    ) : TaskRepository by delegate {
        override suspend fun setCompleted(id: String, completed: Boolean): Result<Unit> =
            Result.failure(DataException(DataError.Network))
    }
}
