package com.bopape.studentassistant.util

import android.content.Context
import android.content.SharedPreferences

/**
 * Stores this device's copy of the user's preferences using SharedPreferences, so
 * they apply instantly at start-up even before the network is available. The
 * same values are also saved to the user's profile in Cloud Firestore through
 * the REST API (see ProfileRepository) so they follow the user to other devices.
 */
class SessionManager(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** Language code: "en", "nso" or "zu". */
    var language: String
        get() = LocaleHelper.normalise(prefs.getString(KEY_LANGUAGE, LocaleHelper.ENGLISH))
        set(value) = prefs.edit().putString(KEY_LANGUAGE, LocaleHelper.normalise(value)).apply()

    var notificationsEnabled: Boolean
        get() = prefs.getBoolean(KEY_NOTIFICATIONS, true)
        set(value) = prefs.edit().putBoolean(KEY_NOTIFICATIONS, value).apply()

    var darkThemeEnabled: Boolean
        get() = prefs.getBoolean(KEY_DARK_THEME, false)
        set(value) = prefs.edit().putBoolean(KEY_DARK_THEME, value).apply()

    /** When the last successful sync finished (0 = never). */
    var lastSyncMillis: Long
        get() = prefs.getLong(KEY_LAST_SYNC, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_SYNC, value).apply()

    /** The FCM token last registered with the API, so it can be removed on sign-out. */
    var fcmToken: String?
        get() = prefs.getString(KEY_FCM_TOKEN, null)
        set(value) = prefs.edit().putString(KEY_FCM_TOKEN, value).apply()

    /** Clears per-account values on sign-out (device-level display preferences are kept). */
    fun clearAccountData() {
        prefs.edit().remove(KEY_LAST_SYNC).remove(KEY_FCM_TOKEN).apply()
    }

    companion object {
        private const val PREFS_NAME = "student_assistant_prefs"
        private const val KEY_LANGUAGE = "language"
        private const val KEY_NOTIFICATIONS = "notifications_enabled"
        private const val KEY_DARK_THEME = "dark_theme_enabled"
        private const val KEY_LAST_SYNC = "last_sync_millis"
        private const val KEY_FCM_TOKEN = "fcm_token"
    }
}
