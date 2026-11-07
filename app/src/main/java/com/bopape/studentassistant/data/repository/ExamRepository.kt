package com.bopape.studentassistant.data.repository

import com.bopape.studentassistant.data.model.Exam
import com.bopape.studentassistant.data.remote.RetrofitClient
import com.bopape.studentassistant.util.ApiResult
import com.bopape.studentassistant.util.safeApiCall

/** Talks to the /api/exams endpoints (User Defined feature 4); cached in Room for offline viewing. */
class ExamRepository {

    private val api = RetrofitClient.apiService
    private val loader = CachedListLoader("exams", Exam::class.java)

    suspend fun getExams(): ApiResult<List<Exam>> =
        loader.load("getExams") { api.getExams() }

    suspend fun createExam(exam: Exam): ApiResult<Exam> =
        safeApiCall("createExam") { api.createExam(exam) }

    suspend fun updateExam(exam: Exam): ApiResult<Exam> =
        safeApiCall("updateExam") { api.updateExam(exam.id, exam) }

    suspend fun deleteExam(id: Int): ApiResult<Unit> =
        safeApiCall("deleteExam") { api.deleteExam(id) }
}
