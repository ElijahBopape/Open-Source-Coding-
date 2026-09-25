package com.bopape.studentassistant.util

import com.bopape.studentassistant.data.model.Priority
import com.bopape.studentassistant.data.model.Task

/**
 * The Academic Priority System described in the Part 1 design document (section 3.12):
 * rather than sorting tasks purely alphabetically or chronologically, incomplete work
 * is surfaced first, ordered by priority level and then by how soon it is due.
 *
 * Kept as a plain Kotlin object with no Android dependencies so it can be covered by
 * fast JVM unit tests (see app/src/test).
 */
object PriorityUtils {

    /** Sorts tasks: incomplete before completed, then High > Medium > Low, then soonest due date first. */
    fun sortByPriority(tasks: List<Task>): List<Task> {
        return tasks.sortedWith(
            compareBy(
                { it.completed },
                { Priority.weight(it.priority) },
                { it.dueDate }
            )
        )
    }

    /** Returns the top [limit] incomplete tasks, ordered by priority, for the dashboard. */
    fun topPriorityTasks(tasks: List<Task>, limit: Int = 3): List<Task> {
        return sortByPriority(tasks.filter { !it.completed }).take(limit)
    }
}
