package com.rounds.test.to_dolist.data.tasks.repository

import com.rounds.test.to_dolist.core.common.di.IoDispatcher
import com.rounds.test.to_dolist.data.tasks.api.TaskApi
import com.rounds.test.to_dolist.data.tasks.cache.InMemoryTaskCache
import com.rounds.test.to_dolist.tasks.model.Task
import com.rounds.test.to_dolist.tasks.model.TaskDraft
import com.rounds.test.to_dolist.tasks.repository.TaskRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Cache is the single source of truth for reads; the API is the source of truth for writes. Each write
 * goes to the API first and only updates the cache once the call succeeds, so a failed request never
 * leaves the list showing something the "server" does not have.
 *
 * Every call is wrapped so the caller sees `Result` whose failure is always a
 * [com.rounds.test.to_dolist.tasks.error.DataException] — transport types stop here.
 */
@Singleton
class DefaultTaskRepository @Inject constructor(
    private val api: TaskApi,
    private val cache: InMemoryTaskCache,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : TaskRepository {

    override fun observeTasks(): Flow<List<Task>> = cache.observe()

    override suspend fun refresh(): Result<Unit> = TODO("Skeleton: implemented with the mock network layer")

    override suspend fun getTask(id: String): Result<Task> = TODO("Skeleton: implemented with the mock network layer")

    override suspend fun createTask(draft: TaskDraft): Result<Task> =
        TODO("Skeleton: implemented with the mock network layer")

    override suspend fun updateTask(id: String, draft: TaskDraft): Result<Task> =
        TODO("Skeleton: implemented with the mock network layer")

    override suspend fun setCompleted(id: String, completed: Boolean): Result<Unit> =
        TODO("Skeleton: implemented with the mock network layer")

    override suspend fun deleteTask(id: String): Result<Unit> =
        TODO("Skeleton: implemented with the mock network layer")
}
