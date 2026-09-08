package com.rounds.test.to_dolist.feature.taskeditor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Stateful half of the screen: the only place that knows a ViewModel exists. Splitting it from
 * [TaskEditorScreen] keeps the form previewable and testable without Hilt.
 */
@Composable
fun TaskEditorRoute(
    onDone: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TaskEditorViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    TaskEditorScreen(
        state = state,
        onTitleChange = viewModel::onTitleChange,
        onNotesChange = viewModel::onNotesChange,
        onPriorityChange = viewModel::onPriorityChange,
        onSave = { viewModel.onSave(onSaved = onDone) },
        onBack = onBack,
        onRetry = viewModel::onRetry,
        modifier = modifier,
    )
}
