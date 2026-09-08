package com.rounds.test.to_dolist.feature.tasklist

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Stateful half of the screen: the only place that knows a ViewModel exists. Splitting it from
 * [TaskListScreen] keeps the screen previewable and lets `:app` wire navigation without touching UI.
 */
@Composable
fun TaskListRoute(
    onTaskClick: (String) -> Unit,
    onAddTask: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TaskListViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    TaskListScreen(
        state = state,
        onTaskClick = onTaskClick,
        onAddTask = onAddTask,
        onToggleCompleted = viewModel::onToggleCompleted,
        onDelete = viewModel::onDelete,
        onRetry = viewModel::onRetry,
        modifier = modifier,
    )
}
