package com.rounds.test.to_dolist.tasks.model

import com.rounds.test.to_dolist.core.testing.TestData
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The orderings live in the domain, so they are tested there rather than through a screen. Stability
 * is part of the contract: a re-sort must not shuffle rows that compare equal, or the list appears to
 * move under the user for no reason.
 */
class TaskSortTest {

    private val completedHigh = TestData.task(id = "1", priority = TaskPriority.HIGH, isCompleted = true)
    private val low = TestData.task(id = "2", priority = TaskPriority.LOW)
    private val medium = TestData.task(id = "3", priority = TaskPriority.MEDIUM)
    private val tasks = listOf(completedHigh, low, medium)

    @Test
    fun `default keeps the order the source returned`() {
        assertEquals(listOf("1", "2", "3"), tasks.sortedBy(TaskSort.DEFAULT).ids())
    }

    @Test
    fun `priority puts high first`() {
        assertEquals(listOf("1", "3", "2"), tasks.sortedBy(TaskSort.PRIORITY).ids())
    }

    @Test
    fun `completion puts unfinished first`() {
        assertEquals(listOf("2", "3", "1"), tasks.sortedBy(TaskSort.COMPLETION).ids())
    }

    @Test
    fun `tasks that compare equal keep their relative order`() {
        val sameEverything = listOf(
            TestData.task(id = "a", priority = TaskPriority.MEDIUM),
            TestData.task(id = "b", priority = TaskPriority.MEDIUM),
            TestData.task(id = "c", priority = TaskPriority.MEDIUM),
        )

        assertEquals(listOf("a", "b", "c"), sameEverything.sortedBy(TaskSort.PRIORITY).ids())
        assertEquals(listOf("a", "b", "c"), sameEverything.sortedBy(TaskSort.COMPLETION).ids())
    }

    private fun List<Task>.ids(): List<String> = map { it.id }
}
