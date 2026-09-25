package com.bopape.studentassistant.data.repository

import com.bopape.studentassistant.data.model.Module
import com.bopape.studentassistant.data.remote.RetrofitClient
import com.bopape.studentassistant.util.ApiResult
import com.bopape.studentassistant.util.safeApiCall

/** Talks to the /api/modules endpoints of the custom REST API. */
class ModuleRepository {

    private val api = RetrofitClient.apiService

    suspend fun getModules(): ApiResult<List<Module>> =
        safeApiCall("getModules") { api.getModules() }

    suspend fun createModule(module: Module): ApiResult<Module> =
        safeApiCall("createModule") { api.createModule(module) }

    suspend fun updateModule(module: Module): ApiResult<Module> =
        safeApiCall("updateModule") { api.updateModule(module.id, module) }

    suspend fun deleteModule(id: Int): ApiResult<Unit> =
        safeApiCall("deleteModule") { api.deleteModule(id) }
}
