package com.bopape.studentassistant

import android.app.Application
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import com.bopape.studentassistant.data.sync.SyncScheduler
import com.bopape.studentassistant.notifications.NotificationHelper
import com.bopape.studentassistant.notifications.ReminderWorker
import com.bopape.studentassistant.util.SessionManager

/**
 * Application entry point. Firebase initialises itself from google-services.json;
 * this class wires up the app-wide pieces added for the Final PoE:
 *  - the saved light/dark theme is applied before any screen is drawn;
 *  - notification channels for push and reminder notifications;
 *  - offline sync: a sync runs whenever the connection returns, plus a periodic safety net;
 *  - the daily local reminder job.
 */
class StudentAssistantApp : Application() {

    override fun onCreate() {
        super.onCreate()
        AppGraph.init(this)

        val session = SessionManager(this)
        AppCompatDelegate.setDefaultNightMode(
            if (session.darkThemeEnabled) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        )

        NotificationHelper.createChannels(this)

        AppGraph.networkMonitor.onReconnected {
            Log.i(TAG, "Back online - syncing offline changes")
            SyncScheduler.requestSync(this)
        }
        if (AppGraph.currentUserId != null) {
            SyncScheduler.schedulePeriodic(this)
            ReminderWorker.schedule(this)
        }

        Log.i(TAG, "Application started (v${BuildConfig.VERSION_NAME}, API ${BuildConfig.API_BASE_URL})")
    }

    private companion object {
        const val TAG = "StudentAssistantApp"
    }
}
