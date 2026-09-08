package com.rounds.test.to_dolist.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.rounds.test.to_dolist.feature.taskeditor.navigation.taskEditorSection
import com.rounds.test.to_dolist.feature.tasklist.navigation.taskListSection

/**
 * The only place the two features meet. Each contributes its own section of the graph, so `:app`
 * wires destinations together without importing a single screen composable — and neither feature
 * module needs to know the other exists.
 */
@Composable
fun TodoNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    NavHost(
        navController = navController,
        startDestination = Route.TaskList,
        modifier = modifier,
    ) {
        taskListSection(
            onTaskClick = { taskId -> navController.navigate(Route.TaskEditor(taskId = taskId)) },
            onAddTask = { navController.navigate(Route.TaskEditor()) },
        )
        taskEditorSection(
            onDone = navController::navigateUp,
            onBack = navController::navigateUp,
        )
    }
}
