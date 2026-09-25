package com.bopape.studentassistant.data.repository

import com.bopape.studentassistant.data.model.TimetableEntry
import com.bopape.studentassistant.data.remote.RetrofitClient
import com.bopape.studentassistant.util.ApiResult
import com.bopape.studentassistant.util.safeApiCall

/** Talks to the /api/timetable endpoints of the custom REST API. */
class TimetableRepository {

    private val api = RetrofitClient.apiService

    suspend fun getTimetable(): ApiResult<List<TimetableEntry>> =
        safeApiCall("getTimetable") { api.getTimetable() }

    suspend fun createEntry(entry: TimetableEntry): ApiResult<TimetableEntry> =
        safeApiCall("createTimetableEntry") { api.createTimetableEntry(entry) }

    suspend fun updateEntry(entry: TimetableEntry): ApiResult<TimetableEntry> =
        safeApiCall("updateTimetableEntry") { api.updateTimetableEntry(entry.id, entry) }

    suspend fun deleteEntry(id: Int): ApiResult<Unit> =
        safeApiCall("deleteTimetableEntry") { api.deleteTimetableEntry(id) }
}
