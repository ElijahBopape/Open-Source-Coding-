package com.bopape.studentassistant.data.remote

import com.google.firebase.auth.FirebaseAuth
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Attaches the signed-in user's Firebase UID to every outgoing API request so the
 * backend can scope modules, tasks and timetable entries to that user without a
 * separate login step on the server itself.
 */
class AuthHeaderInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: "anonymous"
        val request = chain.request().newBuilder()
            .addHeader("X-User-Id", uid)
            .build()
        return chain.proceed(request)
    }
}
