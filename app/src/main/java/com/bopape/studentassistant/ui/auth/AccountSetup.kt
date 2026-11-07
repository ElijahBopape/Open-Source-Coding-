package com.bopape.studentassistant.ui.auth

import android.content.Context
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import com.bopape.studentassistant.AppGraph
import com.bopape.studentassistant.data.model.UserProfile
import com.bopape.studentassistant.data.repository.ProfileRepository
import com.bopape.studentassistant.data.sync.SyncScheduler
import com.bopape.studentassistant.notifications.PushRegistrar
import com.bopape.studentassistant.notifications.ReminderWorker
import com.bopape.studentassistant.util.ApiResult
import com.bopape.studentassistant.util.LocaleHelper
import com.bopape.studentassistant.util.SessionManager
import com.google.firebase.auth.FirebaseAuth

/**
 * Work done once after any successful sign-in (email/password or Google):
 *  1. load the user's saved settings from Firestore (via the API) and apply them,
 *     so their language and theme follow them to this device;
 *  2. make sure the profile document holds their name and email;
 *  3. register this phone for push notifications;
 *  4. start the first sync of offline tasks.
 * Failures are logged and ignored - the user can still use the app offline.
 */
object AccountSetup {

    private const val TAG = "AccountSetup"

    suspend fun afterSignIn(context: Context) {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val session = SessionManager(context)
        val repository = ProfileRepository()

        when (val result = repository.getProfile()) {
            is ApiResult.Success -> {
                val profile = result.data
                profile.darkTheme?.let { session.darkThemeEnabled = it }
                profile.notificationsEnabled?.let { session.notificationsEnabled = it }
                profile.language?.let { session.language = it }
                Log.i(TAG, "Applied saved settings from profile: lang=${profile.language}, dark=${profile.darkTheme}")
            }
            is ApiResult.Failure -> Log.w(TAG, "Could not load profile: ${result.message}")
        }

        repository.saveProfile(
            UserProfile(
                displayName = user.displayName.orEmpty(),
                email = user.email.orEmpty(),
                language = session.language,
                darkTheme = session.darkThemeEnabled,
                notificationsEnabled = session.notificationsEnabled,
                photoUrl = user.photoUrl?.toString()?.takeIf { it.startsWith("https://") }
            )
        )

        AppCompatDelegate.setDefaultNightMode(
            if (session.darkThemeEnabled) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        )
        LocaleHelper.apply(session.language)

        PushRegistrar.registerCurrentDevice(context)
        SyncScheduler.requestSync(context)
        SyncScheduler.schedulePeriodic(context)
        ReminderWorker.schedule(context)
        Log.i(TAG, "Account setup finished for ${AppGraph.currentUserId}")
    }
}
