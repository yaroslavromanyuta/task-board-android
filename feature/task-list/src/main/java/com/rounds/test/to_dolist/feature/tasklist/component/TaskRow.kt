package com.rounds.test.to_dolist.feature.tasklist.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.rounds.test.to_dolist.core.ui.component.PriorityIndicator
import com.rounds.test.to_dolist.core.ui.format.RelativeDate
import com.rounds.test.to_dolist.core.ui.format.asText
import com.rounds.test.to_dolist.core.ui.format.relativeDateOf
import com.rounds.test.to_dolist.core.ui.theme.TodoListTheme
import com.rounds.test.to_dolist.feature.tasklist.R
import com.rounds.test.to_dolist.tasks.model.Task
import com.rounds.test.to_dolist.tasks.model.TaskPriority
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * One row: title, priority, a way to complete it, a way to delete it. Stateless — the row reports
 * intent and renders whatever it is handed, so it previews without a ViewModel.
 *
 * The title is capped at two lines with an ellipsis rather than left to wrap: the seed data contains a
 * deliberately long title, and an uncapped row would grow until it pushed the rest of the list off
 * screen (FR-01).
 *
 * [now] is a parameter with a sensible default so that previews and tests can pin "today" instead of
 * rendering something different every day.
 *
 * [isToggling] disables the checkbox while its write is in flight. The control renders the cache and
 * the cache does not move until the call returns, so a second tap inside that window would read the
 * same stale value and mean the same thing as the first - two taps that do not toggle back.
 */
@Composable
fun TaskRow(
    task: Task,
    onClick: () -> Unit,
    onToggleCompleted: (Boolean) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    isToggling: Boolean = false,
    now: Instant = remember { Instant.now() },
) {
    // The label has to name the action the tap performs, or a screen reader announces the
    // opposite of what happens on a row that is already done (NFR-09).
    val toggleLabel = stringResource(
        if (task.isCompleted) R.string.task_list_action_toggle_done
        else R.string.task_list_action_toggle,
    )
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = task.isCompleted,
            onCheckedChange = onToggleCompleted,
            enabled = !isToggling,
            modifier = Modifier.semantics { contentDescription = toggleLabel },
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = task.title,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                PriorityIndicator(priority = task.priority)
                // A task with no due date renders nothing here, not the word "null" (FR-13).
                task.dueDate?.let { dueDate -> DueDate(dueDate = dueDate, now = now) }
            }
        }
        IconButton(onClick = onDelete) {
            Icon(
                imageVector = Icons.Outlined.Delete,
                contentDescription = stringResource(R.string.task_list_action_delete),
            )
        }
    }
}

/**
 * An overdue task is the one case worth colouring: it is the only state where the date is telling the
 * user to do something rather than just informing them.
 */
@Composable
private fun DueDate(
    dueDate: Instant,
    now: Instant,
) {
    val relative = relativeDateOf(target = dueDate, now = now)
    val overdue = relative is RelativeDate.Yesterday || relative is RelativeDate.DaysAgo ||
        (relative is RelativeDate.Absolute && dueDate.isBefore(now))

    Text(
        text = relative.asText(),
        style = MaterialTheme.typography.labelMedium,
        color = if (overdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@PreviewLightDark
@Composable
private fun TaskRowPreview() {
    TodoListTheme(dynamicColor = false) {
        TaskRow(
            task = Task(
                id = "1",
                title = "Renew passport",
                notes = null,
                priority = TaskPriority.HIGH,
                isCompleted = false,
                createdAt = Instant.EPOCH,
                dueDate = PreviewNow.plus(3, ChronoUnit.DAYS),
            ),
            onClick = {},
            onToggleCompleted = {},
            onDelete = {},
            now = PreviewNow,
        )
    }
}

/** The seed row that exists to stress the layout; previewed so a regression is visible without a device. */
@PreviewLightDark
@Composable
private fun TaskRowLongTitlePreview() {
    TodoListTheme(dynamicColor = false) {
        TaskRow(
            task = Task(
                id = "4",
                title = "Migrate the analytics pipeline to the new warehouse and validate dashboards",
                notes = "Long one \u2014 check layout",
                priority = TaskPriority.MEDIUM,
                isCompleted = false,
                createdAt = Instant.EPOCH,
                dueDate = PreviewNow.minus(1, ChronoUnit.DAYS),
            ),
            onClick = {},
            onToggleCompleted = {},
            onDelete = {},
            now = PreviewNow,
        )
    }
}

/** Pinned, so the previews do not say something different tomorrow. */
private val PreviewNow: Instant = Instant.parse("2026-09-08T09:00:00Z")
