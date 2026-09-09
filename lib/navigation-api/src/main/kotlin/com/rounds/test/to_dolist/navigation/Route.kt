package com.rounds.test.to_dolist.navigation

import kotlinx.serialization.Serializable

/**
 * Type-safe destinations (navigation-compose 2.8+), declared in a shared contract module so a feature
 * can navigate to another feature while depending only on the type, never on that feature's module.
 * `:app` is still the only place that maps a route to the composable behind it.
 */
sealed interface Route {

    @Serializable
    data object TaskList : Route {

        /**
         * Key the editor's "a save landed" result is handed back under, written onto this
         * destination's `SavedStateHandle` by whoever pops the editor and read by the list.
         *
         * It lives here rather than in either feature because both ends need the same string and the
         * two features cannot see each other — the same reason the routes themselves are here.
         */
        const val TASK_SAVED_RESULT = "task_saved"
    }

    /** [taskId] null means "create a new task"; a non-null id opens that task for viewing and editing. */
    @Serializable
    data class TaskEditor(val taskId: String? = null) : Route {

        companion object {
            /**
             * The name this destination's one argument is stored under. Navigation derives it from the
             * property name, so a screen can read the argument straight out of `SavedStateHandle`
             * instead of going through `toRoute()`, which needs an Android runtime to decode and
             * therefore cannot be exercised by a JVM unit test.
             *
             * `RouteTest` asserts this constant still matches the serialised property, so the two
             * cannot drift apart silently.
             */
            const val TASK_ID_ARG = "taskId"
        }
    }
}
