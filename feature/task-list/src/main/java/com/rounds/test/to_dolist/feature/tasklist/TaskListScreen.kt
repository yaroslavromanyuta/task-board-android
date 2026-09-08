package com.rounds.test.to_dolist.feature.tasklist

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.rounds.test.to_dolist.core.ui.component.EmptyMessage
import com.rounds.test.to_dolist.core.ui.component.ErrorMessage
import com.rounds.test.to_dolist.core.ui.component.Loading
import com.rounds.test.to_dolist.core.ui.error.asMessage
import com.rounds.test.to_dolist.core.ui.theme.TodoListTheme
import com.rounds.test.to_dolist.feature.tasklist.component.TaskRow
import com.rounds.test.to_dolist.tasks.error.DataError
import com.rounds.test.to_dolist.tasks.model.Task
import com.rounds.test.to_dolist.tasks.model.TaskPriority
import java.time.Instant

/**
 * Stateless screen: everything it shows arrives in [state] and everything it does leaves through a
 * callback. That is what makes the three required states previewable and testable without Hilt.
 *
 * The state order matters — a cached list keeps rendering while a refresh spins, so `isLoading` only
 * takes over the whole screen when there is nothing to show yet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListScreen(
    state: TaskListUiState,
    onTaskClick: (String) -> Unit,
    onAddTask: () -> Unit,
    onToggleCompleted: (String, Boolean) -> Unit,
    onDelete: (String) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = { TopAppBar(title = { Text(stringResource(R.string.task_list_title)) }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddTask) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.task_list_action_add),
                )
            }
        },
    ) { innerPadding ->
        val contentModifier = Modifier.padding(innerPadding)
        when {
            state.tasks.isEmpty() && state.isLoading -> Loading(contentModifier)

            state.tasks.isEmpty() && state.error != null ->
                ErrorMessage(
                    message = state.error.asMessage(),
                    onRetry = onRetry,
                    modifier = contentModifier,
                )

            state.isEmpty -> EmptyMessage(
                message = stringResource(R.string.task_list_empty),
                modifier = contentModifier,
            )

            else -> LazyColumn(modifier = contentModifier.fillMaxSize()) {
                items(items = state.tasks, key = { it.id }) { task ->
                    TaskRow(
                        task = task,
                        onClick = { onTaskClick(task.id) },
                        onToggleCompleted = { completed -> onToggleCompleted(task.id, completed) },
                        onDelete = { onDelete(task.id) },
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TaskListEmptyPreview() {
    TodoListTheme(dynamicColor = false) {
        TaskListScreen(
            state = TaskListUiState(),
            onTaskClick = {},
            onAddTask = {},
            onToggleCompleted = { _, _ -> },
            onDelete = {},
            onRetry = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TaskListErrorPreview() {
    TodoListTheme(dynamicColor = false) {
        TaskListScreen(
            state = TaskListUiState(error = DataError.Network),
            onTaskClick = {},
            onAddTask = {},
            onToggleCompleted = { _, _ -> },
            onDelete = {},
            onRetry = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TaskListContentPreview() {
    TodoListTheme(dynamicColor = false) {
        TaskListScreen(
            state = TaskListUiState(tasks = PreviewTasks),
            onTaskClick = {},
            onAddTask = {},
            onToggleCompleted = { _, _ -> },
            onDelete = {},
            onRetry = {},
        )
    }
}

/** The seed rows, so the preview shows the layout the app actually renders on launch. */
private val PreviewTasks = listOf(
    Task("task-1", "Renew domain registration", "Expires end of month", TaskPriority.HIGH, false, Instant.EPOCH),
    Task("task-2", "Reply to design feedback", null, TaskPriority.MEDIUM, false, Instant.EPOCH),
    Task("task-3", "Book dentist", null, TaskPriority.LOW, true, Instant.EPOCH),
    Task(
        id = "task-4",
        title = "Migrate the analytics pipeline to the new warehouse and validate dashboards",
        notes = "Long one — check layout",
        priority = TaskPriority.MEDIUM,
        isCompleted = false,
        createdAt = Instant.EPOCH,
    ),
)
