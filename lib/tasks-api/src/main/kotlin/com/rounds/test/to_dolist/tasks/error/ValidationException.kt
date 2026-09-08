package com.rounds.test.to_dolist.tasks.error

/**
 * A rule the user can fix in the form, as opposed to [DataError], which is a failure of the source.
 * They are separate types because the UI treats them differently: one highlights a field, the other
 * offers a retry.
 */
sealed class ValidationException(message: String) : Exception(message) {

    /** A task without a title is not a task. Enforced in `SaveTaskUseCase`, never at a call site. */
    class BlankTitle : ValidationException("A task title must not be blank")
}
