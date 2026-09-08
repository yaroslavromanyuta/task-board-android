package com.rounds.test.to_dolist.feature.tasklist

import com.rounds.test.to_dolist.tasks.error.DataError
import com.rounds.test.to_dolist.tasks.model.Task
import com.rounds.test.to_dolist.tasks.model.TaskSort
import com.rounds.test.to_dolist.tasks.model.sortedBy

/**
 * One immutable snapshot of the screen. A single class rather than a sealed hierarchy because a
 * refresh has to be able to spin while already-loaded rows stay on screen.
 *
 * [tasks] is everything the cache holds; [visibleTasks] is what the user asked to see. Keeping both
 * is what lets "you have no tasks" and "nothing matches what you typed" be different sentences.
 *
 * [error] blocks the screen and [message] passes over it: a failure that arrives with rows already
 * showing must not cost the user their context (FR-09 boundary), so the ViewModel chooses between
 * them and the screen does not have to.
 *
 * The derived values are computed once per instance rather than in a getter, so recomposition reads
 * them instead of re-running the filter.
 */
data class TaskListUiState(
    val tasks: List<Task> = emptyList(),
    val query: String = "",
    val sort: TaskSort = TaskSort.DEFAULT,
    val isLoading: Boolean = false,
    val error: DataError? = null,
    val message: TaskListMessage? = null,
) {
    val visibleTasks: List<Task> = tasks
        .filter { it.title.contains(query, ignoreCase = true) }
        .sortedBy(sort)

    /** No tasks at all — as opposed to [hasNoMatches], which is a query the user can clear. */
    val isEmpty: Boolean = tasks.isEmpty() && !isLoading && error == null

    val hasNoMatches: Boolean = tasks.isNotEmpty() && visibleTasks.isEmpty()
}
