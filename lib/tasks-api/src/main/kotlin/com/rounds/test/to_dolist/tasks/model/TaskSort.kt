package com.rounds.test.to_dolist.tasks.model

/**
 * How a list of tasks may be ordered. It lives in the domain rather than in the list screen for the
 * same reason [TaskPriority] is declared low-to-high: the ordering is a fact about tasks, and a second
 * surface — a widget, a wear app — must not be free to invent a different one.
 *
 * [DEFAULT] is the order the source returned. Completed tasks hold their position under it; moving
 * them is [COMPLETION], which is an explicit choice by the user (FR-11, open question Q-1).
 */
enum class TaskSort {
    DEFAULT,
    PRIORITY,
    COMPLETION,
}

/**
 * Every ordering is stable: tasks that compare equal keep the order the source gave them, so a re-sort
 * never shuffles rows the user was looking at.
 */
fun List<Task>.sortedBy(sort: TaskSort): List<Task> = when (sort) {
    TaskSort.DEFAULT -> this
    TaskSort.PRIORITY -> sortedByDescending { it.priority }
    TaskSort.COMPLETION -> sortedBy { it.isCompleted }
}
