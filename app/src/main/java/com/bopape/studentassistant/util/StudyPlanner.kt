package com.bopape.studentassistant.util

import com.bopape.studentassistant.data.model.Exam
import com.bopape.studentassistant.data.model.StudySession
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Study Planner logic (innovative feature, Part 1 design sections 3.8 and 3.12).
 *
 * "Smart plan" splits preparation for an exam or a large task into a number of
 * study sessions spread evenly between today and the day before the deadline,
 * so the student revises a little at a time instead of cramming the night before.
 * Pure Kotlin (no Android types) so the scheduling rules are unit-tested.
 */
object StudyPlanner {

    /**
     * Suggests up to [sessionCount] sessions before [deadline]. The first session
     * is today and the rest are spaced as evenly as the remaining days allow;
     * at most one session per day. Returns an empty list if the deadline is today
     * or already past.
     */
    fun suggestSessions(
        title: String,
        deadline: LocalDate,
        today: LocalDate,
        sessionCount: Int = 4,
        durationMinutes: Int = 60,
        startTime: String = "18:00",
        moduleId: Int? = null,
        examId: Int? = null,
        taskId: Int? = null
    ): List<StudySession> {
        val daysAvailable = ChronoUnit.DAYS.between(today, deadline).toInt()
        if (daysAvailable <= 0 || sessionCount <= 0) return emptyList()

        val count = minOf(sessionCount, daysAvailable)
        return (0 until count).map { i ->
            // Spreads offsets across [0, daysAvailable - 1]: e.g. 10 days, 4 sessions -> 0, 3, 5, 8.
            val offset = daysAvailable - ((count - i) * daysAvailable) / count
            StudySession(
                title = "$title (${i + 1}/$count)",
                date = today.plusDays(offset.toLong()).toString(),
                startTime = startTime,
                durationMinutes = durationMinutes,
                moduleId = moduleId,
                examId = examId,
                taskId = taskId
            )
        }
    }

    /** Convenience wrapper for planning revision for an exam. */
    fun planForExam(exam: Exam, today: LocalDate, sessionCount: Int, durationMinutes: Int, label: String): List<StudySession> {
        val deadline = DateUtils.parse(exam.date) ?: return emptyList()
        return suggestSessions(
            title = "$label: ${exam.title}",
            deadline = deadline,
            today = today,
            sessionCount = sessionCount,
            durationMinutes = durationMinutes,
            moduleId = exam.moduleId,
            examId = exam.id
        )
    }

    /** Percentage (0-100) of sessions completed, for the planner's progress bar. */
    fun completionPercent(sessions: List<StudySession>): Int =
        if (sessions.isEmpty()) 0 else sessions.count { it.completed } * 100 / sessions.size

    /** Total planned minutes for sessions that are not yet completed. */
    fun remainingMinutes(sessions: List<StudySession>): Int =
        sessions.filter { !it.completed }.sumOf { it.durationMinutes }

    /** Average exam mark (academic progress, design section 3.7), or null if no marks recorded. */
    fun averageMark(exams: List<Exam>): Double? {
        val marks = exams.mapNotNull { it.mark }
        return if (marks.isEmpty()) null else marks.average()
    }
}
