package com.rounds.test.to_dolist.feature.taskeditor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.rounds.test.to_dolist.core.testing.FakeTaskRepository
import com.rounds.test.to_dolist.core.testing.MainDispatcherRule
import com.rounds.test.to_dolist.core.testing.TestData
import com.rounds.test.to_dolist.tasks.error.DataError
import com.rounds.test.to_dolist.tasks.model.TaskPriority
import com.rounds.test.to_dolist.tasks.usecase.GetTaskUseCase
import com.rounds.test.to_dolist.tasks.usecase.SaveTaskUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import java.time.Instant
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

    /** Shares the rule's scheduler, so `advanceUntilIdle()` drives the application-scoped save too. */
    private val applicationScope = CoroutineScope(mainDispatcherRule.testDispatcher)

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

    // --- FR-13: the optional due date ------------------------------------------------------------

    @Test
    fun `a due date is saved and can be cleared again`() = runTest {
        val viewModel = createMode()
        viewModel.onTitleChange("Renew domain")
        val due = Instant.parse("2026-09-11T09:00:00Z")

        viewModel.onDueDateChange(due)
        assertEquals(due, viewModel.uiState.value.dueDate)

        viewModel.onSave()
        advanceUntilIdle()
        assertEquals(due, repository.observeTasks().first().single().dueDate)

        viewModel.onDueDateChange(null)
        assertNull(viewModel.uiState.value.dueDate)
    }

    @Test
    fun `edit mode seeds the due date the task already has`() = runTest {
        val due = Instant.parse("2026-09-11T09:00:00Z")
        val repository = FakeTaskRepository(source = listOf(TestData.task(id = "1", dueDate = due)))
        val viewModel = TaskEditorViewModel(
            savedStateHandle = handle(taskId = "1"),
            applicationScope = applicationScope,
            getTask = GetTaskUseCase(repository),
            saveTask = SaveTaskUseCase(repository),
        )

        advanceUntilIdle()

        assertEquals(due, viewModel.uiState.value.dueDate)
    }

    // --- NFR-07: the form survives the process being killed ------------------------------------

    @Test
    fun `field edits are written through to the handle`() = runTest {
        val handle = handle(taskId = null)
        val viewModel = viewModel(handle)

        viewModel.onTitleChange("Half typed")
        viewModel.onNotesChange("and a note")
        viewModel.onPriorityChange(TaskPriority.HIGH)
        viewModel.onDueDateChange(Instant.parse("2026-09-11T09:00:00Z"))

        assertEquals("Half typed", handle.get<String>("form_title"))
        assertEquals("and a note", handle.get<String>("form_notes"))
        assertEquals("HIGH", handle.get<String>("form_priority"))
        assertEquals(
            Instant.parse("2026-09-11T09:00:00Z").toEpochMilli(),
            handle.get<Long>("form_due_date"),
        )
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

    /**
     * The save has to outlive the screen. Popping the editor clears the ViewModel and cancels
     * `viewModelScope`; when the call lived in that scope it went with it, and the task the user had
     * already committed to was never created and nothing said so.
     */
    @Test
    fun `a save already in flight completes after the screen is left`() = runTest {
        val viewModel = createMode()
        viewModel.onTitleChange("Buy stamps")

        viewModel.onSave()
        viewModel.viewModelScope.cancel()
        advanceUntilIdle()

        assertTrue(repository.observeTasks().first().any { it.title == "Buy stamps" })
    }

    private fun viewModel(savedStateHandle: SavedStateHandle) = TaskEditorViewModel(
        savedStateHandle = savedStateHandle,
        applicationScope = applicationScope,
        getTask = GetTaskUseCase(repository),
        saveTask = SaveTaskUseCase(repository),
    )
}
