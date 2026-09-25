package com.bopape.studentassistant.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/** Small date helpers shared by the Tasks and Timetable screens. Dates are stored as "yyyy-MM-dd". */
object DateUtils {

    private val isoFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val displayFormat = SimpleDateFormat("d MMM yyyy", Locale.getDefault())

    fun todayIso(): String = isoFormat.format(Calendar.getInstance().time)

    /** Converts "2026-08-24" to "24 Aug 2026" for display; falls back to the raw value if unparsable. */
    fun formatIsoDateForDisplay(isoDate: String): String {
        return try {
            val date = isoFormat.parse(isoDate) ?: return isoDate
            displayFormat.format(date)
        } catch (e: Exception) {
            isoDate
        }
    }

    fun isoDateFrom(year: Int, month: Int, dayOfMonth: Int): String {
        val calendar = Calendar.getInstance()
        calendar.set(year, month, dayOfMonth)
        return isoFormat.format(calendar.time)
    }
}
