package com.bopape.studentassistant

import com.bopape.studentassistant.data.model.Exam
import com.bopape.studentassistant.data.model.StudySession
import com.bopape.studentassistant.util.StudyPlanner
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** Tests the Study Planner (User Defined feature 5) scheduling and progress rules. */
class StudyPlannerTest {

    private val today = LocalDate.of(2026, 11, 7)

    @Test
    fun suggestSessions_spreadsSessionsEvenlyBeforeTheDeadline() {
        val sessions = StudyPlanner.suggestSessions("Revise", deadline = today.plusDays(10), today = today, sessionCount = 4)

        assertEquals(listOf("2026-11-07", "2026-11-10", "2026-11-12", "2026-11-15"), sessions.map { it.date })
        assertTrue("Every session must be before the deadline", sessions.all { LocalDate.parse(it.date) < today.plusDays(10) })
        assertEquals("Revise (1/4)", sessions.first().title)
    }

    @Test
    fun suggestSessions_neverSchedulesTwoSessionsOnTheSameDay() {
        val sessions = StudyPlanner.suggestSessions("Revise", deadline = today.plusDays(2), today = today, sessionCount = 6)

        assertEquals(2, sessions.size)
        assertEquals(sessions.size, sessions.map { it.date }.toSet().size)
    }

    @Test
    fun suggestSessions_returnsNothingWhenTheDeadlineHasPassed() {
        assertTrue(StudyPlanner.suggestSessions("Late", deadline = today, today = today).isEmpty())
        assertTrue(StudyPlanner.suggestSessions("Late", deadline = today.minusDays(1), today = today).isEmpty())
    }

    @Test
    fun planForExam_linksSessionsToTheExamAndItsModule() {
        val exam = Exam(id = 5, title = "OPSC6312", moduleId = 2, date = "2026-11-14")
        val sessions = StudyPlanner.planForExam(exam, today, sessionCount = 3, durationMinutes = 45, label = "Revise")

        assertEquals(3, sessions.size)
        assertTrue(sessions.all { it.examId == 5 && it.moduleId == 2 && it.durationMinutes == 45 })
    }

    @Test
    fun completionPercent_andRemainingMinutes() {
        val sessions = listOf(
            StudySession(title = "a", date = "2026-11-07", durationMinutes = 60, completed = true),
            StudySession(title = "b", date = "2026-11-08", durationMinutes = 30),
            StudySession(title = "c", date = "2026-11-09", durationMinutes = 45),
            StudySession(title = "d", date = "2026-11-10", durationMinutes = 60, completed = true)
        )
        assertEquals(50, StudyPlanner.completionPercent(sessions))
        assertEquals(75, StudyPlanner.remainingMinutes(sessions))
        assertEquals(0, StudyPlanner.completionPercent(emptyList()))
    }

    @Test
    fun averageMark_ignoresExamsWithoutMarks() {
        val exams = listOf(
            Exam(title = "A", date = "2026-10-01", mark = 70.0),
            Exam(title = "B", date = "2026-10-02", mark = 80.0),
            Exam(title = "C", date = "2026-12-01")
        )
        assertEquals(75.0, StudyPlanner.averageMark(exams)!!, 0.001)
        assertNull(StudyPlanner.averageMark(listOf(Exam(title = "C", date = "2026-12-01"))))
    }
}
