package com.rounds.test.to_dolist.navigation

import kotlinx.serialization.serializer
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * `Route.TaskEditor.TASK_ID_ARG` is the name navigation stores the argument under, and a screen reads
 * it by that name. Navigation derives the name from the serialised property, so renaming `taskId`
 * would break the read at runtime and nowhere else — this test is what turns that into a build
 * failure.
 */
class RouteTest {

    @Test
    fun `the task editor's argument name matches the constant screens read it by`() {
        val descriptor = serializer<Route.TaskEditor>().descriptor

        assertEquals(1, descriptor.elementsCount)
        assertEquals(Route.TaskEditor.TASK_ID_ARG, descriptor.getElementName(0))
    }
}
