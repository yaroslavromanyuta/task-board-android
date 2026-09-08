package com.rounds.test.to_dolist.feature.tasklist

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.rounds.test.to_dolist.core.ui.component.EmptyMessage
import com.rounds.test.to_dolist.core.ui.component.ErrorMessage
import com.rounds.test.to_dolist.core.ui.component.Loading
import com.rounds.test.to_dolist.core.ui.error.asMessage
import com.rounds.test.to_dolist.core.ui.theme.TodoListTheme
import com.rounds.test.to_dolist.feature.tasklist.component.TaskRow
import com.rounds.test.to_dolist.tasks.error.DataError
import com.rounds.test.to_dolist.tasks.model.Task
import com.rounds.test.to_dolist.tasks.model.TaskPriority
import com.rounds.test.to_dolist.tasks.model.TaskSort
import java.time.Instant

/**
 * Stateless screen: everything it shows arrives in [state] and everything it does leaves through a
 * callback. That is what makes every required state previewable and testable without Hilt.
 *
 * The branch order matters. A cached list keeps rendering while a refresh spins, so `isLoading` only
 * takes over the whole screen when there is nothing to show yet, and "no tasks at all" is a different
 * sentence from "nothing matches what you typed" because only one of them is fixed by clearing a
 * field.
 *
 * Failures that arrive with rows on screen come through [TaskListUiState.message] and pass over the
 * list in the snackbar instead of replacing it.
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
    onQueryChange: (String) -> Unit,
    onSortChange: (TaskSort) -> Unit,
    onUndoDelete: (TaskListMessage.TaskDeleted) -> Unit,
    onMessageShown: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    SnackbarEffect(
        message = state.message,
        hostState = snackbarHostState,
        onUndoDelete = onUndoDelete,
        onMessageShown = onMessageShown,
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.task_list_title)) },
                actions = {
                    if (state.tasks.isNotEmpty()) {
                        SortMenu(selected = state.sort, onSortChange = onSortChange)
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddTask) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.task_list_action_add),
                )
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
        ) {
            // Only offered once there is something to search: a filter over an empty screen is noise.
            if (state.tasks.isNotEmpty()) {
                SearchField(query = state.query, onQueryChange = onQueryChange)
            }

            Box(modifier = Modifier.weight(1f)) {
                when {
                    state.tasks.isEmpty() && state.isLoading -> Loading()

                    state.tasks.isEmpty() && state.error != null ->
                        ErrorMessage(message = state.error.asMessage(), onRetry = onRetry)

                    state.isEmpty -> EmptyMessage(message = stringResource(R.string.task_list_empty))

                    state.hasNoMatches -> EmptyMessage(
                        message = stringResource(R.string.task_list_no_matches, state.query),
                    )

                    else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(items = state.visibleTasks, key = { it.id }) { task ->
                            TaskRow(
                                task = task,
                                onClick = { onTaskClick(task.id) },
                                onToggleCompleted = { done -> onToggleCompleted(task.id, done) },
                                onDelete = { onDelete(task.id) },
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * A delete offers a way back; a failure is only read. Both are cleared through [onMessageShown] so
 * that a configuration change re-collecting the state does not show the same thing twice.
 */
@Composable
private fun SnackbarEffect(
    message: TaskListMessage?,
    hostState: SnackbarHostState,
    onUndoDelete: (TaskListMessage.TaskDeleted) -> Unit,
    onMessageShown: () -> Unit,
) {
    val text = when (message) {
        is TaskListMessage.Failure -> message.error.asMessage()
        is TaskListMessage.TaskDeleted -> stringResource(R.string.task_list_deleted)
        null -> null
    }
    val undoLabel = stringResource(R.string.task_list_action_undo)

    LaunchedEffect(message) {
        if (message == null || text == null) return@LaunchedEffect

        val result = hostState.showSnackbar(
            message = text,
            actionLabel = if (message is TaskListMessage.TaskDeleted) undoLabel else null,
        )

        if (result == SnackbarResult.ActionPerformed && message is TaskListMessage.TaskDeleted) {
            onUndoDelete(message)
        } else {
            onMessageShown()
        }
    }
}

@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        singleLine = true,
        label = { Text(stringResource(R.string.task_list_search_hint)) },
        leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = stringResource(R.string.task_list_action_clear_search),
                    )
                }
            }
        },
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

@Composable
private fun SortMenu(
    selected: TaskSort,
    onSortChange: (TaskSort) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    IconButton(onClick = { expanded = true }) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.Sort,
            contentDescription = stringResource(R.string.task_list_action_sort),
        )
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        TaskSort.entries.forEach { sort ->
            DropdownMenuItem(
                text = { Text(sort.label()) },
                leadingIcon = { RadioButton(selected = sort == selected, onClick = null) },
                onClick = {
                    expanded = false
                    onSortChange(sort)
                },
            )
        }
    }
}

@Composable
private fun TaskSort.label(): String = stringResource(
    when (this) {
        TaskSort.DEFAULT -> R.string.task_list_sort_default
        TaskSort.PRIORITY -> R.string.task_list_sort_priority
        TaskSort.COMPLETION -> R.string.task_list_sort_completion
    },
)

@PreviewLightDark
@Composable
private fun TaskListEmptyPreview() = PreviewScreen(TaskListUiState())

@PreviewLightDark
@Composable
private fun TaskListErrorPreview() = PreviewScreen(TaskListUiState(error = DataError.Network))

@PreviewLightDark
@Composable
private fun TaskListContentPreview() = PreviewScreen(TaskListUiState(tasks = PreviewTasks))

/** A query that matches nothing: a state the list could not previously express. */
@PreviewLightDark
@Composable
private fun TaskListNoMatchesPreview() =
    PreviewScreen(TaskListUiState(tasks = PreviewTasks, query = "invoice"))

@Composable
private fun PreviewScreen(state: TaskListUiState) {
    TodoListTheme(dynamicColor = false) {
        TaskListScreen(
            state = state,
            onTaskClick = {},
            onAddTask = {},
            onToggleCompleted = { _, _ -> },
            onDelete = {},
            onRetry = {},
            onQueryChange = {},
            onSortChange = {},
            onUndoDelete = {},
            onMessageShown = {},
        )
    }
}

/** The seed rows, so the previews show the layout the app actually renders on launch. */
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
