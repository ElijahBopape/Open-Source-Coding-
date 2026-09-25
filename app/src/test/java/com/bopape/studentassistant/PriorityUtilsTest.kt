package com.bopape.studentassistant

import com.bopape.studentassistant.data.model.Priority
import com.bopape.studentassistant.data.model.Task
import com.bopape.studentassistant.util.PriorityUtils
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for the Academic Priority System (PriorityUtils), the app's main
 * innovative feature. These run on the JVM with no Android dependency, per the
 * automated testing requirement in the assessment brief.
 */
class PriorityUtilsTest {

    private fun task(title: String, priority: String, dueDate: String, completed: Boolean = false) =
        Task(id = 0, title = title, dueDate = dueDate, priority = priority, completed = completed)

    @Test
    fun sortByPriority_ordersHighBeforeMediumBeforeLow() {
        val tasks = listOf(
            task("Low priority", Priority.LOW, "2026-08-24"),
            task("High priority", Priority.HIGH, "2026-08-24"),
            task("Medium priority", Priority.MEDIUM, "2026-08-24")
        )

        val sorted = PriorityUtils.sortByPriority(tasks)

        assertEquals("High priority", sorted[0].title)
        assertEquals("Medium priority", sorted[1].title)
        assertEquals("Low priority", sorted[2].title)
    }

    @Test
    fun sortByPriority_ordersBySoonestDueDateWithinSamePriority() {
        val tasks = listOf(
            task("Due later", Priority.HIGH, "2026-09-01"),
            task("Due sooner", Priority.HIGH, "2026-08-20")
        )

        val sorted = PriorityUtils.sortByPriority(tasks)

        assertEquals("Due sooner", sorted[0].title)
        assertEquals("Due later", sorted[1].title)
    }

    @Test
    fun sortByPriority_putsCompletedTasksLast() {
        val tasks = listOf(
            task("Done but high priority", Priority.HIGH, "2026-08-20", completed = true),
            task("Pending low priority", Priority.LOW, "2026-08-25", completed = false)
        )

        val sorted = PriorityUtils.sortByPriority(tasks)

        assertEquals("Pending low priority", sorted[0].title)
        assertEquals("Done but high priority", sorted[1].title)
    }

    @Test
    fun topPriorityTasks_excludesCompletedAndRespectsLimit() {
        val tasks = listOf(
            task("Complete 1", Priority.HIGH, "2026-08-20", completed = true),
            task("Pending 1", Priority.HIGH, "2026-08-21"),
            task("Pending 2", Priority.MEDIUM, "2026-08-22"),
            task("Pending 3", Priority.LOW, "2026-08-23")
        )

        val top = PriorityUtils.topPriorityTasks(tasks, limit = 2)

        assertEquals(2, top.size)
        assertEquals("Pending 1", top[0].title)
        assertEquals("Pending 2", top[1].title)
    }
}
