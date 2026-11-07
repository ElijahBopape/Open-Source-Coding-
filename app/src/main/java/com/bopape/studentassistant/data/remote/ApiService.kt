package com.bopape.studentassistant.data.remote

import com.bopape.studentassistant.data.model.DeviceRegistration
import com.bopape.studentassistant.data.model.Exam
import com.bopape.studentassistant.data.model.Module
import com.bopape.studentassistant.data.model.NotificationResult
import com.bopape.studentassistant.data.model.StudySession
import com.bopape.studentassistant.data.model.Task
import com.bopape.studentassistant.data.model.TimetableEntry
import com.bopape.studentassistant.data.model.UserProfile
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

/**
 * Defines the REST API this app was built against (see /backend in this repository
 * for the server implementation, which stores everything in Cloud Firestore).
 * Every request carries the signed-in user's Firebase ID token via
 * [AuthHeaderInterceptor]; the server verifies it and scopes all data to that user.
 *
 * Follows the Retrofit interface pattern described in the official documentation:
 * https://square.github.io/retrofit/ (Square, 2026).
 */
interface ApiService {

    // ---- Modules -------------------------------------------------------------
    @GET("api/modules")
    suspend fun getModules(): Response<List<Module>>

    @POST("api/modules")
    suspend fun createModule(@Body module: Module): Response<Module>

    @PUT("api/modules/{id}")
    suspend fun updateModule(@Path("id") id: Int, @Body module: Module): Response<Module>

    @DELETE("api/modules/{id}")
    suspend fun deleteModule(@Path("id") id: Int): Response<Unit>

    // ---- Tasks (offline-capable, see TaskRepository / TaskSyncEngine) -------
    @GET("api/tasks")
    suspend fun getTasks(): Response<List<Task>>

    @POST("api/tasks")
    suspend fun createTask(@Body task: Task): Response<Task>

    @PUT("api/tasks/{id}")
    suspend fun updateTask(@Path("id") id: Int, @Body task: Task): Response<Task>

    @DELETE("api/tasks/{id}")
    suspend fun deleteTask(@Path("id") id: Int): Response<Unit>

    // ---- Timetable -----------------------------------------------------------
    @GET("api/timetable")
    suspend fun getTimetable(): Response<List<TimetableEntry>>

    @POST("api/timetable")
    suspend fun createTimetableEntry(@Body entry: TimetableEntry): Response<TimetableEntry>

    @PUT("api/timetable/{id}")
    suspend fun updateTimetableEntry(@Path("id") id: Int, @Body entry: TimetableEntry): Response<TimetableEntry>

    @DELETE("api/timetable/{id}")
    suspend fun deleteTimetableEntry(@Path("id") id: Int): Response<Unit>

    // ---- Exams (User Defined feature 4) --------------------------------------
    @GET("api/exams")
    suspend fun getExams(): Response<List<Exam>>

    @POST("api/exams")
    suspend fun createExam(@Body exam: Exam): Response<Exam>

    @PUT("api/exams/{id}")
    suspend fun updateExam(@Path("id") id: Int, @Body exam: Exam): Response<Exam>

    @DELETE("api/exams/{id}")
    suspend fun deleteExam(@Path("id") id: Int): Response<Unit>

    // ---- Study planner (User Defined feature 5) ------------------------------
    @GET("api/study-sessions")
    suspend fun getStudySessions(): Response<List<StudySession>>

    @POST("api/study-sessions")
    suspend fun createStudySession(@Body session: StudySession): Response<StudySession>

    @PUT("api/study-sessions/{id}")
    suspend fun updateStudySession(@Path("id") id: Int, @Body session: StudySession): Response<StudySession>

    @DELETE("api/study-sessions/{id}")
    suspend fun deleteStudySession(@Path("id") id: Int): Response<Unit>

    // ---- Profile / settings, devices and notifications -----------------------
    @GET("api/profile")
    suspend fun getProfile(): Response<UserProfile>

    @PUT("api/profile")
    suspend fun updateProfile(@Body profile: UserProfile): Response<UserProfile>

    @POST("api/devices")
    suspend fun registerDevice(@Body device: DeviceRegistration): Response<Unit>

    @DELETE("api/devices/{token}")
    suspend fun unregisterDevice(@Path("token") token: String): Response<Unit>

    @POST("api/notifications/test")
    suspend fun sendTestNotification(): Response<NotificationResult>

    @POST("api/notifications/reminders")
    suspend fun sendReminders(): Response<NotificationResult>
}
