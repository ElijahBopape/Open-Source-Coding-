package com.bopape.studentassistant.notifications

import android.content.Context
import android.util.Log
import com.bopape.studentassistant.AppGraph
import com.bopape.studentassistant.data.model.DeviceRegistration
import com.bopape.studentassistant.data.remote.RetrofitClient
import com.bopape.studentassistant.util.ApiResult
import com.bopape.studentassistant.util.SessionManager
import com.bopape.studentassistant.util.safeApiCall
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Registers this device's FCM token with the REST API so the server can push
 * notifications to it, and removes it again on sign-out.
 */
object PushRegistrar {

    private const val TAG = "PushRegistrar"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Fetches the current FCM token and registers it (called after every sign-in). */
    fun registerCurrentDevice(context: Context) {
        scope.launch {
            try {
                val token = FirebaseMessaging.getInstance().token.await()
                register(context, token)
            } catch (e: Exception) {
                Log.w(TAG, "Could not get FCM token (is Google Play services available?)", e)
            }
        }
    }

    fun registerToken(context: Context, token: String) {
        scope.launch { register(context, token) }
    }

    private suspend fun register(context: Context, token: String) {
        if (AppGraph.currentUserId == null) return
        when (val result = safeApiCall("registerDevice") { RetrofitClient.apiService.registerDevice(DeviceRegistration(token)) }) {
            is ApiResult.Success -> {
                SessionManager(context).fcmToken = token
                Log.i(TAG, "Device registered for push notifications")
            }
            is ApiResult.Failure -> Log.w(TAG, "Device registration failed: ${result.message}")
        }
    }

    /** Called before sign-out (while the user's ID token is still valid). */
    suspend fun unregister(context: Context) {
        val token = SessionManager(context).fcmToken ?: return
        safeApiCall("unregisterDevice") { RetrofitClient.apiService.unregisterDevice(token) }
        Log.i(TAG, "Device unregistered from push notifications")
    }
}
