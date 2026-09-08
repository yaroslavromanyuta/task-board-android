package com.rounds.test.to_dolist.tasks.usecase

import com.rounds.test.to_dolist.tasks.repository.TaskRepository
import javax.inject.Inject

/** The only call that hits the mock network on the list screen; drives the loading and error states. */
class RefreshTasksUseCase @Inject constructor(
    private val repository: TaskRepository,
) {
    suspend operator fun invoke(): Result<Unit> = repository.refresh()
}
