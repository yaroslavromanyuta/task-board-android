package com.rounds.test.to_dolist.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.rounds.test.to_dolist.core.ui.R
import com.rounds.test.to_dolist.core.ui.theme.PriorityHigh
import com.rounds.test.to_dolist.core.ui.theme.PriorityLow
import com.rounds.test.to_dolist.core.ui.theme.PriorityMedium
import com.rounds.test.to_dolist.core.ui.theme.TodoListTheme
import com.rounds.test.to_dolist.tasks.model.TaskPriority

/**
 * Both screens show priority, so the mapping from enum to colour and label is defined once. Colour is
 * paired with a text label rather than used alone — colour on its own is not an accessible signal.
 */
@Composable
fun PriorityIndicator(
    priority: TaskPriority,
    modifier: Modifier = Modifier,
) {
    val label = stringResource(priority.labelRes())
    Row(
        modifier = modifier.clearAndSetSemantics { contentDescription = label },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(color = priority.color())
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun Box(color: Color) {
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .size(8.dp)
            .background(color = color, shape = CircleShape),
    )
}

@Composable
private fun TaskPriority.color(): Color = when (this) {
    TaskPriority.LOW -> PriorityLow
    TaskPriority.MEDIUM -> PriorityMedium
    TaskPriority.HIGH -> PriorityHigh
}

private fun TaskPriority.labelRes(): Int = when (this) {
    TaskPriority.LOW -> R.string.core_ui_priority_low
    TaskPriority.MEDIUM -> R.string.core_ui_priority_medium
    TaskPriority.HIGH -> R.string.core_ui_priority_high
}

@Preview(showBackground = true)
@Composable
private fun PriorityIndicatorPreview() {
    TodoListTheme(dynamicColor = false) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            TaskPriority.entries.forEach { PriorityIndicator(priority = it) }
        }
    }
}
