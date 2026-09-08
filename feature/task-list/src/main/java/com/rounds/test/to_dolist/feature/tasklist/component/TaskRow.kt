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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.rounds.test.to_dolist.core.ui.component.PriorityIndicator
import com.rounds.test.to_dolist.core.ui.theme.TodoListTheme
import com.rounds.test.to_dolist.feature.tasklist.R
import com.rounds.test.to_dolist.tasks.model.Task
import com.rounds.test.to_dolist.tasks.model.TaskPriority
import java.time.Instant

/**
 * One row: title, priority, a way to complete it, a way to delete it. Stateless — the row reports
 * intent and renders whatever it is handed, so it previews without a ViewModel.
 *
 * The title is capped at two lines with an ellipsis rather than left to wrap: the seed data contains a
 * deliberately long title, and an uncapped row would grow until it pushed the rest of the list off
 * screen (FR-01).
 */
@Composable
fun TaskRow(
    task: Task,
    onClick: () -> Unit,
    onToggleCompleted: (Boolean) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val toggleLabel = stringResource(R.string.task_list_action_toggle)
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
            PriorityIndicator(priority = task.priority)
        }
        IconButton(onClick = onDelete) {
            Icon(
                imageVector = Icons.Outlined.Delete,
                contentDescription = stringResource(R.string.task_list_action_delete),
            )
        }
    }
}

@Preview(showBackground = true)
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
            ),
            onClick = {},
            onToggleCompleted = {},
            onDelete = {},
        )
    }
}

/** The seed row that exists to stress the layout; previewed so a regression is visible without a device. */
@Preview(showBackground = true)
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
            ),
            onClick = {},
            onToggleCompleted = {},
            onDelete = {},
        )
    }
}
