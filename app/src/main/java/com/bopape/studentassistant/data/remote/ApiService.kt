package com.bopape.studentassistant.data.remote

import com.bopape.studentassistant.data.model.Module
import com.bopape.studentassistant.data.model.Task
import com.bopape.studentassistant.data.model.TimetableEntry
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * Defines the REST API this app was built against (see /backend in this repository
 * for the server implementation). Every request carries the caller's Firebase UID in
 * the "X-User-Id" header via [AuthHeaderInterceptor], so each user only ever sees
 * their own data on the server.
 *
 * Follows the Retrofit interface pattern described in the official documentation:
 * https://square.github.io/retrofit/ (Square, 2026).
 */
interface ApiService {

    @GET("api/modules")
    suspend fun getModules(): Response<List<Module>>

    @POST("api/modules")
    suspend fun createModule(@Body module: Module): Response<Module>

    @PUT("api/modules/{id}")
    suspend fun updateModule(@Path("id") id: Int, @Body module: Module): Response<Module>

    @DELETE("api/modules/{id}")
    suspend fun deleteModule(@Path("id") id: Int): Response<Unit>

    @GET("api/tasks")
    suspend fun getTasks(): Response<List<Task>>

    @POST("api/tasks")
    suspend fun createTask(@Body task: Task): Response<Task>

    @PUT("api/tasks/{id}")
    suspend fun updateTask(@Path("id") id: Int, @Body task: Task): Response<Task>

    @DELETE("api/tasks/{id}")
    suspend fun deleteTask(@Path("id") id: Int): Response<Unit>

    @GET("api/timetable")
    suspend fun getTimetable(): Response<List<TimetableEntry>>

    @POST("api/timetable")
    suspend fun createTimetableEntry(@Body entry: TimetableEntry): Response<TimetableEntry>

    @PUT("api/timetable/{id}")
    suspend fun updateTimetableEntry(@Path("id") id: Int, @Body entry: TimetableEntry): Response<TimetableEntry>

    @DELETE("api/timetable/{id}")
    suspend fun deleteTimetableEntry(@Path("id") id: Int): Response<Unit>
}
