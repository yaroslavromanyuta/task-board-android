package com.rounds.test.to_dolist.feature.tasklist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rounds.test.to_dolist.tasks.error.asDataError
import com.rounds.test.to_dolist.tasks.model.TaskSort
import com.rounds.test.to_dolist.tasks.usecase.DeleteTaskUseCase
import com.rounds.test.to_dolist.tasks.usecase.ObserveTasksUseCase
import com.rounds.test.to_dolist.tasks.usecase.RefreshTasksUseCase
import com.rounds.test.to_dolist.tasks.usecase.RestoreOutcome
import com.rounds.test.to_dolist.tasks.usecase.RestoreTaskUseCase
import com.rounds.test.to_dolist.tasks.usecase.ToggleTaskCompletedUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Owns the list state. Rows arrive from the cache stream and never from a call's return value, so
 * whatever a write produced is on screen because the source has it, not because this class assumed it.
 *
 * Failures are routed by one rule, which is REQUIREMENTS.md §9's boundary case stated directly: if
 * there is content on screen the failure passes over it as a [TaskListMessage], and if there is not,
 * it becomes the screen. A flaky moment must not cost the user the list they were reading.
 *
 * Messages queue rather than overwrite. A [TaskListMessage.TaskDeleted] is the only copy of a deleted
 * task, so the rule is that nothing may displace one that has not been acted on — not an unrelated
 * failure, and not the failure of the undo itself.
 *
 * Search and sort are derived in [TaskListUiState] from the cached list, never by re-querying the
 * source: they are a way of looking at what is already here, not a different request.
 */
@HiltViewModel
class TaskListViewModel @Inject constructor(
    private val observeTasks: ObserveTasksUseCase,
    private val refreshTasks: RefreshTasksUseCase,
    private val toggleCompleted: ToggleTaskCompletedUseCase,
    private val deleteTask: DeleteTaskUseCase,
    private val restoreTask: RestoreTaskUseCase,
) : ViewModel() {

    /** Starts loading rather than empty: the first refresh is already on its way, and an empty state
     * that flashes for one frame before the list arrives is a worse lie than a spinner. */
    private val _uiState = MutableStateFlow(TaskListUiState(isLoading = true))
    val uiState: StateFlow<TaskListUiState> = _uiState.asStateFlow()

    init {
        observeTasks()
            .onEach { tasks -> _uiState.update { it.copy(tasks = tasks) } }
            .launchIn(viewModelScope)

        refresh()
    }

    fun onRetry() = refresh()

    fun onQueryChange(query: String) {
        _uiState.update { it.copy(query = query) }
    }

    fun onSortChange(sort: TaskSort) {
        _uiState.update { it.copy(sort = sort) }
    }

    /**
     * A save landed while this screen was in the back stack. The query is cleared, because the editor
     * closing is the app's only "saved" signal and a new task whose title does not match the live
     * filter would otherwise arrive into a screen reading "No tasks match ..." — the same picture a
     * failed save paints.
     *
     * The result itself is read from the back stack entry by `taskListSection` and handed here. It
     * cannot be read from an injected `SavedStateHandle`: Hilt builds the ViewModel its own handle,
     * and a value written to `NavBackStackEntry.savedStateHandle` never reaches it.
     */
    fun onTaskSaved() {
        _uiState.update { it.copy(query = "") }
    }

    /**
     * The checkbox renders the cache and the cache does not move until the write returns, so a second
     * tap inside the same call would read the same stale value and send it again — two taps meaning
     * one write. The id is held for the duration and the row's control goes inert instead, which is
     * the shape `TaskEditorViewModel.onSave` already uses against the same race.
     */
    fun onToggleCompleted(id: String, completed: Boolean) {
        if (id in _uiState.value.pendingToggles) return
        _uiState.update { it.copy(pendingToggles = it.pendingToggles + id) }

        viewModelScope.launch {
            try {
                toggleCompleted(id, completed).onFailure(::reportFailure)
            } finally {
                _uiState.update { it.copy(pendingToggles = it.pendingToggles - id) }
            }
        }
    }

    /**
     * The task is captured before the call, not looked up after it: once the delete succeeds it is
     * gone from the cache, and Undo needs every field back.
     */
    fun onDelete(id: String) {
        val deleted = _uiState.value.tasks.firstOrNull { it.id == id } ?: return

        viewModelScope.launch {
            deleteTask(id).fold(
                onSuccess = { post(TaskListMessage.TaskDeleted(deleted)) },
                onFailure = ::reportFailure,
            )
        }
    }

    /**
     * The offer is consumed on the way in and re-posted if the restore fails. The message carries the
     * only copy of the task left anywhere, so dropping it before the call succeeded is what made a
     * failed undo lose the task outright — at the shipped 15% failure rate, about one undo in seven.
     *
     * A restore that re-created the task but lost its completion flag is reported as itself and not as
     * a failure: the row is back, and what the user has to be told is which field is not.
     */
    fun onUndoDelete(message: TaskListMessage.TaskDeleted) {
        consume(message)

        viewModelScope.launch {
            restoreTask(message.task).fold(
                onSuccess = { outcome ->
                    if (outcome is RestoreOutcome.CompletionLost) {
                        post(TaskListMessage.CompletionNotRestored(outcome.error))
                    }
                },
                onFailure = { throwable ->
                    reportFailure(throwable)
                    post(message)
                },
            )
        }
    }

    /** Called once the message has been shown, so a configuration change does not replay it. */
    fun onMessageShown() {
        _uiState.update { it.copy(messages = it.messages.drop(1)) }
    }

    private fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            refreshTasks().fold(
                onSuccess = { _uiState.update { it.copy(isLoading = false, error = null) } },
                onFailure = { throwable ->
                    _uiState.update { it.copy(isLoading = false) }
                    reportFailure(throwable)
                },
            )
        }
    }

    /**
     * With rows on screen a failure is reported and dismissed; with nothing on screen there is nothing
     * to preserve, so it takes the screen and offers a Retry.
     */
    private fun reportFailure(throwable: Throwable) {
        val error = throwable.asDataError()
        _uiState.update { state ->
            if (state.tasks.isEmpty()) {
                state.copy(error = error)
            } else {
                state.copy(messages = state.messages.plusDistinct(TaskListMessage.Failure(error)))
            }
        }
    }

    private fun post(message: TaskListMessage) {
        _uiState.update { it.copy(messages = it.messages.plusDistinct(message)) }
    }

    private fun consume(message: TaskListMessage) {
        _uiState.update { it.copy(messages = it.messages - message) }
    }

    /**
     * A message equal to one already waiting is dropped rather than queued twice: repeating "No
     * connection" once per lost dice roll is noise, and two identical entries in a row would also
     * leave the snackbar's key unchanged and the second of them unshown.
     */
    private fun List<TaskListMessage>.plusDistinct(message: TaskListMessage): List<TaskListMessage> =
        if (message in this) this else this + message
}
