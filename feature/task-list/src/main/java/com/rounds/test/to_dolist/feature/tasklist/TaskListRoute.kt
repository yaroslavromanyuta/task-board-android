package com.rounds.test.to_dolist.feature.tasklist

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Stateful half of the screen: the only place that knows a ViewModel exists. Splitting it from
 * [TaskListScreen] keeps the screen previewable and lets `:app` wire navigation without touching UI.
 *
 * [taskSaved] arrives from the navigation section, which reads it off the back stack entry. The
 * ViewModel is told rather than asked: it has no way to see a result that lives on a handle Hilt did
 * not give it, and this keeps the nav plumbing in the one file that already knows about navigation.
 */
@Composable
fun TaskListRoute(
    onTaskClick: (String) -> Unit,
    onAddTask: () -> Unit,
    modifier: Modifier = Modifier,
    taskSaved: Boolean = false,
    onTaskSavedHandled: () -> Unit = {},
    viewModel: TaskListViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(taskSaved) {
        if (!taskSaved) return@LaunchedEffect
        viewModel.onTaskSaved()
        onTaskSavedHandled()
    }

    TaskListScreen(
        state = state,
        onTaskClick = onTaskClick,
        onAddTask = onAddTask,
        onToggleCompleted = viewModel::onToggleCompleted,
        onDelete = viewModel::onDelete,
        onRetry = viewModel::onRetry,
        onQueryChange = viewModel::onQueryChange,
        onSortChange = viewModel::onSortChange,
        onUndoDelete = viewModel::onUndoDelete,
        onMessageShown = viewModel::onMessageShown,
        modifier = modifier,
    )
}
