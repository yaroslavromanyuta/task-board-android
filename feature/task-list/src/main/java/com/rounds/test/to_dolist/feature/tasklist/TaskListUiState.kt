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
 * [error] blocks the screen and [messages] pass over it: a failure that arrives with rows already
 * showing must not cost the user their context (FR-09 boundary), so the ViewModel chooses between
 * them and the screen does not have to.
 *
 * [messages] is a queue and not a slot. One slot meant the last writer won, and the writer that lost
 * could be a `TaskDeleted` holding the only copy of a deleted task — so an unrelated failure landing
 * during the few seconds an undo offer is up took the task with it.
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
    val messages: List<TaskListMessage> = emptyList(),
    /** Ids whose completion write is in flight. Their checkbox is inert until it returns (FR-03). */
    val pendingToggles: Set<String> = emptySet(),
) {
    val visibleTasks: List<Task> = tasks
        .filter { it.title.contains(query, ignoreCase = true) }
        .sortedBy(sort)

    /** The one the snackbar is showing. The rest wait their turn rather than being overwritten. */
    val message: TaskListMessage? = messages.firstOrNull()

    /** No tasks at all — as opposed to [hasNoMatches], which is a query the user can clear. */
    val isEmpty: Boolean = tasks.isEmpty() && !isLoading && error == null

    val hasNoMatches: Boolean = tasks.isNotEmpty() && visibleTasks.isEmpty()
}
