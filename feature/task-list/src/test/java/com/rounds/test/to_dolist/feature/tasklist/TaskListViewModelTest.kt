package com.rounds.test.to_dolist.feature.tasklist

import app.cash.turbine.test
import com.rounds.test.to_dolist.core.testing.FakeTaskRepository
import com.rounds.test.to_dolist.core.testing.MainDispatcherRule
import com.rounds.test.to_dolist.core.testing.TestData
import com.rounds.test.to_dolist.tasks.error.DataError
import com.rounds.test.to_dolist.tasks.error.DataException
import com.rounds.test.to_dolist.tasks.model.TaskPriority
import com.rounds.test.to_dolist.tasks.model.TaskSort
import com.rounds.test.to_dolist.tasks.repository.TaskRepository
import com.rounds.test.to_dolist.tasks.usecase.DeleteTaskUseCase
import com.rounds.test.to_dolist.tasks.usecase.ObserveTasksUseCase
import com.rounds.test.to_dolist.tasks.usecase.RefreshTasksUseCase
import com.rounds.test.to_dolist.tasks.usecase.RestoreTaskUseCase
import com.rounds.test.to_dolist.tasks.usecase.ToggleTaskCompletedUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Every branch of the state table in REQUIREMENTS.md §9 has a case here, including the boundary the
 * table calls out: a failure arriving with rows on screen must be reported without costing the user
 * the list. The ViewModel is exercised against `:core:testing`'s fake, with no data module on the
 * classpath — which is the boundary rules paying for themselves.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TaskListViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `starts loading and then shows the loaded list`() = runTest {
        val viewModel = viewModel()

        viewModel.uiState.test {
            val initial = awaitItem()
            assertTrue(initial.isLoading)
            assertTrue(initial.tasks.isEmpty())
            assertFalse(initial.isEmpty)

            advanceUntilIdle()

            val loaded = expectMostRecentItem()
            assertEquals(TestData.tasks, loaded.tasks)
            assertFalse(loaded.isLoading)
            assertNull(loaded.error)
        }
    }

    @Test
    fun `a source with no tasks renders the empty state`() = runTest {
        val viewModel = viewModel(FakeTaskRepository(source = emptyList()))

        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isEmpty)
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `a failed first load surfaces a typed error and retry recovers`() = runTest {
        val repository = FakeTaskRepository()
        repository.nextError = DataError.Network
        val viewModel = viewModel(repository)

        advanceUntilIdle()
        assertEquals(DataError.Network, viewModel.uiState.value.error)
        assertNull(viewModel.uiState.value.message)
        assertTrue(viewModel.uiState.value.tasks.isEmpty())
        assertFalse(viewModel.uiState.value.isLoading)

        viewModel.onRetry()
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.error)
        assertEquals(TestData.tasks, viewModel.uiState.value.tasks)
    }

    @Test
    fun `toggling a task updates its row`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onToggleCompleted(id = "1", completed = true)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.tasks.first { it.id == "1" }.isCompleted)
    }

    @Test
    fun `deleting a task removes its row`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onDelete(id = "1")
        advanceUntilIdle()

        assertEquals(listOf("2", "3"), viewModel.uiState.value.tasks.map { it.id })
    }

    // --- FR-09 boundary: a failure with content on screen ---------------------------------------

    @Test
    fun `a failed write is reported over the list, not instead of it`() = runTest {
        val repository = FakeTaskRepository()
        val viewModel = viewModel(repository)
        advanceUntilIdle()
        repository.nextError = DataError.Conflict

        viewModel.onDelete(id = "1")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(TaskListMessage.Failure(DataError.Conflict), state.message)
        assertNull(state.error)
        assertEquals(TestData.tasks, state.tasks)
    }

    @Test
    fun `a failed refresh with rows on screen does not blank them`() = runTest {
        val repository = FakeTaskRepository()
        val viewModel = viewModel(repository)
        advanceUntilIdle()
        repository.nextError = DataError.Network

        viewModel.onRetry()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(TaskListMessage.Failure(DataError.Network), state.message)
        assertNull(state.error)
        assertEquals(TestData.tasks, state.tasks)
        assertFalse(state.isLoading)
    }

    @Test
    fun `a shown message is cleared so a rotation does not repeat it`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onDelete(id = "1")
        advanceUntilIdle()
        viewModel.onMessageShown()

        assertNull(viewModel.uiState.value.message)
    }

    // --- FR-12: undo ----------------------------------------------------------------------------

    @Test
    fun `a successful delete offers the task back`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()
        val deleted = TestData.tasks.first { it.id == "1" }

        viewModel.onDelete(id = "1")
        advanceUntilIdle()

        assertEquals(TaskListMessage.TaskDeleted(deleted), viewModel.uiState.value.message)
    }

    @Test
    fun `undo restores the task with its fields intact`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onDelete(id = "2")
        advanceUntilIdle()
        val message = viewModel.uiState.value.message as TaskListMessage.TaskDeleted

        viewModel.onUndoDelete(message)
        advanceUntilIdle()

        val restored = viewModel.uiState.value.tasks.first { it.title == "Renew passport" }
        assertEquals("Book a slot first", restored.notes)
        assertEquals(TaskPriority.HIGH, restored.priority)
        assertEquals(3, viewModel.uiState.value.tasks.size)
        assertNull(viewModel.uiState.value.message)
    }

    @Test
    fun `undoing a completed task restores it completed`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onDelete(id = "3")
        advanceUntilIdle()
        viewModel.onUndoDelete(viewModel.uiState.value.message as TaskListMessage.TaskDeleted)
        advanceUntilIdle()

        val restored = viewModel.uiState.value.tasks.first { it.title == "Water the plants" }
        assertTrue(restored.isCompleted)
    }

    @Test
    fun `a failed undo is reported and the offer comes back`() = runTest {
        val repository = FakeTaskRepository()
        val viewModel = viewModel(repository)
        advanceUntilIdle()

        viewModel.onDelete(id = "1")
        advanceUntilIdle()
        val message = viewModel.uiState.value.message as TaskListMessage.TaskDeleted
        repository.nextError = DataError.Network

        viewModel.onUndoDelete(message)
        advanceUntilIdle()

        // The failure is read first and the offer waits behind it: the message carries the only copy
        // of the task, so a restore that failed must not be the end of it.
        assertEquals(TaskListMessage.Failure(DataError.Network), viewModel.uiState.value.message)
        assertEquals(listOf("2", "3"), viewModel.uiState.value.tasks.map { it.id })

        viewModel.onMessageShown()

        assertEquals(message, viewModel.uiState.value.message)
    }

    @Test
    fun `the second offer restores the task`() = runTest {
        val repository = FakeTaskRepository()
        val viewModel = viewModel(repository)
        advanceUntilIdle()

        viewModel.onDelete(id = "1")
        advanceUntilIdle()
        repository.nextError = DataError.Network
        viewModel.onUndoDelete(viewModel.uiState.value.message as TaskListMessage.TaskDeleted)
        advanceUntilIdle()
        viewModel.onMessageShown()

        viewModel.onUndoDelete(viewModel.uiState.value.message as TaskListMessage.TaskDeleted)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.tasks.any { it.title == "Buy milk" })
        assertNull(viewModel.uiState.value.message)
    }

    /**
     * An undo that re-created the task but could not re-apply its completion flag is not a failure:
     * the row is back. Reporting it as one describes a restore that visibly happened as one that did
     * not, and says nothing about the field that was actually lost.
     */
    @Test
    fun `a restore that loses the completion flag says so`() = runTest {
        val viewModel = viewModel(SetCompletedAlwaysFails(FakeTaskRepository()))
        advanceUntilIdle()

        viewModel.onDelete(id = "3")
        advanceUntilIdle()
        viewModel.onUndoDelete(viewModel.uiState.value.message as TaskListMessage.TaskDeleted)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(TaskListMessage.CompletionNotRestored(DataError.Network), state.message)
        assertFalse(state.tasks.first { it.title == "Water the plants" }.isCompleted)
    }

    // --- Messages queue rather than overwrite ----------------------------------------------------

    /**
     * The deleted task exists only inside its message, so a failure landing during the few seconds an
     * undo offer is up used to take the task with it - and at a 15% failure rate that needs the user
     * to do nothing at all.
     */
    @Test
    fun `a failure does not displace a pending undo offer`() = runTest {
        val repository = FakeTaskRepository()
        val viewModel = viewModel(repository)
        advanceUntilIdle()

        viewModel.onDelete(id = "1")
        advanceUntilIdle()
        val offer = viewModel.uiState.value.message as TaskListMessage.TaskDeleted

        repository.nextError = DataError.Network
        viewModel.onToggleCompleted(id = "2", completed = true)
        advanceUntilIdle()

        assertEquals(offer, viewModel.uiState.value.message)

        viewModel.onMessageShown()

        assertEquals(TaskListMessage.Failure(DataError.Network), viewModel.uiState.value.message)
    }

    @Test
    fun `an identical failure is not queued twice`() = runTest {
        val repository = FakeTaskRepository()
        val viewModel = viewModel(repository)
        advanceUntilIdle()

        repository.nextError = DataError.Network
        viewModel.onRetry()
        advanceUntilIdle()
        repository.nextError = DataError.Network
        viewModel.onRetry()
        advanceUntilIdle()

        assertEquals(
            listOf(TaskListMessage.Failure(DataError.Network)),
            viewModel.uiState.value.messages,
        )
    }

    // --- A write already in flight ---------------------------------------------------------------

    /**
     * The checkbox renders the cache, which does not move until the write returns, so a second tap
     * inside that window read the same stale value and sent the same thing again: two taps, one
     * meaning. The row is held for the duration and the screen renders its control inert.
     */
    @Test
    fun `a second toggle while the first is in flight is ignored`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onToggleCompleted(id = "1", completed = true)
        assertTrue("1" in viewModel.uiState.value.pendingToggles)

        viewModel.onToggleCompleted(id = "1", completed = true)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.tasks.first { it.id == "1" }.isCompleted)
        assertTrue(viewModel.uiState.value.pendingToggles.isEmpty())
    }

    @Test
    fun `a toggle that fails still releases the row`() = runTest {
        val repository = FakeTaskRepository()
        val viewModel = viewModel(repository)
        advanceUntilIdle()
        repository.nextError = DataError.Network

        viewModel.onToggleCompleted(id = "1", completed = true)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.pendingToggles.isEmpty())
        assertEquals(TaskListMessage.Failure(DataError.Network), viewModel.uiState.value.message)
    }

    // --- A save made under an active search ------------------------------------------------------

    /**
     * The editor closing is the only "saved" signal the app has, so a save whose title does not match
     * the live query used to close onto "No tasks match ..." - the one picture that says the task is
     * not there, shown straight after the action that created it.
     */
    @Test
    fun `a save clears the query so the new task is visible`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()
        viewModel.onQueryChange("invoice")
        assertTrue(viewModel.uiState.value.hasNoMatches)

        viewModel.onTaskSaved()

        assertEquals("", viewModel.uiState.value.query)
        assertFalse(viewModel.uiState.value.hasNoMatches)
    }

    @Test
    fun `a query typed after a save is left alone`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onTaskSaved()
        viewModel.onQueryChange("passport")

        assertEquals("passport", viewModel.uiState.value.query)
    }

    // --- FR-10 and FR-11: search and sort --------------------------------------------------------

    @Test
    fun `typing narrows the list and clearing restores it`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onQueryChange("passport")
        assertEquals(listOf("2"), viewModel.uiState.value.visibleTasks.map { it.id })
        assertEquals(TestData.tasks, viewModel.uiState.value.tasks)

        viewModel.onQueryChange("")
        assertEquals(TestData.tasks, viewModel.uiState.value.visibleTasks)
    }

    @Test
    fun `the query ignores case`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onQueryChange("PASSPORT")

        assertEquals(listOf("2"), viewModel.uiState.value.visibleTasks.map { it.id })
    }

    @Test
    fun `a query matching nothing is not the same state as having no tasks`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onQueryChange("invoice")

        val state = viewModel.uiState.value
        assertTrue(state.hasNoMatches)
        assertFalse(state.isEmpty)
    }

    @Test
    fun `sorting by priority reorders without touching the source`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onSortChange(TaskSort.PRIORITY)

        val state = viewModel.uiState.value
        assertEquals(listOf("2", "3", "1"), state.visibleTasks.map { it.id })
        assertEquals(listOf("1", "2", "3"), state.tasks.map { it.id })
    }

    @Test
    fun `sorting by completion puts unfinished tasks first`() = runTest {
        val completedFirst = listOf(
            TestData.task(id = "1", isCompleted = true),
            TestData.task(id = "2"),
        )
        val viewModel = viewModel(FakeTaskRepository(source = completedFirst))
        advanceUntilIdle()

        viewModel.onSortChange(TaskSort.COMPLETION)

        assertEquals(listOf("2", "1"), viewModel.uiState.value.visibleTasks.map { it.id })
    }

    @Test
    fun `search and sort compose`() = runTest {
        val source = listOf(
            TestData.task(id = "1", title = "Task alpha", priority = TaskPriority.LOW),
            TestData.task(id = "2", title = "Task beta", priority = TaskPriority.HIGH),
            TestData.task(id = "3", title = "Other", priority = TaskPriority.HIGH),
        )
        val viewModel = viewModel(FakeTaskRepository(source = source))
        advanceUntilIdle()

        viewModel.onQueryChange("task")
        viewModel.onSortChange(TaskSort.PRIORITY)

        assertEquals(listOf("2", "1"), viewModel.uiState.value.visibleTasks.map { it.id })
    }

    /** Lets the second half of a restore fail on its own, which is how the flag gets lost. */
    private class SetCompletedAlwaysFails(
        delegate: TaskRepository,
    ) : TaskRepository by delegate {
        override suspend fun setCompleted(id: String, completed: Boolean): Result<Unit> =
            Result.failure(DataException(DataError.Network))
    }

    private fun viewModel(repository: TaskRepository = FakeTaskRepository()) = TaskListViewModel(
        observeTasks = ObserveTasksUseCase(repository),
        refreshTasks = RefreshTasksUseCase(repository),
        toggleCompleted = ToggleTaskCompletedUseCase(repository),
        deleteTask = DeleteTaskUseCase(repository),
        restoreTask = RestoreTaskUseCase(repository),
    )
}
