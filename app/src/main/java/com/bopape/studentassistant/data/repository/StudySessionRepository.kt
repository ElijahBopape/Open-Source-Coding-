package com.bopape.studentassistant.data.repository

import android.util.Log
import com.bopape.studentassistant.data.model.StudySession
import com.bopape.studentassistant.data.remote.RetrofitClient
import com.bopape.studentassistant.util.ApiResult
import com.bopape.studentassistant.util.safeApiCall

/** Talks to the /api/study-sessions endpoints (User Defined feature 5, the Study Planner). */
class StudySessionRepository {

    private val api = RetrofitClient.apiService
    private val loader = CachedListLoader("studySessions", StudySession::class.java)

    suspend fun getSessions(): ApiResult<List<StudySession>> =
        loader.load("getStudySessions") { api.getStudySessions() }

    suspend fun createSession(session: StudySession): ApiResult<StudySession> =
        safeApiCall("createStudySession") { api.createStudySession(session) }

    suspend fun updateSession(session: StudySession): ApiResult<StudySession> =
        safeApiCall("updateStudySession") { api.updateStudySession(session.id, session) }

    suspend fun deleteSession(id: Int): ApiResult<Unit> =
        safeApiCall("deleteStudySession") { api.deleteStudySession(id) }

    /** Saves a whole generated study plan; stops at the first failure and reports how many were saved. */
    suspend fun createAll(sessions: List<StudySession>): ApiResult<Int> {
        var saved = 0
        for (session in sessions) {
            when (val result = createSession(session)) {
                is ApiResult.Success -> saved++
                is ApiResult.Failure -> {
                    Log.w("StudySessionRepository", "Plan stopped after $saved sessions: ${result.message}")
                    return if (saved == 0) result else ApiResult.Success(saved)
                }
            }
        }
        return ApiResult.Success(saved)
    }
}
