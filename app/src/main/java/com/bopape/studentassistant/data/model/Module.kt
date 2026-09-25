package com.bopape.studentassistant.data.model

/**
 * An academic module (course), the foundation entity that tasks and timetable
 * entries are linked to. Mirrors the Module entity from the Part 1 design document.
 */
data class Module(
    val id: Int = 0,
    val name: String,
    val code: String,
    val lecturer: String,
    val venue: String
)
