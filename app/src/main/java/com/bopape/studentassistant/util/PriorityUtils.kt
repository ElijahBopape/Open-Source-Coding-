package com.bopape.studentassistant.util

import com.bopape.studentassistant.data.model.Exam
import com.bopape.studentassistant.data.model.Priority
import com.bopape.studentassistant.data.model.Task
import java.time.LocalDate

/**
 * The Academic Priority System described in the Part 1 design document (section 3.12):
 * rather than sorting tasks purely alphabetically or chronologically, incomplete work
 * is surfaced first, ordered by priority level and then by how soon it is due.
 *
 * Final PoE addition - exam proximity: a task belonging to a module that has an
 * exam within [EXAM_BOOST_DAYS] days is treated as one priority level higher
 * (Low -> Medium, Medium -> High), because revision for that module has become
 * more urgent than its label suggests.
 *
 * Kept as a plain Kotlin object with no Android dependencies so it can be covered by
 * fast JVM unit tests (see app/src/test).
 */
object PriorityUtils {

    const val EXAM_BOOST_DAYS = 7

    /** Module ids that have an exam between today and [EXAM_BOOST_DAYS] days from now. */
    fun modulesWithExamSoon(exams: List<Exam>, today: LocalDate = LocalDate.now()): Set<Int> =
        exams.filter { exam ->
            val days = DateUtils.daysUntil(exam.date, today)
            exam.moduleId != null && days != null && days in 0..EXAM_BOOST_DAYS
        }.mapNotNull { it.moduleId }.toSet()

    /** The priority weight after the exam-proximity boost (0 = most urgent). */
    fun effectiveWeight(task: Task, examModules: Set<Int>): Int {
        val base = Priority.weight(task.priority)
        val boosted = task.moduleId != null && task.moduleId in examModules
        return if (boosted) (base - 1).coerceAtLeast(0) else base
    }

    /** True when the task's priority was raised because its module has an exam soon. */
    fun isExamBoosted(task: Task, examModules: Set<Int>): Boolean =
        !task.completed && task.moduleId != null && task.moduleId in examModules &&
            Priority.weight(task.priority) > 0

    /** Sorts tasks: incomplete before completed, then effective priority, then soonest due date first. */
    fun sortByPriority(
        tasks: List<Task>,
        exams: List<Exam> = emptyList(),
        today: LocalDate = LocalDate.now()
    ): List<Task> {
        val examModules = modulesWithExamSoon(exams, today)
        return tasks.sortedWith(
            compareBy(
                { it.completed },
                { effectiveWeight(it, examModules) },
                { it.dueDate }
            )
        )
    }

    /** Returns the top [limit] incomplete tasks, ordered by priority, for the dashboard. */
    fun topPriorityTasks(
        tasks: List<Task>,
        limit: Int = 3,
        exams: List<Exam> = emptyList(),
        today: LocalDate = LocalDate.now()
    ): List<Task> = sortByPriority(tasks.filter { !it.completed }, exams, today).take(limit)
}
