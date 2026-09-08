package com.rounds.test.to_dolist.feature.taskeditor

import androidx.lifecycle.SavedStateHandle
import com.rounds.test.to_dolist.core.testing.FakeTaskRepository
import com.rounds.test.to_dolist.core.testing.MainDispatcherRule
import com.rounds.test.to_dolist.core.testing.TestData
import com.rounds.test.to_dolist.tasks.error.DataError
import com.rounds.test.to_dolist.tasks.model.TaskPriority
import com.rounds.test.to_dolist.tasks.usecase.GetTaskUseCase
import com.rounds.test.to_dolist.tasks.usecase.SaveTaskUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * One test class for both modes, because there is one ViewModel for both modes: what separates them
 * is the navigation argument and nothing else, and that is what the two `viewModel(...)` factories
 * below vary.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TaskEditorViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeTaskRepository()

    @Test
    fun `create mode starts on an empty form and asks the source for nothing`() = runTest {
        val viewModel = createMode()

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isEditing)
        assertFalse(state.isLoading)
        assertEquals("", state.title)
        assertEquals("", state.notes)
        assertEquals(TaskPriority.MEDIUM, state.priority)
        assertFalse(state.canSave)
    }

    @Test
    fun `edit mode loads and seeds the form from the task`() = runTest {
        val existing = TestData.tasks.first { it.notes != null }
        val viewModel = editMode(existing.id)

        assertTrue(viewModel.uiState.value.isLoading)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.isEditing)
        assertEquals(existing.title, state.title)
        assertEquals(existing.notes, state.notes)
        assertEquals(existing.priority, state.priority)
        assertNull(state.error)
    }

    @Test
    fun `a task with no notes seeds an empty field rather than the word null`() = runTest {
        val existing = TestData.tasks.first { it.notes == null }
        val viewModel = editMode(existing.id)

        advanceUntilIdle()

        assertEquals("", viewModel.uiState.value.notes)
    }

    @Test
    fun `a failed load shows an error and retry recovers`() = runTest {
        repository.nextError = DataError.Network
        val viewModel = editMode("1")

        advanceUntilIdle()
        assertEquals(DataError.Network, viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isLoading)

        viewModel.onRetry()
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.error)
        assertEquals(TestData.tasks.first { it.id == "1" }.title, viewModel.uiState.value.title)
    }

    @Test
    fun `a blank title is rejected by the use case and marked on the field`() = runTest {
        val viewModel = createMode()
        viewModel.onTitleChange("   ")

        viewModel.onSave()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.titleError)
        assertFalse(state.isSaved)
        assertNull(state.saveError)
        assertTrue(repository.observeTasks().first().isEmpty())
    }

    @Test
    fun `saving in create mode reaches the source and reports success once`() = runTest {
        val viewModel = createMode()
        viewModel.onTitleChange("Write the walkthrough")
        viewModel.onNotesChange("Ten minutes")
        viewModel.onPriorityChange(TaskPriority.HIGH)

        viewModel.onSave()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isSaved)
        assertFalse(state.isSaving)

        val saved = repository.observeTasks().first().single()
        assertEquals("Write the walkthrough", saved.title)
        assertEquals("Ten minutes", saved.notes)
        assertEquals(TaskPriority.HIGH, saved.priority)
    }

    @Test
    fun `saving in edit mode updates the task in place`() = runTest {
        repository.refresh()
        val viewModel = editMode("1")
        advanceUntilIdle()

        viewModel.onTitleChange("Renamed")
        viewModel.onSave()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isSaved)
        val tasks = repository.observeTasks().first()
        assertEquals(TestData.tasks.size, tasks.size)
        assertEquals("Renamed", tasks.first { it.id == "1" }.title)
    }

    @Test
    fun `a failed save keeps the form and does not navigate away`() = runTest {
        val viewModel = createMode()
        viewModel.onTitleChange("Book the venue")
        viewModel.onNotesChange("Before Friday")
        repository.nextError = DataError.Conflict

        viewModel.onSave()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(DataError.Conflict, state.saveError)
        assertFalse(state.isSaved)
        assertFalse(state.isSaving)
        assertEquals("Book the venue", state.title)
        assertEquals("Before Friday", state.notes)
    }

    @Test
    fun `a shown save error is cleared so a rotation does not repeat it`() = runTest {
        val viewModel = createMode()
        viewModel.onTitleChange("Book the venue")
        repository.nextError = DataError.Conflict
        viewModel.onSave()
        advanceUntilIdle()

        viewModel.onSaveErrorShown()

        assertNull(viewModel.uiState.value.saveError)
    }

    // --- NFR-07: the form survives the process being killed ------------------------------------

    @Test
    fun `field edits are written through to the handle`() = runTest {
        val handle = handle(taskId = null)
        val viewModel = viewModel(handle)

        viewModel.onTitleChange("Half typed")
        viewModel.onNotesChange("and a note")
        viewModel.onPriorityChange(TaskPriority.HIGH)

        assertEquals("Half typed", handle.get<String>("form_title"))
        assertEquals("and a note", handle.get<String>("form_notes"))
        assertEquals("HIGH", handle.get<String>("form_priority"))
    }

    @Test
    fun `a recreated process restores the form the user had typed`() = runTest {
        val restored = viewModel(
            handle(taskId = null).apply {
                set("form_title", "Half typed")
                set("form_notes", "and a note")
                set("form_priority", "HIGH")
            },
        )

        advanceUntilIdle()

        val state = restored.uiState.value
        assertEquals("Half typed", state.title)
        assertEquals("and a note", state.notes)
        assertEquals(TaskPriority.HIGH, state.priority)
    }

    /**
     * The saved form is newer than anything the source holds, so re-running the load would overwrite
     * the user's work with the server's values the moment the process came back.
     */
    @Test
    fun `a restored edit form is not reloaded over`() = runTest {
        val restored = viewModel(
            handle(taskId = "1").apply {
                set("form_title", "Half typed")
                set("form_notes", "")
                set("form_priority", "MEDIUM")
            },
        )

        assertFalse(restored.uiState.value.isLoading)
        advanceUntilIdle()

        assertEquals("Half typed", restored.uiState.value.title)
        assertTrue(restored.uiState.value.isEditing)
    }

    @Test
    fun `a loaded form is remembered too, so it survives a kill before the first keystroke`() = runTest {
        val handle = handle(taskId = "2")
        viewModel(handle)

        advanceUntilIdle()

        val existing = TestData.tasks.first { it.id == "2" }
        assertEquals(existing.title, handle.get<String>("form_title"))
        assertEquals(existing.priority.name, handle.get<String>("form_priority"))
    }

    private fun createMode() = viewModel(handle(taskId = null))

    private fun editMode(taskId: String) = viewModel(handle(taskId = taskId))

    private fun handle(taskId: String?) = SavedStateHandle(mapOf("taskId" to taskId))

    private fun viewModel(savedStateHandle: SavedStateHandle) = TaskEditorViewModel(
        savedStateHandle = savedStateHandle,
        getTask = GetTaskUseCase(repository),
        saveTask = SaveTaskUseCase(repository),
    )
}
