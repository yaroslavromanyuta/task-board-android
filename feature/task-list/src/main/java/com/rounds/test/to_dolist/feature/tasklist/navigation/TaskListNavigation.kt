package com.rounds.test.to_dolist.feature.tasklist.navigation

import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.rounds.test.to_dolist.feature.tasklist.TaskListRoute
import com.rounds.test.to_dolist.navigation.Route

/**
 * The module's own slice of the graph. `:app` composes these sections instead of importing screens,
 * so a feature can be added or removed without editing a shared NavHost body.
 *
 * The "a save landed" result is read here, from the back stack entry, and not from a `SavedStateHandle`
 * injected into the ViewModel. Those are two different handles: `NavBackStackEntry.savedStateHandle` is
 * keyed to the entry's own internal holder, while Hilt builds the ViewModel's from the same registry
 * under the ViewModel's key, so a value written into one is invisible to the other. On device that
 * showed up as the query surviving a save — the defect the result exists to close (issue #16).
 */
fun NavGraphBuilder.taskListSection(
    onTaskClick: (String) -> Unit,
    onAddTask: () -> Unit,
) {
    composable<Route.TaskList> { entry ->
        val savedStateHandle = entry.savedStateHandle
        val taskSaved by savedStateHandle
            .getStateFlow(Route.TaskList.TASK_SAVED_RESULT, false)
            .collectAsStateWithLifecycle()

        TaskListRoute(
            onTaskClick = onTaskClick,
            onAddTask = onAddTask,
            taskSaved = taskSaved,
            // Consumed here, so returning to this destination a second time does not clear a query
            // the user typed after the save.
            onTaskSavedHandled = { savedStateHandle[Route.TaskList.TASK_SAVED_RESULT] = false },
        )
    }
}
