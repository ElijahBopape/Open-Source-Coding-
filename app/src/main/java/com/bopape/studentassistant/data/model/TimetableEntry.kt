package com.bopape.studentassistant.data.model

/**
 * A recurring weekly class entry. Times are stored as "HH:mm" 24-hour strings so
 * they sort correctly as plain text without needing a date component.
 */
data class TimetableEntry(
    val id: Int = 0,
    val moduleId: Int,
    val dayOfWeek: String,
    val startTime: String,
    val endTime: String,
    val venue: String = ""
)
