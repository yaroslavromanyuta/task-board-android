package com.rounds.test.to_dolist.tasks.usecase

import com.rounds.test.to_dolist.tasks.model.Task
import com.rounds.test.to_dolist.tasks.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Streams the cached list. Sorting/filtering policy belongs here rather than in the ViewModel, so
 * both the list screen and any future widget agree on the order.
 *
 * The default order is the order the source returned (REQUIREMENTS.md §5): a task does not move
 * because it was completed. Explicit sorting is FR-11 and lands here when it does.
 */
class ObserveTasksUseCase @Inject constructor(
    private val repository: TaskRepository,
) {
    operator fun invoke(): Flow<List<Task>> = repository.observeTasks()
}
