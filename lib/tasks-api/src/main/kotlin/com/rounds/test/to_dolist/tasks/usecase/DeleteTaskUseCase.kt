package com.rounds.test.to_dolist.tasks.usecase

import com.rounds.test.to_dolist.tasks.repository.TaskRepository
import javax.inject.Inject

class DeleteTaskUseCase @Inject constructor(
    private val repository: TaskRepository,
) {
    suspend operator fun invoke(id: String): Result<Unit> = repository.deleteTask(id)
}
