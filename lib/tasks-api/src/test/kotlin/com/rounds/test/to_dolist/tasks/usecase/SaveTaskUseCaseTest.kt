package com.rounds.test.to_dolist.tasks.usecase

import com.rounds.test.to_dolist.core.testing.FakeTaskRepository
import com.rounds.test.to_dolist.core.testing.TestData
import com.rounds.test.to_dolist.tasks.error.DataError
import com.rounds.test.to_dolist.tasks.error.DataException
import com.rounds.test.to_dolist.tasks.error.ValidationException
import com.rounds.test.to_dolist.tasks.model.TaskDraft
import com.rounds.test.to_dolist.tasks.model.TaskPriority
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Validation and create-vs-update routing are the two rules `SaveTaskUseCase` owns outright, so they
 * are tested here rather than through a ViewModel that could quietly re-implement them.
 */
class SaveTaskUseCaseTest {

    private val repository = FakeTaskRepository()
    private val saveTask = SaveTaskUseCase(repository)

    @Test
    fun `blank title is rejected`() = runTest {
        val result = saveTask(id = null, draft = draft(title = ""))

        assertTrue(result.exceptionOrNull() is ValidationException.BlankTitle)
    }

    @Test
    fun `whitespace-only title is rejected`() = runTest {
        val result = saveTask(id = null, draft = draft(title = "   "))

        assertTrue(result.exceptionOrNull() is ValidationException.BlankTitle)
    }

    @Test
    fun `title is trimmed before it reaches the repository`() = runTest {
        val saved = saveTask(id = null, draft = draft(title = "  Buy milk  ")).getOrThrow()

        assertEquals("Buy milk", saved.title)
    }

    @Test
    fun `blank notes are normalised away`() = runTest {
        val saved = saveTask(id = null, draft = draft(notes = "   ")).getOrThrow()

        assertEquals(null, saved.notes)
    }

    @Test
    fun `a null id creates`() = runTest {
        val before = repository.observeTasks().first().size

        val saved = saveTask(id = null, draft = draft(title = "New task")).getOrThrow()

        assertEquals("New task", saved.title)
        assertEquals(before + 1, repository.observeTasks().first().size)
    }

    @Test
    fun `a non-null id updates in place`() = runTest {
        repository.refresh()
        val existing = TestData.tasks.first()

        val saved = saveTask(id = existing.id, draft = draft(title = "Renamed")).getOrThrow()

        assertEquals(existing.id, saved.id)
        assertEquals("Renamed", saved.title)
        assertEquals(TestData.tasks.size, repository.observeTasks().first().size)
    }

    @Test
    fun `a repository failure is passed through untouched`() = runTest {
        repository.nextError = DataError.Network

        val result = saveTask(id = null, draft = draft())

        assertEquals(DataError.Network, (result.exceptionOrNull() as DataException).error)
    }

    private fun draft(
        title: String = "Task",
        notes: String? = null,
        priority: TaskPriority = TaskPriority.MEDIUM,
    ) = TaskDraft(title = title, notes = notes, priority = priority)
}
