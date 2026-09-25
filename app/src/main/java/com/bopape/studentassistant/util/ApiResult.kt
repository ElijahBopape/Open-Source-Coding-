package com.bopape.studentassistant.util

/** A simple success/failure wrapper so UI code never has to catch exceptions directly. */
sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Failure(val message: String) : ApiResult<Nothing>()
}
