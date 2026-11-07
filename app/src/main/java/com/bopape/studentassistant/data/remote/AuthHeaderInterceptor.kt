package com.bopape.studentassistant.data.remote

import android.util.Log
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import okhttp3.Interceptor
import okhttp3.Response
import java.util.concurrent.TimeUnit

/**
 * Adds "Authorization: Bearer <Firebase ID token>" to every API request. The
 * server verifies the token's signature with the Firebase Admin SDK, so it knows
 * exactly which user is calling and a user can never act as someone else.
 * The Firebase SDK caches the token and refreshes it about once an hour.
 *
 * OkHttp runs interceptors on a background thread, so blocking on the token
 * Task here is safe. The UID is also sent as X-User-Id for local test servers
 * that run without Firebase credentials.
 *
 * Reference: Google (2026) Verify ID tokens. https://firebase.google.com/docs/auth/admin/verify-id-tokens
 */
class AuthHeaderInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val user = FirebaseAuth.getInstance().currentUser
        val builder = chain.request().newBuilder()

        if (user != null) {
            builder.header("X-User-Id", user.uid)
            try {
                val token = Tasks.await(user.getIdToken(false), 10, TimeUnit.SECONDS).token
                if (token != null) builder.header("Authorization", "Bearer $token")
            } catch (e: Exception) {
                // No token (e.g. offline with an expired token): the request then fails
                // with 401 or a network error, which safeApiCall turns into a friendly message.
                Log.w(TAG, "Could not get Firebase ID token", e)
            }
        }
        return chain.proceed(builder.build())
    }

    private companion object {
        const val TAG = "AuthHeaderInterceptor"
    }
}
