package com.rounds.test.to_dolist.tasks.usecase

import com.rounds.test.to_dolist.tasks.error.DataError
import com.rounds.test.to_dolist.tasks.model.Task

/**
 * What a restore actually achieved. A `Result<Task>` could only say "the task is back" or "it is not",
 * and [RestoreTaskUseCase] has a third answer: the task is back but a field of it is not.
 *
 * The partial case is not a failure — the row exists and the user can see it — so it travels as a
 * success the caller has to look inside, rather than as an exception that would contradict the screen.
 */
sealed interface RestoreOutcome {

    val task: Task

    /** Every field the user could see came back. */
    data class Restored(override val task: Task) : RestoreOutcome

    /**
     * The task was re-created but re-applying its completion flag failed, so it is back *unfinished*.
     * [error] is why, so the caller can say which half went wrong instead of reporting a bare failure
     * over a row that visibly arrived.
     */
    data class CompletionLost(override val task: Task, val error: DataError) : RestoreOutcome
}
