package com.rounds.test.to_dolist.data.tasks.cache

import com.rounds.test.to_dolist.tasks.model.Task
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The cache the UI actually reads. The repository writes here after every successful network call, so
 * the list screen re-renders from one stream instead of each screen fetching for itself.
 *
 * In-memory by choice: the brief needs loading/empty/error states and a believable async source, not
 * durability across process death. Swapping this for Room later changes this file and the DI module,
 * nothing above them.
 */
@Singleton
class InMemoryTaskCache @Inject constructor() {

    private val tasks = MutableStateFlow<List<Task>>(emptyList())

    fun observe(): Flow<List<Task>> = tasks

    fun snapshot(): List<Task> = tasks.value

    fun replaceAll(tasks: List<Task>) {
        this.tasks.value = tasks
    }

    /** Replaces by id rather than appending blindly, so a re-saved task cannot appear twice. */
    fun upsert(task: Task) {
        tasks.update { current ->
            val index = current.indexOfFirst { it.id == task.id }
            if (index < 0) current + task else current.toMutableList().apply { this[index] = task }
        }
    }

    fun remove(id: String) {
        tasks.update { current -> current.filterNot { it.id == id } }
    }
}
