package com.bopape.studentassistant.data.model

/**
 * One block of planned study (Study Planner, Part 1 design sections 3.8 and 3.12).
 * A session can be linked to an exam ([examId]) or a task ([taskId], the task's
 * server id) so large pieces of work are broken into smaller, scheduled steps.
 */
data class StudySession(
    val id: Int = 0,
    val title: String,
    val date: String,
    val startTime: String = "",
    val durationMinutes: Int = 60,
    val moduleId: Int? = null,
    val taskId: Int? = null,
    val examId: Int? = null,
    val completed: Boolean = false
)
