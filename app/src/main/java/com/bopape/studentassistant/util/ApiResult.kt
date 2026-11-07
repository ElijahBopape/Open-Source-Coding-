package com.bopape.studentassistant.util

/**
 * A simple success/failure wrapper so UI code never has to catch exceptions directly.
 * [Success.fromCache] is true when the data came from the offline Room cache
 * because the API could not be reached.
 */
sealed class ApiResult<out T> {
    data class Success<T>(val data: T, val fromCache: Boolean = false) : ApiResult<T>()
    data class Failure(val message: String, val isNetworkError: Boolean = false) : ApiResult<Nothing>()
}
