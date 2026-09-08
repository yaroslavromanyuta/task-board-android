package com.rounds.test.to_dolist.tasks.usecase

import com.rounds.test.to_dolist.tasks.model.Task
import com.rounds.test.to_dolist.tasks.repository.TaskRepository
import javax.inject.Inject

/**
 * Loads one task for the editor. The editor receives only an id through navigation and refetches, so
 * the nav payload stays small and survives process death.
 */
class GetTaskUseCase @Inject constructor(
    private val repository: TaskRepository,
) {
    suspend operator fun invoke(id: String): Result<Task> = repository.getTask(id)
}
