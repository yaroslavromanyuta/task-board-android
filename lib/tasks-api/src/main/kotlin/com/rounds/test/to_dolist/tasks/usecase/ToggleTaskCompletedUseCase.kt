package com.rounds.test.to_dolist.tasks.usecase

import com.rounds.test.to_dolist.tasks.repository.TaskRepository
import javax.inject.Inject

/**
 * Flips the completed flag. Takes the desired value rather than toggling blind, so a stale row tapped
 * twice cannot end up disagreeing with the cache.
 */
class ToggleTaskCompletedUseCase @Inject constructor(
    private val repository: TaskRepository,
) {
    suspend operator fun invoke(id: String, completed: Boolean): Result<Unit> =
        repository.setCompleted(id, completed)
}
