package com.rounds.test.to_dolist.feature.tasklist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rounds.test.to_dolist.tasks.error.asDataError
import com.rounds.test.to_dolist.tasks.usecase.DeleteTaskUseCase
import com.rounds.test.to_dolist.tasks.usecase.ObserveTasksUseCase
import com.rounds.test.to_dolist.tasks.usecase.RefreshTasksUseCase
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
 * Errors are written into the state rather than replacing it: `TaskListScreen` only surfaces the error
 * view when there is nothing else to show, so a failure with rows on screen leaves them there
 * (REQUIREMENTS.md §9). Turning that into a snackbar is E5.
 */
@HiltViewModel
class TaskListViewModel @Inject constructor(
    private val observeTasks: ObserveTasksUseCase,
    private val refreshTasks: RefreshTasksUseCase,
    private val toggleCompleted: ToggleTaskCompletedUseCase,
    private val deleteTask: DeleteTaskUseCase,
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

    fun onToggleCompleted(id: String, completed: Boolean) {
        viewModelScope.launch {
            toggleCompleted(id, completed).onFailure(::reportFailure)
        }
    }

    fun onDelete(id: String) {
        viewModelScope.launch {
            deleteTask(id).onFailure(::reportFailure)
        }
    }

    private fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            refreshTasks().fold(
                onSuccess = { _uiState.update { it.copy(isLoading = false, error = null) } },
                onFailure = { throwable ->
                    _uiState.update { it.copy(isLoading = false, error = throwable.asDataError()) }
                },
            )
        }
    }

    private fun reportFailure(throwable: Throwable) {
        _uiState.update { it.copy(error = throwable.asDataError()) }
    }
}
