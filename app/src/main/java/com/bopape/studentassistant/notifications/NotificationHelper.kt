package com.bopape.studentassistant.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.bopape.studentassistant.AppGraph
import com.bopape.studentassistant.R
import com.bopape.studentassistant.ui.main.MainActivity
import com.bopape.studentassistant.util.SessionManager

/**
 * Creates the notification channels and shows notifications, for both FCM push
 * messages and the app's own local deadline reminders.
 * Reference: Android Developers (2026) Create and manage notification channels.
 * https://developer.android.com/develop/ui/views/notifications/channels
 */
object NotificationHelper {

    /** Must match android.notification.channelId sent by the backend (notify.js). */
    const val CHANNEL_UPDATES = "student_assistant_updates"
    const val CHANNEL_REMINDERS = "student_assistant_reminders"
    const val EXTRA_OPEN_TAB = "open_tab"

    private const val TAG = "NotificationHelper"

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val localized = AppGraph.localizedContext()
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_UPDATES, localized.getString(R.string.channel_updates_name), NotificationManager.IMPORTANCE_HIGH)
                .apply { description = localized.getString(R.string.channel_updates_description) }
        )
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_REMINDERS, localized.getString(R.string.channel_reminders_name), NotificationManager.IMPORTANCE_DEFAULT)
                .apply { description = localized.getString(R.string.channel_reminders_description) }
        )
    }

    /** Shows a notification unless the user switched notifications off or denied the permission. */
    fun show(context: Context, channel: String, title: String, body: String, openTab: String? = null) {
        if (!SessionManager(context).notificationsEnabled) {
            Log.d(TAG, "Notifications are switched off in Settings - not showing '$title'")
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w(TAG, "POST_NOTIFICATIONS permission not granted - not showing '$title'")
            return
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_OPEN_TAB, openTab)
        }
        val pendingIntent = PendingIntent.getActivity(
            context, title.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(ContextCompat.getColor(context, R.color.primary))
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify((title + body).hashCode(), notification)
        Log.i(TAG, "Notification shown: $title")
    }
}
