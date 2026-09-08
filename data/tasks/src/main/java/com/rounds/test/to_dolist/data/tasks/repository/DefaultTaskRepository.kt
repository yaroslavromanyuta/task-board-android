package com.rounds.test.to_dolist.data.tasks.repository

import com.rounds.test.to_dolist.core.common.coroutines.suspendRunCatching
import com.rounds.test.to_dolist.core.common.di.IoDispatcher
import com.rounds.test.to_dolist.data.tasks.api.TaskApi
import com.rounds.test.to_dolist.data.tasks.cache.InMemoryTaskCache
import com.rounds.test.to_dolist.data.tasks.mapper.toDomain
import com.rounds.test.to_dolist.data.tasks.mapper.toPayload
import com.rounds.test.to_dolist.tasks.error.DataError
import com.rounds.test.to_dolist.tasks.error.DataException
import com.rounds.test.to_dolist.tasks.model.Task
import com.rounds.test.to_dolist.tasks.model.TaskDraft
import com.rounds.test.to_dolist.tasks.model.toDraft
import com.rounds.test.to_dolist.tasks.repository.TaskRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Cache is the single source of truth for reads; the API is the source of truth for writes. Each write
 * goes to the API first and only updates the cache once the call succeeds, so a failed request never
 * leaves the list showing something the "server" does not have.
 *
 * Every call is wrapped so the caller sees `Result` whose failure is always a [DataException] —
 * transport types stop here.
 */
@Singleton
class DefaultTaskRepository @Inject constructor(
    private val api: TaskApi,
    private val cache: InMemoryTaskCache,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : TaskRepository {

    override fun observeTasks(): Flow<List<Task>> = cache.observe()

    override suspend fun refresh(): Result<Unit> = dataCatching {
        cache.replaceAll(api.getTasks().map { it.toDomain() })
    }

    /**
     * Deliberately does not write to the cache: the cache mirrors what a list load returned, and one
     * task fetched by id must not be able to insert a row the list never saw.
     */
    override suspend fun getTask(id: String): Result<Task> = dataCatching {
        api.getTask(id).toDomain()
    }

    override suspend fun createTask(draft: TaskDraft): Result<Task> = dataCatching {
        api.createTask(draft.toPayload()).toDomain().also(cache::upsert)
    }

    /**
     * The draft carries only the editable half of a task, so the completion flag has to be carried
     * over rather than defaulted — otherwise editing a completed task's title would silently
     * un-complete it. It comes from the cache when the list is loaded and from the source when it is
     * not, which is the case after process death on the editor screen.
     */
    override suspend fun updateTask(id: String, draft: TaskDraft): Result<Task> = dataCatching {
        val completed = currentCompleted(id)
        api.updateTask(id, draft.toPayload(completed = completed)).toDomain().also(cache::upsert)
    }

    /**
     * `TaskApi` has no "set completed" operation by design, so this is an update carrying the current
     * fields plus the new flag. The current fields come from the cache when they are there, and from
     * the source when they are not, rather than from the caller — a stale row cannot overwrite a title
     * someone edited elsewhere.
     */
    override suspend fun setCompleted(id: String, completed: Boolean): Result<Unit> = dataCatching {
        val current = cache.snapshot().firstOrNull { it.id == id } ?: api.getTask(id).toDomain()
        api.updateTask(id, current.toDraft().toPayload(completed = completed))
            .toDomain()
            .also(cache::upsert)
        Unit
    }

    private suspend fun currentCompleted(id: String): Boolean =
        cache.snapshot().firstOrNull { it.id == id }?.isCompleted ?: api.getTask(id).completed

    override suspend fun deleteTask(id: String): Result<Unit> = dataCatching {
        api.deleteTask(id)
        cache.remove(id)
    }

    /**
     * NFR-04 in one place: the data layer's failures are *always* a [DataException], so a ViewModel
     * can call `asDataError()` without a defensive branch. The API only throws typed failures, but a
     * mapper or a future implementation could throw something else, and that would otherwise reach the
     * presentation layer as an untyped crash.
     */
    private suspend fun <T> dataCatching(block: suspend () -> T): Result<T> =
        withContext(ioDispatcher) {
            suspendRunCatching { block() }
                .recoverCatching { throwable ->
                    throw if (throwable is DataException) throwable
                    else DataException(DataError.Unknown, throwable)
                }
        }
}
