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
            // A save is reported back to whatever opened the editor before the pop, because the list
            // has no other way to know one happened: closing the editor is the app's only "saved"
            // signal, and a new task hidden by an active search would otherwise land unseen.
            onDone = {
                navController.previousBackStackEntry
                    ?.savedStateHandle
                    ?.set(Route.TaskList.TASK_SAVED_RESULT, true)
                navController.navigateUp()
            },
            onBack = navController::navigateUp,
        )
    }
}
