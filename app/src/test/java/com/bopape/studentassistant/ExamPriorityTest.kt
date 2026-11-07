package com.bopape.studentassistant

import com.bopape.studentassistant.data.model.Exam
import com.bopape.studentassistant.data.model.Priority
import com.bopape.studentassistant.data.model.Task
import com.bopape.studentassistant.util.DateUtils
import com.bopape.studentassistant.util.LocaleHelper
import com.bopape.studentassistant.util.PriorityUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * Tests the Final PoE additions to the Academic Priority System (exam proximity
 * boost), plus the date and language helpers used by the localised UI.
 */
class ExamPriorityTest {

    private val today = LocalDate.of(2026, 11, 7)

    @Test
    fun taskForModuleWithExamThisWeek_isRaisedAboveSamePriorityTask() {
        val exams = listOf(Exam(title = "Exam", moduleId = 1, date = "2026-11-10"))
        val tasks = listOf(
            Task(title = "Other module", moduleId = 2, dueDate = "2026-11-08", priority = Priority.MEDIUM),
            Task(title = "Exam module", moduleId = 1, dueDate = "2026-11-20", priority = Priority.MEDIUM)
        )

        val sorted = PriorityUtils.sortByPriority(tasks, exams, today)

        assertEquals("Exam module", sorted.first().title)
        assertTrue(PriorityUtils.isExamBoosted(sorted.first(), PriorityUtils.modulesWithExamSoon(exams, today)))
    }

    @Test
    fun examMoreThanAWeekAway_givesNoBoost() {
        val exams = listOf(Exam(title = "Exam", moduleId = 1, date = "2026-11-30"))
        assertTrue(PriorityUtils.modulesWithExamSoon(exams, today).isEmpty())
    }

    @Test
    fun pastExam_givesNoBoost() {
        val exams = listOf(Exam(title = "Exam", moduleId = 1, date = "2026-11-01"))
        assertTrue(PriorityUtils.modulesWithExamSoon(exams, today).isEmpty())
    }

    @Test
    fun highPriorityTask_cannotBeBoostedFurther() {
        val task = Task(title = "t", moduleId = 1, dueDate = "2026-11-08", priority = Priority.HIGH)
        assertEquals(0, PriorityUtils.effectiveWeight(task, setOf(1)))
        assertFalse(PriorityUtils.isExamBoosted(task, setOf(1)))
    }

    @Test
    fun dateUtils_formatsWithTranslatedMonthNames() {
        val sepediMonths = arrayOf("Jan", "Feb", "Mat", "Apo", "Mei", "Jun", "Jul", "Ago", "Set", "Okt", "Nof", "Dis")
        assertEquals("7 Nof 2026", DateUtils.formatForDisplay("2026-11-07", sepediMonths))
        assertEquals("not-a-date", DateUtils.formatForDisplay("not-a-date", sepediMonths))
    }

    @Test
    fun dateUtils_daysUntilAndDayName() {
        assertEquals(3L, DateUtils.daysUntil("2026-11-10", today))
        assertEquals(-2L, DateUtils.daysUntil("2026-11-05", today))
        assertEquals("Saturday", DateUtils.todayStoredDayName(today))
    }

    @Test
    fun localeHelper_mapsStoredValuesToSupportedLanguageCodes() {
        assertEquals("nso", LocaleHelper.normalise("Sepedi"))
        assertEquals("zu", LocaleHelper.normalise("isiZulu"))
        assertEquals("zu", LocaleHelper.normalise("zu"))
        assertEquals("en", LocaleHelper.normalise("fr"))
        assertEquals("en", LocaleHelper.normalise(null))
    }
}
