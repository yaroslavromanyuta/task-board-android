package com.rounds.test.to_dolist.feature.taskeditor

import com.rounds.test.to_dolist.tasks.error.DataError
import com.rounds.test.to_dolist.tasks.model.TaskPriority

/**
 * Create and edit share this state because they share the form. [isEditing] is derived from whether
 * navigation handed us an id, so the two modes cannot drift apart.
 *
 * [titleError] is separate from [error]: a blank title is a form problem the user fixes in place,
 * while [error] is a data-layer failure that needs a retry.
 */
data class TaskEditorUiState(
    val taskId: String? = null,
    val title: String = "",
    val notes: String = "",
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val titleError: Boolean = false,
    val error: DataError? = null,
) {
    val isEditing: Boolean get() = taskId != null
    val canSave: Boolean get() = title.isNotBlank() && !isSaving && !isLoading
}
