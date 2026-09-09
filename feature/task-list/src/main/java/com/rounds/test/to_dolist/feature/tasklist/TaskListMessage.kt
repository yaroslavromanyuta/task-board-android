package com.rounds.test.to_dolist.feature.tasklist

import com.rounds.test.to_dolist.tasks.error.DataError
import com.rounds.test.to_dolist.tasks.model.Task

/**
 * Something the list has to say once, over the top of whatever is on screen, rather than instead of
 * it. Three cases, because they are acted on differently: two are read and dismissed, and one offers
 * a way back.
 *
 * These queue rather than overwrite ([TaskListUiState.messages]). A [TaskDeleted] holds the only copy
 * of a deleted task, so a message that displaced it would take the task with it.
 */
sealed interface TaskListMessage {

    /** A refresh or a write failed while there were already rows on screen (REQUIREMENTS.md §9). */
    data class Failure(val error: DataError) : TaskListMessage

    /** A delete succeeded. Carries the whole task, because that is what Undo needs to put it back. */
    data class TaskDeleted(val task: Task) : TaskListMessage

    /**
     * An undo re-created the task but could not re-apply its completion flag, so the row is back and
     * unfinished. Distinct from [Failure] because "it failed" over a row that visibly arrived reads as
     * a lie — the user has to be told which half was lost.
     */
    data class CompletionNotRestored(val error: DataError) : TaskListMessage
}
