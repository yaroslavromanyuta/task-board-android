package com.rounds.test.to_dolist.core.ui.format

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * Date arithmetic against a fixed "now", never the wall clock: a test that says "tomorrow" has to mean
 * the same thing at 23:59 as it does at 09:00.
 *
 * The zone is pinned too. Whether a task is due "today" is a question about the viewer's calendar, and
 * a machine in a different zone must not get a different answer here than the assertions were written
 * for.
 */
class RelativeDateTest {

    private val zone: ZoneId = ZoneId.of("Europe/Kyiv")
    private val now: Instant = Instant.parse("2026-09-08T09:00:00Z")

    @Test
    fun `the same day is today, whatever the time of day`() {
        assertEquals(RelativeDate.Today, relativeDateOf(now, now, zone))
        assertEquals(RelativeDate.Today, relativeDateOf(inDays(0, atHour = 23), now, zone))
    }

    @Test
    fun `one day either side reads as tomorrow and yesterday`() {
        assertEquals(RelativeDate.Tomorrow, relativeDateOf(inDays(1), now, zone))
        assertEquals(RelativeDate.Yesterday, relativeDateOf(inDays(-1), now, zone))
    }

    @Test
    fun `a few days out counts the days`() {
        assertEquals(RelativeDate.InDays(3), relativeDateOf(inDays(3), now, zone))
        assertEquals(RelativeDate.DaysAgo(2), relativeDateOf(inDays(-2), now, zone))
    }

    @Test
    fun `a week is still counted, and eight days is not`() {
        assertEquals(RelativeDate.InDays(7), relativeDateOf(inDays(7), now, zone))
        assertEquals(RelativeDate.DaysAgo(7), relativeDateOf(inDays(-7), now, zone))

        assertEquals(
            RelativeDate.Absolute(LocalDate.of(2026, 9, 16)),
            relativeDateOf(inDays(8), now, zone),
        )
        assertEquals(
            RelativeDate.Absolute(LocalDate.of(2026, 8, 31)),
            relativeDateOf(inDays(-8), now, zone),
        )
    }

    /**
     * 22:00 UTC is already the next calendar day in Kyiv. The difference is a day boundary, not
     * twenty-four hours, which is the whole reason this takes a zone.
     */
    @Test
    fun `the day boundary is the viewer's, not UTC's`() {
        val lateEvening = Instant.parse("2026-09-08T22:00:00Z")

        assertEquals(RelativeDate.Tomorrow, relativeDateOf(lateEvening, now, zone))
        assertEquals(RelativeDate.Today, relativeDateOf(lateEvening, now, ZoneId.of("UTC")))
    }

    private fun inDays(days: Long, atHour: Int? = null): Instant {
        val shifted = now.plus(days, ChronoUnit.DAYS)
        if (atHour == null) return shifted

        return shifted.atZone(zone).withHour(atHour).withMinute(0).toInstant()
    }
}
