package com.rounds.test.to_dolist.feature.tasklist

import androidx.lifecycle.ViewModel
import com.rounds.test.to_dolist.tasks.usecase.DeleteTaskUseCase
import com.rounds.test.to_dolist.tasks.usecase.ObserveTasksUseCase
import com.rounds.test.to_dolist.tasks.usecase.RefreshTasksUseCase
import com.rounds.test.to_dolist.tasks.usecase.ToggleTaskCompletedUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * Owns the list state. The use cases are already wired through Hilt so that filling them in is the
 * only remaining step — the graph, the scopes and the state shape do not change.
 *
 * The handlers are intentionally inert while the data layer is a skeleton: the screen has to stay
 * runnable and navigable, and a `TODO()` here would crash the app on first tap.
 */
@HiltViewModel
class TaskListViewModel @Inject constructor(
    private val observeTasks: ObserveTasksUseCase,
    private val refreshTasks: RefreshTasksUseCase,
    private val toggleCompleted: ToggleTaskCompletedUseCase,
    private val deleteTask: DeleteTaskUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TaskListUiState())
    val uiState: StateFlow<TaskListUiState> = _uiState.asStateFlow()

    init {
        // TODO(data layer): collect observeTasks() into _uiState and kick off the first refresh().
    }

    fun onRetry() {
        // TODO(data layer): set isLoading, call refreshTasks(), map failure to state.error.
    }

    fun onToggleCompleted(id: String, completed: Boolean) {
        // TODO(data layer): call toggleCompleted(id, completed).
    }

    fun onDelete(id: String) {
        // TODO(data layer): call deleteTask(id).
    }
}
