package com.bopape.studentassistant.data.model

/**
 * An examination linked to a module (Part 1 design, section 3.6). [mark] is the
 * percentage obtained once the exam has been written; the dashboard averages the
 * recorded marks to show academic progress (section 3.7).
 */
data class Exam(
    val id: Int = 0,
    val title: String,
    val moduleId: Int? = null,
    val date: String,
    val time: String = "",
    val venue: String = "",
    val durationMinutes: Int = 120,
    val mark: Double? = null
)
