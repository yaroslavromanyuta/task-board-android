package com.rounds.test.to_dolist.feature.tasklist

import com.rounds.test.to_dolist.tasks.error.DataError
import com.rounds.test.to_dolist.tasks.model.Task

/**
 * Something the list has to say once, over the top of whatever is on screen, rather than instead of
 * it. Two cases because only one of the two is a failure, and they are acted on differently: one is
 * read and dismissed, the other offers a way back.
 */
sealed interface TaskListMessage {

    /** A refresh or a write failed while there were already rows on screen (REQUIREMENTS.md §9). */
    data class Failure(val error: DataError) : TaskListMessage

    /** A delete succeeded. Carries the whole task, because that is what Undo needs to put it back. */
    data class TaskDeleted(val task: Task) : TaskListMessage
}
