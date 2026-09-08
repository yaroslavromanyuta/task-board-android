package com.rounds.test.to_dolist.feature.taskeditor

import com.rounds.test.to_dolist.tasks.error.DataError
import com.rounds.test.to_dolist.tasks.model.TaskPriority

/**
 * Create and edit share this state because they share the form. [isEditing] is derived from whether
 * navigation handed us an id, so the two modes cannot drift apart.
 *
 * Three kinds of thing can go wrong here and they are deliberately three fields, because the screen
 * has to do something different with each:
 *  - [titleError] is a form problem the user fixes in place;
 *  - [error] means the edit-mode load failed and there is nothing to edit — the form is replaced by a
 *    message and a Retry;
 *  - [saveError] means the save failed while the form is perfectly good. Blanking it would throw away
 *    what the user typed, so it is surfaced transiently and the form stays exactly as it was (FR-02).
 */
data class TaskEditorUiState(
    val taskId: String? = null,
    val title: String = "",
    val notes: String = "",
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val titleError: Boolean = false,
    val error: DataError? = null,
    val saveError: DataError? = null,
) {
    val isEditing: Boolean get() = taskId != null
    val canSave: Boolean get() = title.isNotBlank() && !isSaving && !isLoading
}
