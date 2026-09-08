package com.rounds.test.to_dolist.feature.tasklist

import app.cash.turbine.test
import com.rounds.test.to_dolist.core.testing.FakeTaskRepository
import com.rounds.test.to_dolist.core.testing.MainDispatcherRule
import com.rounds.test.to_dolist.core.testing.TestData
import com.rounds.test.to_dolist.tasks.error.DataError
import com.rounds.test.to_dolist.tasks.repository.TaskRepository
import com.rounds.test.to_dolist.tasks.usecase.DeleteTaskUseCase
import com.rounds.test.to_dolist.tasks.usecase.ObserveTasksUseCase
import com.rounds.test.to_dolist.tasks.usecase.RefreshTasksUseCase
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
 * Every branch of the state table in REQUIREMENTS.md §9 has a case here. The ViewModel is exercised
 * against `:core:testing`'s fake, with no data module on the classpath — which is the boundary rules
 * paying for themselves.
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

    @Test
    fun `a failed write reports the error without blanking the list`() = runTest {
        val repository = FakeTaskRepository()
        val viewModel = viewModel(repository)
        advanceUntilIdle()
        repository.nextError = DataError.Conflict

        viewModel.onDelete(id = "1")
        advanceUntilIdle()

        assertEquals(DataError.Conflict, viewModel.uiState.value.error)
        assertEquals(TestData.tasks, viewModel.uiState.value.tasks)
    }

    private fun viewModel(repository: TaskRepository = FakeTaskRepository()) = TaskListViewModel(
        observeTasks = ObserveTasksUseCase(repository),
        refreshTasks = RefreshTasksUseCase(repository),
        toggleCompleted = ToggleTaskCompletedUseCase(repository),
        deleteTask = DeleteTaskUseCase(repository),
    )
}
