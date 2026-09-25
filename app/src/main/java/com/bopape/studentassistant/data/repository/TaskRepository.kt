package com.bopape.studentassistant.data.repository

import com.bopape.studentassistant.data.model.Task
import com.bopape.studentassistant.data.remote.RetrofitClient
import com.bopape.studentassistant.util.ApiResult
import com.bopape.studentassistant.util.safeApiCall

/** Talks to the /api/tasks endpoints of the custom REST API. */
class TaskRepository {

    private val api = RetrofitClient.apiService

    suspend fun getTasks(): ApiResult<List<Task>> =
        safeApiCall("getTasks") { api.getTasks() }

    suspend fun createTask(task: Task): ApiResult<Task> =
        safeApiCall("createTask") { api.createTask(task) }

    suspend fun updateTask(task: Task): ApiResult<Task> =
        safeApiCall("updateTask") { api.updateTask(task.id, task) }

    suspend fun deleteTask(id: Int): ApiResult<Unit> =
        safeApiCall("deleteTask") { api.deleteTask(id) }
}
