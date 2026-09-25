package com.bopape.studentassistant.data.model

/** Priority levels used by the Academic Priority System described in the Part 1 design. */
object Priority {
    const val HIGH = "High"
    const val MEDIUM = "Medium"
    const val LOW = "Low"

    /** Lower number sorts first: High-priority, soon-due work rises to the top. */
    fun weight(priority: String): Int = when (priority) {
        HIGH -> 0
        MEDIUM -> 1
        else -> 2
    }
}

/**
 * An assignment or academic task. [moduleId] is optional so a task can be created
 * before a module exists, or for general (non-module) academic work.
 * [dueDate] is stored as an ISO date string, e.g. "2026-08-24".
 */
data class Task(
    val id: Int = 0,
    val title: String,
    val description: String = "",
    val moduleId: Int? = null,
    val dueDate: String,
    val priority: String = Priority.MEDIUM,
    val completed: Boolean = false
)
