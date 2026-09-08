package com.rounds.test.to_dolist.data.tasks.api

import com.rounds.test.to_dolist.core.common.time.Clock
import com.rounds.test.to_dolist.data.tasks.api.model.TaskPayload
import com.rounds.test.to_dolist.tasks.error.DataError
import com.rounds.test.to_dolist.tasks.error.DataException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import kotlin.random.Random

/**
 * The mock source is the one component whose whole point is to be slow and unreliable, so its tests
 * must not be. Every case here either seeds the dice or switches them off (NFR-05): no assertion
 * depends on a roll the test did not choose.
 */
class FakeTaskApiTest {

    private val clock = FixedClock(Instant.parse("2026-01-01T00:00:00Z"))

    private fun api(seed: Int = 1, failureRate: Double = 0.0) =
        FakeTaskApi(clock, Random(seed)).apply { this.failureRate = failureRate }

    @Test
    fun `seed data matches the brief`() = runTest {
        val tasks = api().getTasks()

        assertEquals(4, tasks.size)
        assertEquals("Renew domain registration", tasks[0].title)
        assertEquals("Expires end of month", tasks[0].notes)
        assertEquals("High", tasks[0].priority)
        assertEquals(false, tasks[0].completed)
        assertEquals("Book dentist", tasks[2].title)
        assertEquals(true, tasks[2].completed)
        assertEquals(
            "Migrate the analytics pipeline to the new warehouse and validate dashboards",
            tasks[3].title,
        )
        assertTrue(tasks.all { it.createdAtEpochMillis == clock.now().toEpochMilli() })
    }

    @Test
    fun `a read takes between 300 and 800 ms`() = runTest {
        val api = api()

        repeat(50) {
            val start = currentTime
            api.getTasks()
            val elapsed = currentTime - start

            assertTrue(
                "latency $elapsed ms outside the specified window",
                elapsed in FakeTaskApi.MIN_LATENCY_MS..FakeTaskApi.MAX_LATENCY_MS,
            )
        }
    }

    @Test
    fun `the default failure rate is roughly 15 percent`() = runTest {
        val api = api(failureRate = FakeTaskApi.DEFAULT_FAILURE_RATE)
        var failures = 0

        repeat(CALLS) {
            runCatching { api.getTasks() }.onFailure { failures++ }
        }

        val rate = failures.toDouble() / CALLS
        assertTrue("observed failure rate $rate", rate in 0.12..0.18)
    }

    @Test
    fun `a zero failure rate never fails and a rate of one always does`() = runTest {
        val never = api(failureRate = 0.0)
        repeat(100) { never.getTasks() }

        val always = api(failureRate = 1.0)
        repeat(10) {
            assertEquals(DataError.Network, always.readError())
        }
    }

    @Test
    fun `an unknown id is NotFound every time, never a dice roll`() = runTest {
        val api = api(failureRate = 0.0)

        repeat(10) {
            val error = runCatching { api.getTask("no-such-id") }.dataError()
            assertEquals(DataError.NotFound, error)
        }
        assertEquals(DataError.NotFound, runCatching { api.deleteTask("no-such-id") }.dataError())
        assertEquals(
            DataError.NotFound,
            runCatching { api.updateTask("no-such-id", payload()) }.dataError(),
        )
    }

    @Test
    fun `create, read, update and delete round-trip`() = runTest {
        val api = api()

        val created = api.createTask(payload(title = "Write the walkthrough"))
        assertEquals("Write the walkthrough", created.title)
        assertEquals(clock.now().toEpochMilli(), created.createdAtEpochMillis)
        assertEquals(created, api.getTask(created.id))
        assertEquals(5, api.getTasks().size)

        val updated = api.updateTask(created.id, payload(title = "Renamed", completed = true))
        assertEquals(created.id, updated.id)
        assertEquals("Renamed", updated.title)
        assertEquals(true, updated.completed)
        assertEquals(updated, api.getTask(created.id))

        api.deleteTask(created.id)
        assertEquals(4, api.getTasks().size)
        assertEquals(DataError.NotFound, runCatching { api.getTask(created.id) }.dataError())
    }

    @Test
    fun `ids are assigned by the source and never repeat`() = runTest {
        val api = api()

        val ids = List(20) { api.createTask(payload()).id }

        assertEquals(20, ids.toSet().size)
        assertTrue(api.getTasks().map { it.id }.toSet().containsAll(ids))
    }

    /**
     * Runs on a real multi-threaded dispatcher rather than the test scheduler: a `Mutex` that is not
     * doing its job is invisible when every coroutine shares one thread.
     */
    @Test
    fun `concurrent writes do not corrupt the store`() = runBlocking {
        val api = api()

        val created = withContext(Dispatchers.Default) {
            List(CONCURRENT_WRITES) { index ->
                async { api.createTask(payload(title = "Task $index")) }
            }.awaitAll()
        }

        val stored = api.getTasks()
        assertEquals(4 + CONCURRENT_WRITES, stored.size)
        assertEquals(stored.size, stored.map { it.id }.toSet().size)
        assertTrue(stored.map { it.id }.containsAll(created.map { it.id }))
    }

    private suspend fun FakeTaskApi.readError(): DataError = runCatching { getTasks() }.dataError()

    private fun Result<*>.dataError(): DataError {
        val failure = requireNotNull(exceptionOrNull()) { "expected the call to fail" }
        assertTrue("expected a DataException, got $failure", failure is DataException)
        return (failure as DataException).error
    }

    private fun payload(
        title: String = "Task",
        notes: String? = null,
        priority: String = "MEDIUM",
        completed: Boolean = false,
    ) = TaskPayload(title = title, notes = notes, priority = priority, completed = completed)

    private class FixedClock(private val instant: Instant) : Clock {
        override fun now(): Instant = instant
    }

    private companion object {
        const val CALLS = 4000
        const val CONCURRENT_WRITES = 64
    }
}
