package com.bopape.studentassistant.data.repository

import com.bopape.studentassistant.data.model.TimetableEntry
import com.bopape.studentassistant.data.remote.RetrofitClient
import com.bopape.studentassistant.util.ApiResult
import com.bopape.studentassistant.util.safeApiCall

/** Talks to the /api/timetable endpoints; the list is cached in Room for offline viewing. */
class TimetableRepository {

    private val api = RetrofitClient.apiService
    private val loader = CachedListLoader("timetable", TimetableEntry::class.java)

    suspend fun getTimetable(): ApiResult<List<TimetableEntry>> =
        loader.load("getTimetable") { api.getTimetable() }

    suspend fun createEntry(entry: TimetableEntry): ApiResult<TimetableEntry> =
        safeApiCall("createTimetableEntry") { api.createTimetableEntry(entry) }

    suspend fun updateEntry(entry: TimetableEntry): ApiResult<TimetableEntry> =
        safeApiCall("updateTimetableEntry") { api.updateTimetableEntry(entry.id, entry) }

    suspend fun deleteEntry(id: Int): ApiResult<Unit> =
        safeApiCall("deleteTimetableEntry") { api.deleteTimetableEntry(id) }
}
