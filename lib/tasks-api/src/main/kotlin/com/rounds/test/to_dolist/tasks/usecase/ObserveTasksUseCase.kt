package com.rounds.test.to_dolist.tasks.usecase

import com.rounds.test.to_dolist.tasks.model.Task
import com.rounds.test.to_dolist.tasks.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Streams the cached list. Sorting/filtering policy belongs here rather than in the ViewModel, so
 * both the list screen and any future widget agree on the order.
 */
class ObserveTasksUseCase @Inject constructor(
    private val repository: TaskRepository,
) {
    operator fun invoke(): Flow<List<Task>> = TODO("Skeleton: implemented with the data layer")
}
