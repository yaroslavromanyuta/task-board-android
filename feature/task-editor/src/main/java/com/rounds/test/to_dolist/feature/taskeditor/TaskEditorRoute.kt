package com.rounds.test.to_dolist.feature.taskeditor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Stateful half of the screen: the only place that knows a ViewModel exists. Splitting it from
 * [TaskEditorScreen] keeps the form previewable and testable without Hilt.
 *
 * Leaving the screen after a successful save is driven by state rather than by a callback handed to
 * the ViewModel: the save is asynchronous, and this way the navigation happens here, while the
 * composable is still in the composition, instead of from a coroutine that cannot know that.
 */
@Composable
fun TaskEditorRoute(
    onDone: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TaskEditorViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.isSaved) {
        if (state.isSaved) onDone()
    }

    TaskEditorScreen(
        state = state,
        onTitleChange = viewModel::onTitleChange,
        onNotesChange = viewModel::onNotesChange,
        onPriorityChange = viewModel::onPriorityChange,
        onSave = viewModel::onSave,
        onBack = onBack,
        onRetry = viewModel::onRetry,
        onSaveErrorShown = viewModel::onSaveErrorShown,
        modifier = modifier,
    )
}
