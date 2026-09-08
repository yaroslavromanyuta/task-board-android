package com.rounds.test.to_dolist.tasks.usecase

import com.rounds.test.to_dolist.tasks.error.ValidationException
import com.rounds.test.to_dolist.tasks.model.Task
import com.rounds.test.to_dolist.tasks.model.TaskDraft
import com.rounds.test.to_dolist.tasks.repository.TaskRepository
import javax.inject.Inject

/**
 * Create-or-update behind one call: a null [id] means the draft is new. This is what lets a single
 * editor screen (and a single feature module) serve both "add" and "edit".
 *
 * Title validation lives here, not in the ViewModel, so the rule cannot drift between call sites. The
 * title is trimmed on the way through: "  " is blank, and " Buy milk " and "Buy milk" are one task.
 */
class SaveTaskUseCase @Inject constructor(
    private val repository: TaskRepository,
) {
    suspend operator fun invoke(id: String?, draft: TaskDraft): Result<Task> {
        val title = draft.title.trim()
        if (title.isEmpty()) return Result.failure(ValidationException.BlankTitle())

        val normalised = draft.copy(title = title, notes = draft.notes?.trim()?.ifEmpty { null })
        return if (id == null) repository.createTask(normalised)
        else repository.updateTask(id, normalised)
    }
}
