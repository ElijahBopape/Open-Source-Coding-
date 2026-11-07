package com.bopape.studentassistant.util

import android.util.Log
import com.bopape.studentassistant.AppGraph
import com.bopape.studentassistant.R
import retrofit2.Response
import java.io.IOException

private const val TAG = "NetworkUtils"

/**
 * Runs a Retrofit call and converts it into an [ApiResult], so repositories never
 * let a network exception or an HTTP error response crash the calling screen.
 * Error messages come from string resources so they appear in the user's language.
 */
suspend fun <T> safeApiCall(
    logLabel: String,
    call: suspend () -> Response<T>
): ApiResult<T> {
    val context = AppGraph.localizedContext()
    return try {
        val response = call()
        Log.d(TAG, "$logLabel -> HTTP ${response.code()}")
        if (response.isSuccessful) {
            @Suppress("UNCHECKED_CAST")
            ApiResult.Success(response.body() ?: (Unit as T))
        } else {
            val message = when (response.code()) {
                400 -> context.getString(com.bopape.studentassistant.R.string.error_invalid_input)
                401 -> context.getString(R.string.error_session_expired)
                404 -> context.getString(R.string.error_not_found)
                else -> context.getString(R.string.error_server, response.code())
            }
            ApiResult.Failure(message)
        }
    } catch (e: IOException) {
        Log.e(TAG, "$logLabel failed: network error", e)
        ApiResult.Failure(context.getString(R.string.error_network), isNetworkError = true)
    } catch (e: Exception) {
        Log.e(TAG, "$logLabel failed: unexpected error", e)
        ApiResult.Failure(context.getString(R.string.error_generic))
    }
}
