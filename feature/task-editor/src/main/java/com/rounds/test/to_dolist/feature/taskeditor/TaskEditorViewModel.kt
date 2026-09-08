package com.rounds.test.to_dolist.feature.taskeditor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rounds.test.to_dolist.navigation.Route
import com.rounds.test.to_dolist.tasks.error.ValidationException
import com.rounds.test.to_dolist.tasks.error.asDataError
import com.rounds.test.to_dolist.tasks.model.TaskDraft
import com.rounds.test.to_dolist.tasks.model.TaskPriority
import com.rounds.test.to_dolist.tasks.usecase.GetTaskUseCase
import com.rounds.test.to_dolist.tasks.usecase.SaveTaskUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Reads its argument from [SavedStateHandle] rather than from a composable parameter, so the screen
 * survives process death without `:app` having to re-supply anything. The presence of an id is the
 * only thing that separates "add" from "view / edit": there is no mode flag to get out of step.
 *
 * The form itself is written through to the same handle on every keystroke, so a half-typed task
 * survives the process being killed in the background and not merely a rotation (NFR-07). The
 * alternative, one `getStateFlow` per field, would have turned one state object into a `combine` of
 * four flows to persist three strings.
 *
 * Validation is not repeated here. [SaveTaskUseCase] owns the blank-title rule, and this class maps
 * the failure it returns onto the field the user has to fix — which is why the rule cannot drift
 * between this screen and any future caller.
 */
@HiltViewModel
class TaskEditorViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val getTask: GetTaskUseCase,
    private val saveTask: SaveTaskUseCase,
) : ViewModel() {

    /**
     * Null means "create". Read once here, by the name [Route.TaskEditor] publishes, so the rest of
     * the class never re-derives the mode. `toRoute()` would express the same thing with the route
     * type, but it decodes through an Android runtime and returns nothing in a JVM unit test, which
     * would leave the edit path untested; `RouteTest` guards the name instead.
     */
    private val taskId: String? = savedStateHandle[Route.TaskEditor.TASK_ID_ARG]

    /**
     * True when this instance was rebuilt over a form the user had already filled in — a process
     * recreation rather than a fresh navigation.
     */
    private val hasSavedForm: Boolean = savedStateHandle.contains(KEY_TITLE)

    private val _uiState = MutableStateFlow(
        TaskEditorUiState(
            taskId = taskId,
            title = savedStateHandle[KEY_TITLE] ?: "",
            notes = savedStateHandle[KEY_NOTES] ?: "",
            priority = savedStateHandle.get<String>(KEY_PRIORITY)?.let(TaskPriority::valueOf)
                ?: TaskPriority.MEDIUM,
            isLoading = taskId != null && !hasSavedForm,
        ),
    )
    val uiState: StateFlow<TaskEditorUiState> = _uiState.asStateFlow()

    init {
        // Not reloaded over a restored form: the source's values are older than what the user typed.
        if (!hasSavedForm) taskId?.let(::load)
    }

    fun onTitleChange(value: String) {
        savedStateHandle[KEY_TITLE] = value
        _uiState.update { it.copy(title = value, titleError = false) }
    }

    fun onNotesChange(value: String) {
        savedStateHandle[KEY_NOTES] = value
        _uiState.update { it.copy(notes = value) }
    }

    fun onPriorityChange(value: TaskPriority) {
        savedStateHandle[KEY_PRIORITY] = value.name
        _uiState.update { it.copy(priority = value) }
    }

    /**
     * Success is reported through [TaskEditorUiState.isSaved] rather than a callback: the call is
     * asynchronous, and a lambda invoked from `viewModelScope` would not know whether the screen it
     * was meant to leave is still there. The route watches the flag instead.
     */
    fun onSave() {
        val state = _uiState.value
        if (state.isSaving) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, saveError = null, titleError = false) }

            saveTask(
                id = state.taskId,
                draft = TaskDraft(
                    title = state.title,
                    notes = state.notes,
                    priority = state.priority,
                ),
            ).fold(
                onSuccess = { _uiState.update { it.copy(isSaving = false, isSaved = true) } },
                onFailure = ::onSaveFailed,
            )
        }
    }

    fun onRetry() {
        val taskId = _uiState.value.taskId ?: return
        load(taskId)
    }

    /** Called once the snackbar has been shown, so a configuration change does not show it again. */
    fun onSaveErrorShown() {
        _uiState.update { it.copy(saveError = null) }
    }

    private fun load(taskId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            getTask(taskId).fold(
                onSuccess = { task ->
                    rememberForm(
                        title = task.title,
                        notes = task.notes.orEmpty(),
                        priority = task.priority,
                    )
                    _uiState.update {
                        it.copy(
                            title = task.title,
                            notes = task.notes.orEmpty(),
                            priority = task.priority,
                            isLoading = false,
                        )
                    }
                },
                onFailure = { throwable ->
                    _uiState.update { it.copy(isLoading = false, error = throwable.asDataError()) }
                },
            )
        }
    }

    /**
     * A blank title is the user's to fix in the field they are already looking at; anything else is a
     * failure of the source, and the form survives it untouched.
     */
    /** Keeps the handle in step with a form the user did not type — the values a load just supplied. */
    private fun rememberForm(title: String, notes: String, priority: TaskPriority) {
        savedStateHandle[KEY_TITLE] = title
        savedStateHandle[KEY_NOTES] = notes
        savedStateHandle[KEY_PRIORITY] = priority.name
    }

    private fun onSaveFailed(throwable: Throwable) {
        _uiState.update {
            if (throwable is ValidationException.BlankTitle) {
                it.copy(isSaving = false, titleError = true)
            } else {
                it.copy(isSaving = false, saveError = throwable.asDataError())
            }
        }
    }

    private companion object {
        const val KEY_TITLE = "form_title"
        const val KEY_NOTES = "form_notes"
        const val KEY_PRIORITY = "form_priority"
    }
}
