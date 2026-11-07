package com.bopape.studentassistant.notifications

import android.util.Log
import com.bopape.studentassistant.data.sync.SyncScheduler
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * Receives real-time push notifications from Firebase Cloud Messaging.
 *
 * - onNewToken: FCM issued this device a new registration token, so it is sent
 *   to the REST API (POST /api/devices) - that is how the server knows where to
 *   push this user's alerts.
 * - onMessageReceived: called for messages that arrive while the app is open
 *   (when the app is in the background, Android shows the notification itself).
 *   Server data changed, so a sync is also started to refresh local data.
 *
 * Reference: Google (2026) Receive messages in an Android app.
 * https://firebase.google.com/docs/cloud-messaging/android/receive
 */
class StudentMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        Log.i(TAG, "New FCM token issued")
        PushRegistrar.registerToken(applicationContext, token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val title = message.notification?.title ?: message.data["title"] ?: return
        val body = message.notification?.body ?: message.data["body"].orEmpty()
        val type = message.data["type"]
        Log.i(TAG, "Push received (type=$type): $title")

        NotificationHelper.show(applicationContext, NotificationHelper.CHANNEL_UPDATES, title, body, openTab = type)

        if (type == "task" || type == "exam") SyncScheduler.requestSync(applicationContext)
    }

    private companion object {
        const val TAG = "StudentMessaging"
    }
}
