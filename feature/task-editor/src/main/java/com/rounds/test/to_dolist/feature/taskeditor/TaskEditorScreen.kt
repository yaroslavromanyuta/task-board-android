package com.rounds.test.to_dolist.feature.taskeditor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.rounds.test.to_dolist.core.ui.component.ErrorMessage
import com.rounds.test.to_dolist.core.ui.component.Loading
import com.rounds.test.to_dolist.core.ui.error.asMessage
import com.rounds.test.to_dolist.core.ui.theme.TodoListTheme
import com.rounds.test.to_dolist.tasks.error.DataError
import com.rounds.test.to_dolist.tasks.model.TaskPriority
import com.rounds.test.to_dolist.core.ui.R as CoreUiR

/**
 * One form for both modes. The only visible difference is the app bar title, which is why creating a
 * task and viewing/editing one are a single feature module rather than two near-identical ones.
 *
 * The two failure kinds are rendered differently on purpose. A failed *load* means there is nothing
 * to edit, so the form gives way to a message and a Retry. A failed *save* means the form is still
 * good and still full of the user's work, so it is reported over the top and nothing is lost (FR-02).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskEditorScreen(
    state: TaskEditorUiState,
    onTitleChange: (String) -> Unit,
    onNotesChange: (String) -> Unit,
    onPriorityChange: (TaskPriority) -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onSaveErrorShown: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val saveErrorMessage = state.saveError?.asMessage()

    LaunchedEffect(saveErrorMessage) {
        if (saveErrorMessage != null) {
            snackbarHostState.showSnackbar(saveErrorMessage)
            onSaveErrorShown()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            if (state.isEditing) R.string.task_editor_title_edit
                            else R.string.task_editor_title_create,
                        ),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(CoreUiR.string.core_ui_action_back),
                        )
                    }
                },
                actions = {
                    TextButton(onClick = onSave, enabled = state.canSave) {
                        Text(stringResource(R.string.task_editor_action_save))
                    }
                },
            )
        },
    ) { innerPadding ->
        val contentModifier = Modifier.padding(innerPadding)
        when {
            state.isLoading -> Loading(contentModifier)

            state.error != null -> ErrorMessage(
                message = state.error.asMessage(),
                onRetry = onRetry,
                modifier = contentModifier,
            )

            else -> TaskEditorForm(
                state = state,
                onTitleChange = onTitleChange,
                onNotesChange = onNotesChange,
                onPriorityChange = onPriorityChange,
                modifier = contentModifier,
            )
        }
    }
}

@Composable
private fun TaskEditorForm(
    state: TaskEditorUiState,
    onTitleChange: (String) -> Unit,
    onNotesChange: (String) -> Unit,
    onPriorityChange: (TaskPriority) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        OutlinedTextField(
            value = state.title,
            onValueChange = onTitleChange,
            label = { Text(stringResource(R.string.task_editor_field_title)) },
            isError = state.titleError,
            supportingText = if (state.titleError) {
                { Text(stringResource(R.string.task_editor_error_title_blank)) }
            } else {
                null
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
            value = state.notes,
            onValueChange = onNotesChange,
            label = { Text(stringResource(R.string.task_editor_field_notes)) },
            minLines = 3,
            modifier = Modifier.fillMaxWidth(),
        )

        Text(
            text = stringResource(R.string.task_editor_field_priority),
            style = MaterialTheme.typography.labelLarge,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TaskPriority.entries.forEach { priority ->
                FilterChip(
                    selected = state.priority == priority,
                    onClick = { onPriorityChange(priority) },
                    label = { Text(priority.label()) },
                )
            }
        }
    }
}

@Composable
private fun TaskPriority.label(): String = stringResource(
    when (this) {
        TaskPriority.LOW -> CoreUiR.string.core_ui_priority_low
        TaskPriority.MEDIUM -> CoreUiR.string.core_ui_priority_medium
        TaskPriority.HIGH -> CoreUiR.string.core_ui_priority_high
    },
)

@Preview(showBackground = true)
@Composable
private fun TaskEditorCreatePreview() {
    TodoListTheme(dynamicColor = false) {
        TaskEditorScreen(
            state = TaskEditorUiState(),
            onTitleChange = {},
            onNotesChange = {},
            onPriorityChange = {},
            onSave = {},
            onBack = {},
            onRetry = {},
            onSaveErrorShown = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TaskEditorEditPreview() {
    TodoListTheme(dynamicColor = false) {
        TaskEditorScreen(
            state = TaskEditorUiState(
                taskId = "task-1",
                title = "Renew domain registration",
                notes = "Expires end of month",
                priority = TaskPriority.HIGH,
            ),
            onTitleChange = {},
            onNotesChange = {},
            onPriorityChange = {},
            onSave = {},
            onBack = {},
            onRetry = {},
            onSaveErrorShown = {},
        )
    }
}

/** The edit-mode load failed: there is nothing to edit, so the form gives way entirely. */
@Preview(showBackground = true)
@Composable
private fun TaskEditorLoadErrorPreview() {
    TodoListTheme(dynamicColor = false) {
        TaskEditorScreen(
            state = TaskEditorUiState(taskId = "task-1", error = DataError.NotFound),
            onTitleChange = {},
            onNotesChange = {},
            onPriorityChange = {},
            onSave = {},
            onBack = {},
            onRetry = {},
            onSaveErrorShown = {},
        )
    }
}
