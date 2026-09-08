package com.rounds.test.to_dolist.data.tasks.api

import com.rounds.test.to_dolist.core.common.time.Clock
import com.rounds.test.to_dolist.data.tasks.api.model.TaskDto
import com.rounds.test.to_dolist.data.tasks.api.model.TaskPayload
import com.rounds.test.to_dolist.tasks.error.DataError
import com.rounds.test.to_dolist.tasks.error.DataException
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

/**
 * Stand-in for the backend, built to behave like one rather than like a local list:
 *  - every call suspends for a uniform [latencyMillis] ms, so "loading" is a state a user can
 *    actually see;
 *  - every call has a [failureRate] chance of throwing `DataException(DataError.Network)`, so the
 *    error path is exercised by simply using the app;
 *  - an unknown id is a deterministic `DataError.NotFound`, never a dice roll;
 *  - the store is a `MutableList<TaskDto>` behind a [Mutex], because callers hit it concurrently;
 *  - ids and `createdAt` are assigned here, from the injected [Clock].
 *
 * [random] is injected and both [failureRate] and [latencyMillis] are `var`s, which is what makes
 * the two things a real transport does unpredictably testable: tests seed the dice, and a demo — or
 * a QA journey — turns them off or slows them down.
 */
@Singleton
class FakeTaskApi @Inject constructor(
    private val clock: Clock,
    private val random: Random,
) : TaskApi {

    private val mutex = Mutex()
    private val store = mutableListOf<TaskDto>()
    private var lastId = 0

    /**
     * Probability that any one call fails. Set to `0.0` to demo the happy path on cue and to `1.0` to
     * demo the error state; the default is the ~15% the brief asks for.
     */
    var failureRate: Double = DEFAULT_FAILURE_RATE

    /**
     * How long a call takes. The default is the 300-800 ms the brief asks for; widening it is how a
     * scripted UI test gets a loading state that outlives the time it takes to read the screen.
     */
    var latencyMillis: LongRange = MIN_LATENCY_MS..MAX_LATENCY_MS

    init {
        SEED_TASKS.forEach { seed ->
            store += seed.toDto(id = newId(), createdAt = clock.now())
        }
    }

    override suspend fun getTasks(): List<TaskDto> = call { store.toList() }

    override suspend fun getTask(id: String): TaskDto = call { store.requireTask(id) }

    override suspend fun createTask(payload: TaskPayload): TaskDto = call {
        TaskDto(
            id = newId(),
            title = payload.title,
            notes = payload.notes,
            priority = payload.priority,
            completed = payload.completed,
            createdAtEpochMillis = clock.now().toEpochMilli(),
            dueDateEpochMillis = payload.dueDateEpochMillis,
        ).also { store += it }
    }

    override suspend fun updateTask(id: String, payload: TaskPayload): TaskDto = call {
        val index = store.indexOfFirst { it.id == id }
        if (index < 0) throw DataException(DataError.NotFound)
        store[index].copy(
            title = payload.title,
            notes = payload.notes,
            priority = payload.priority,
            completed = payload.completed,
            dueDateEpochMillis = payload.dueDateEpochMillis,
        ).also { store[index] = it }
    }

    override suspend fun deleteTask(id: String) = call {
        val removed = store.removeAll { it.id == id }
        if (!removed) throw DataException(DataError.NotFound)
    }

    /**
     * The shape every operation shares. The dice are rolled up front, under the lock, so a seeded
     * [random] produces the same sequence regardless of how calls interleave; the delay then happens
     * outside the lock, so concurrent callers overlap the way they would against a real server.
     */
    private suspend fun <T> call(block: () -> T): T {
        val (delayMillis, failed) = mutex.withLock {
            random.nextLong(latencyMillis.first, latencyMillis.last + 1) to
                (random.nextDouble() < failureRate)
        }
        delay(delayMillis)
        if (failed) throw DataException(DataError.Network)
        return mutex.withLock { block() }
    }

    private fun newId(): String = "task-${++lastId}"

    private fun List<TaskDto>.requireTask(id: String): TaskDto =
        firstOrNull { it.id == id } ?: throw DataException(DataError.NotFound)

    companion object {
        const val MIN_LATENCY_MS = 300L
        const val MAX_LATENCY_MS = 800L
        const val DEFAULT_FAILURE_RATE = 0.15
    }
}
