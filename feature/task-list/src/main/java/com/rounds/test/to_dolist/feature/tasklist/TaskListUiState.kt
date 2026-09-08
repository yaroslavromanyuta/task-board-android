package com.rounds.test.to_dolist.feature.tasklist

import com.rounds.test.to_dolist.tasks.error.DataError
import com.rounds.test.to_dolist.tasks.model.Task

/**
 * One immutable snapshot of the screen. A single class rather than a sealed hierarchy because a
 * refresh has to be able to spin while already-loaded rows stay on screen.
 *
 * [isEmpty] is derived, not stored, so "empty" can never disagree with the list it describes.
 */
data class TaskListUiState(
    val tasks: List<Task> = emptyList(),
    val isLoading: Boolean = false,
    val error: DataError? = null,
) {
    val isEmpty: Boolean get() = tasks.isEmpty() && !isLoading && error == null
}
