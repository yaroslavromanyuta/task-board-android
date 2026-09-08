package com.rounds.test.to_dolist.tasks.usecase

import com.rounds.test.to_dolist.tasks.model.Task
import com.rounds.test.to_dolist.tasks.model.toDraft
import com.rounds.test.to_dolist.tasks.repository.TaskRepository
import javax.inject.Inject

/**
 * Puts a deleted task back. The source has no "restore" operation — it has the five REST-shaped calls
 * a backend would offer — so this re-creates the task, which means the id is new and the row lands
 * where a newly created one would.
 *
 * The two-step is why this is a use case and not a line in a ViewModel: [com.rounds.test.to_dolist.tasks.model.TaskDraft]
 * is deliberately the *editable* half of a task, so creating from one cannot carry `isCompleted` and
 * the flag has to be re-applied afterwards. "Restore" meaning "every field the user could see comes
 * back" is a domain rule, and it belongs where the next caller will find it.
 */
class RestoreTaskUseCase @Inject constructor(
    private val repository: TaskRepository,
) {
    suspend operator fun invoke(task: Task): Result<Task> {
        val created = repository.createTask(task.toDraft()).getOrElse { return Result.failure(it) }
        if (!task.isCompleted) return Result.success(created)

        return repository.setCompleted(created.id, completed = true)
            .map { created.copy(isCompleted = true) }
    }
}
