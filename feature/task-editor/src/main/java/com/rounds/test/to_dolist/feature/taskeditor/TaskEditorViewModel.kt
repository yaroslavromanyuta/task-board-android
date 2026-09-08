package com.rounds.test.to_dolist.feature.taskeditor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.navigation.toRoute
import com.rounds.test.to_dolist.navigation.Route
import com.rounds.test.to_dolist.tasks.usecase.GetTaskUseCase
import com.rounds.test.to_dolist.tasks.usecase.SaveTaskUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

/**
 * Reads its argument from [SavedStateHandle] rather than from a composable parameter, so the screen
 * survives process death without `:app` having to re-supply anything.
 *
 * Field edits are handled here already — the form is genuinely usable — while everything that needs
 * the data layer is left marked. That keeps the skeleton navigable instead of crashing on first tap.
 */
@HiltViewModel
class TaskEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getTask: GetTaskUseCase,
    private val saveTask: SaveTaskUseCase,
) : ViewModel() {

    private val route: Route.TaskEditor = savedStateHandle.toRoute()

    private val _uiState = MutableStateFlow(TaskEditorUiState(taskId = route.taskId))
    val uiState: StateFlow<TaskEditorUiState> = _uiState.asStateFlow()

    init {
        // TODO(data layer): when route.taskId != null, load it via getTask() and seed the form.
    }

    fun onTitleChange(value: String) {
        _uiState.update { it.copy(title = value, titleError = false) }
    }

    fun onNotesChange(value: String) {
        _uiState.update { it.copy(notes = value) }
    }

    fun onPriorityChange(value: com.rounds.test.to_dolist.tasks.model.TaskPriority) {
        _uiState.update { it.copy(priority = value) }
    }

    /**
     * @param onSaved invoked once the task is persisted; navigation is the caller's business, not the
     * ViewModel's.
     */
    fun onSave(onSaved: () -> Unit) {
        val state = _uiState.value
        if (state.title.isBlank()) {
            _uiState.update { it.copy(titleError = true) }
            return
        }
        // TODO(data layer): call saveTask(state.taskId, draft) and only report success on Result.success.
        onSaved()
    }

    fun onRetry() {
        // TODO(data layer): re-run the initial load for the edit case.
    }
}
