package com.bopape.studentassistant.data.repository

import android.util.Log
import com.bopape.studentassistant.AppGraph
import com.bopape.studentassistant.data.local.CacheEntity
import com.bopape.studentassistant.util.ApiResult
import com.bopape.studentassistant.util.safeApiCall
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import retrofit2.Response

/**
 * Loads a list from the API and keeps the latest copy in Room. If the API cannot
 * be reached the cached copy is returned instead (flagged fromCache = true), so
 * modules, the timetable, exams and study sessions stay readable offline.
 */
class CachedListLoader<T>(private val collection: String, itemClass: Class<T>) {

    private val gson = Gson()
    private val listType = TypeToken.getParameterized(List::class.java, itemClass).type
    private val cacheDao get() = AppGraph.database.cacheDao()

    suspend fun load(label: String, call: suspend () -> Response<List<T>>): ApiResult<List<T>> {
        val userId = AppGraph.currentUserId ?: return ApiResult.Success(emptyList())
        val result = safeApiCall(label, call)

        if (result is ApiResult.Success) {
            cacheDao.put(CacheEntity(userId, collection, gson.toJson(result.data)))
            return result
        }

        val cached = cacheDao.get(userId, collection) ?: return result
        Log.i(TAG, "$label: API unavailable, showing cached $collection")
        val items: List<T> = gson.fromJson(cached.json, listType)
        return ApiResult.Success(items, fromCache = true)
    }

    private companion object {
        const val TAG = "CachedListLoader"
    }
}
