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
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * "Restore" is two calls, not one, because the source has no restore operation and `TaskDraft` cannot
 * carry `isCompleted`. These tests are what stop that second call being dropped as redundant.
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

        val restored = restoreTask(deleted).getOrThrow()

        assertEquals("Book dentist", restored.title)
        assertEquals("Before Friday", restored.notes)
        assertEquals(TaskPriority.HIGH, restored.priority)
        assertTrue(repository.observeTasks().first().any { it.id == restored.id })
    }

    @Test
    fun `the source assigns a new id, because it never knew the old one came back`() = runTest {
        val deleted = TestData.task(id = "gone")

        val restored = restoreTask(deleted).getOrThrow()

        assertNotEquals("gone", restored.id)
    }

    @Test
    fun `a completed task comes back completed`() = runTest {
        val deleted = TestData.task(id = "gone", isCompleted = true)

        val restored = restoreTask(deleted).getOrThrow()

        assertTrue(restored.isCompleted)
        assertTrue(repository.observeTasks().first().first { it.id == restored.id }.isCompleted)
    }

    @Test
    fun `a failure to re-create is reported and nothing is stored`() = runTest {
        repository.nextError = DataError.Network

        val result = restoreTask(TestData.task(id = "gone"))

        assertEquals(DataError.Network, (result.exceptionOrNull() as DataException).error)
        assertTrue(repository.observeTasks().first().isEmpty())
    }

    /**
     * The completion half can fail on its own. The task is back either way, so the caller is told and
     * can decide; silently returning success would claim a flag that is not set.
     */
    @Test
    fun `a failure to re-apply completion is reported`() = runTest {
        val restore = RestoreTaskUseCase(SetCompletedAlwaysFails(repository))

        val result = restore(TestData.task(id = "gone", isCompleted = true))

        assertEquals(DataError.Network, (result.exceptionOrNull() as DataException).error)
        assertEquals(1, repository.observeTasks().first().size)
    }

    private class SetCompletedAlwaysFails(
        delegate: TaskRepository,
    ) : TaskRepository by delegate {
        override suspend fun setCompleted(id: String, completed: Boolean): Result<Unit> =
            Result.failure(DataException(DataError.Network))
    }
}
