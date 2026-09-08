package com.rounds.test.to_dolist.feature.taskeditor.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.rounds.test.to_dolist.feature.taskeditor.TaskEditorRoute
import com.rounds.test.to_dolist.navigation.Route

/**
 * Both entry points — adding a task and opening an existing one — land here.
 * [Route.TaskEditor.taskId] is what tells them apart, and the ViewModel reads it from
 * `SavedStateHandle` rather than taking it as a parameter.
 */
fun NavGraphBuilder.taskEditorSection(
    onDone: () -> Unit,
    onBack: () -> Unit,
) {
    composable<Route.TaskEditor> {
        TaskEditorRoute(
            onDone = onDone,
            onBack = onBack,
        )
    }
}
