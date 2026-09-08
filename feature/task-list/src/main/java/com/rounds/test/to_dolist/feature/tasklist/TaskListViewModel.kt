package com.rounds.test.to_dolist.feature.tasklist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rounds.test.to_dolist.tasks.error.asDataError
import com.rounds.test.to_dolist.tasks.model.TaskSort
import com.rounds.test.to_dolist.tasks.usecase.DeleteTaskUseCase
import com.rounds.test.to_dolist.tasks.usecase.ObserveTasksUseCase
import com.rounds.test.to_dolist.tasks.usecase.RefreshTasksUseCase
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

    fun onToggleCompleted(id: String, completed: Boolean) {
        viewModelScope.launch {
            toggleCompleted(id, completed).onFailure(::reportFailure)
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

    fun onUndoDelete(message: TaskListMessage.TaskDeleted) {
        _uiState.update { it.copy(message = null) }

        viewModelScope.launch {
            restoreTask(message.task).onFailure(::reportFailure)
        }
    }

    /** Called once the message has been shown, so a configuration change does not replay it. */
    fun onMessageShown() {
        _uiState.update { it.copy(message = null) }
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
                state.copy(message = TaskListMessage.Failure(error))
            }
        }
    }

    private fun post(message: TaskListMessage) {
        _uiState.update { it.copy(message = message) }
    }
}
