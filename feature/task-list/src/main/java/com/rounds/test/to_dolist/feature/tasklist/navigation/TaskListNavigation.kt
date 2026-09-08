package com.rounds.test.to_dolist.feature.tasklist.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.rounds.test.to_dolist.feature.tasklist.TaskListRoute
import com.rounds.test.to_dolist.navigation.Route

/**
 * The module's own slice of the graph. `:app` composes these sections instead of importing screens,
 * so a feature can be added or removed without editing a shared NavHost body.
 */
fun NavGraphBuilder.taskListSection(
    onTaskClick: (String) -> Unit,
    onAddTask: () -> Unit,
) {
    composable<Route.TaskList> {
        TaskListRoute(
            onTaskClick = onTaskClick,
            onAddTask = onAddTask,
        )
    }
}
