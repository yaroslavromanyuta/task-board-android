package com.rounds.test.to_dolist.navigation

import kotlinx.serialization.Serializable

/**
 * Type-safe destinations (navigation-compose 2.8+), declared in a shared contract module so a feature
 * can navigate to another feature while depending only on the type, never on that feature's module.
 * `:app` is still the only place that maps a route to the composable behind it.
 */
sealed interface Route {

    @Serializable
    data object TaskList : Route

    /** [taskId] null means "create a new task"; a non-null id opens that task for viewing and editing. */
    @Serializable
    data class TaskEditor(val taskId: String? = null) : Route
}
