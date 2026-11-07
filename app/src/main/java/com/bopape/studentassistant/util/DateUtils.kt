package com.bopape.studentassistant.util

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit

/**
 * Date helpers shared by every screen. Dates are stored as ISO "yyyy-MM-dd"
 * strings (sortable as plain text, and what the API expects).
 *
 * Month and day names are not taken from Java's locale data, because it has no
 * Sepedi month names - the caller passes in the translated names from the
 * string resources instead (R.array.months_short / R.array.days_of_week), so
 * dates read naturally in all three languages. Pure Kotlin, so unit-tested.
 */
object DateUtils {

    /** English day names exactly as stored in timetable entries (Monday first). */
    val storedDayNames = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")

    fun today(): LocalDate = LocalDate.now()

    fun todayIso(): String = today().toString()

    fun parse(isoDate: String?): LocalDate? = try {
        if (isoDate.isNullOrBlank()) null else LocalDate.parse(isoDate)
    } catch (e: DateTimeParseException) {
        null
    }

    fun isoDateFrom(year: Int, monthZeroBased: Int, dayOfMonth: Int): String =
        LocalDate.of(year, monthZeroBased + 1, dayOfMonth).toString()

    /** "2026-11-07" -> "7 Nov 2026" using the supplied (translated) short month names. */
    fun formatForDisplay(isoDate: String, monthNames: Array<String>): String {
        val date = parse(isoDate) ?: return isoDate
        return "${date.dayOfMonth} ${monthNames[date.monthValue - 1]} ${date.year}"
    }

    /** Whole days from today until [isoDate]; negative if it is in the past, null if unparsable. */
    fun daysUntil(isoDate: String, today: LocalDate = today()): Long? =
        parse(isoDate)?.let { ChronoUnit.DAYS.between(today, it) }

    /** Index (0 = Monday) of today in [storedDayNames]. */
    fun todayDayIndex(today: LocalDate = today()): Int = today.dayOfWeek.value - DayOfWeek.MONDAY.value

    /** Today's English day name, matching TimetableEntry.dayOfWeek. */
    fun todayStoredDayName(today: LocalDate = today()): String = storedDayNames[todayDayIndex(today)]
}
