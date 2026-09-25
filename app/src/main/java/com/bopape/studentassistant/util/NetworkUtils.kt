package com.bopape.studentassistant.util

import android.util.Log
import retrofit2.Response
import java.io.IOException

private const val TAG = "NetworkUtils"

/**
 * Runs a Retrofit call and converts it into an [ApiResult], so repositories never
 * let a network exception or an HTTP error response crash the calling screen.
 * Shared by ModuleRepository, TaskRepository and TimetableRepository.
 */
suspend fun <T> safeApiCall(
    logLabel: String,
    call: suspend () -> Response<T>
): ApiResult<T> {
    return try {
        val response = call()
        Log.d(TAG, "$logLabel -> HTTP ${response.code()}")
        if (response.isSuccessful) {
            @Suppress("UNCHECKED_CAST")
            ApiResult.Success(response.body() ?: (Unit as T))
        } else {
            ApiResult.Failure("Server error (${response.code()})")
        }
    } catch (e: IOException) {
        Log.e(TAG, "$logLabel failed: network error", e)
        ApiResult.Failure("Could not reach the server. Check your connection.")
    } catch (e: Exception) {
        Log.e(TAG, "$logLabel failed: unexpected error", e)
        ApiResult.Failure(e.message ?: "Something went wrong")
    }
}
