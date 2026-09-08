package com.rounds.test.to_dolist.core.ui.format

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.rounds.test.to_dolist.core.ui.R
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.ChronoUnit

/**
 * How far away a date is, in the terms a person actually uses. Deliberately a value rather than a
 * string: [relativeDateOf] is the arithmetic and is unit-tested against fixed dates, while [asText]
 * is the wording and stays in the resources with everything else the app says.
 *
 * Beyond a week in either direction, "in 23 days" stops being easier to read than the date itself, so
 * the scale gives up and shows [Absolute].
 */
sealed interface RelativeDate {

    data object Today : RelativeDate

    data object Tomorrow : RelativeDate

    data object Yesterday : RelativeDate

    data class InDays(val days: Long) : RelativeDate

    data class DaysAgo(val days: Long) : RelativeDate

    data class Absolute(val date: LocalDate) : RelativeDate
}

/**
 * A [Task][com.rounds.test.to_dolist.tasks.model.Task] carries an [Instant], because a point in time
 * is what survives a transport unambiguously. "Which day is that" is a question only the viewer's
 * [zone] can answer, so the conversion happens here and not in the domain.
 */
fun relativeDateOf(
    target: Instant,
    now: Instant,
    zone: ZoneId = ZoneId.systemDefault(),
): RelativeDate {
    val targetDate = target.atZone(zone).toLocalDate()
    val today = now.atZone(zone).toLocalDate()

    return when (val days = ChronoUnit.DAYS.between(today, targetDate)) {
        0L -> RelativeDate.Today
        1L -> RelativeDate.Tomorrow
        -1L -> RelativeDate.Yesterday
        in 2L..MAX_RELATIVE_DAYS -> RelativeDate.InDays(days)
        in -MAX_RELATIVE_DAYS..-2L -> RelativeDate.DaysAgo(-days)
        else -> RelativeDate.Absolute(targetDate)
    }
}

@Composable
fun RelativeDate.asText(): String = when (this) {
    RelativeDate.Today -> stringResource(R.string.core_ui_due_today)
    RelativeDate.Tomorrow -> stringResource(R.string.core_ui_due_tomorrow)
    RelativeDate.Yesterday -> stringResource(R.string.core_ui_due_yesterday)

    is RelativeDate.InDays ->
        pluralStringResource(R.plurals.core_ui_due_in_days, days.toInt(), days.toInt())

    is RelativeDate.DaysAgo ->
        pluralStringResource(R.plurals.core_ui_due_days_ago, days.toInt(), days.toInt())

    is RelativeDate.Absolute -> stringResource(
        R.string.core_ui_due_on,
        date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)),
    )
}

/** A week either side. Past that, the date reads better than the arithmetic. */
private const val MAX_RELATIVE_DAYS = 7L
