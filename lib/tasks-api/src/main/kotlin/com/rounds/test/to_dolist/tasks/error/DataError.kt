package com.rounds.test.to_dolist.tasks.error

/**
 * Typed failure categories. No strings live here: the UI decides the wording, so the domain stays
 * free of resources and locale.
 *
 * The set is deliberately small and matches what the mock network layer can actually produce — a
 * category nothing can emit is a category nobody tests.
 */
sealed interface DataError {
    /** The request never reached the source. Retrying immediately is reasonable. */
    data object Network : DataError

    /** The source answered too slowly. Same remedy as [Network], different wording. */
    data object Timeout : DataError

    /** The task is gone — usually deleted from another screen. Retrying will not help. */
    data object NotFound : DataError

    /** A write raced with another write. The caller should refresh before retrying. */
    data object Conflict : DataError

    data object Unknown : DataError
}

/** Carrier so repositories can keep returning [Result] while still exposing a typed [error]. */
class DataException(
    val error: DataError,
    cause: Throwable? = null,
) : Exception(cause?.message ?: error.toString(), cause)

/** Failures produced by the data layer are always [DataException]; anything else is a bug, not a category. */
fun Throwable.asDataError(): DataError = (this as? DataException)?.error ?: DataError.Unknown
