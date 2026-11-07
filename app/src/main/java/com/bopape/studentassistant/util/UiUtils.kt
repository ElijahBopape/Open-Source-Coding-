package com.bopape.studentassistant.util

import android.content.Context
import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.bopape.studentassistant.R
import com.bopape.studentassistant.data.model.Priority

/**
 * Apps targeting Android 15+ are drawn edge-to-edge (behind the status and
 * navigation bars). This pads [view] by the system-bar insets so no content is
 * hidden behind them, and consumes the insets so children are not padded twice.
 * Reference: Android Developers (2026) Display content edge-to-edge in views.
 * https://developer.android.com/develop/ui/views/layout/edge-to-edge
 */
fun View.applySystemBarInsets() {
    val start = paddingLeft
    val top = paddingTop
    val end = paddingRight
    val bottom = paddingBottom
    ViewCompat.setOnApplyWindowInsetsListener(this) { v, insets ->
        val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime())
        v.updatePadding(left = start + bars.left, top = top + bars.top, right = end + bars.right, bottom = bottom + bars.bottom)
        WindowInsetsCompat.CONSUMED
    }
}

/** Translated label for a stored priority value ("High"/"Medium"/"Low"). */
fun Context.priorityLabel(priority: String): String = getString(
    when (priority) {
        Priority.HIGH -> R.string.priority_high
        Priority.LOW -> R.string.priority_low
        else -> R.string.priority_medium
    }
)

/** Translated day name for a stored English day name ("Monday" ... "Sunday"). */
fun Context.dayLabel(storedDay: String): String {
    val index = DateUtils.storedDayNames.indexOf(storedDay)
    val names = resources.getStringArray(R.array.days_of_week)
    return if (index >= 0) names[index] else storedDay
}

/** ISO date formatted with translated month names, e.g. "7 Nov 2026" / "7 Nof 2026". */
fun Context.displayDate(isoDate: String): String =
    DateUtils.formatForDisplay(isoDate, resources.getStringArray(R.array.months_short))

/** "Today", "Tomorrow", "In 5 days", "3 days ago" - translated. */
fun Context.relativeDays(isoDate: String): String {
    val days = DateUtils.daysUntil(isoDate) ?: return ""
    return when {
        days == 0L -> getString(R.string.relative_today)
        days == 1L -> getString(R.string.relative_tomorrow)
        days > 1L -> getString(R.string.relative_in_days, days.toInt())
        else -> getString(R.string.relative_days_ago, (-days).toInt())
    }
}
