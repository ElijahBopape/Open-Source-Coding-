package com.bopape.studentassistant.util

import android.content.Context
import android.content.SharedPreferences

/**
 * Stores the user's app preferences (settings screen) locally on the device using
 * SharedPreferences. Firebase Authentication is the source of truth for the account
 * itself; this only covers this device's display preferences.
 */
class SessionManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var language: String
        get() = prefs.getString(KEY_LANGUAGE, "English") ?: "English"
        set(value) = prefs.edit().putString(KEY_LANGUAGE, value).apply()

    var notificationsEnabled: Boolean
        get() = prefs.getBoolean(KEY_NOTIFICATIONS, true)
        set(value) = prefs.edit().putBoolean(KEY_NOTIFICATIONS, value).apply()

    var darkThemeEnabled: Boolean
        get() = prefs.getBoolean(KEY_DARK_THEME, false)
        set(value) = prefs.edit().putBoolean(KEY_DARK_THEME, value).apply()

    companion object {
        private const val PREFS_NAME = "student_assistant_prefs"
        private const val KEY_LANGUAGE = "language"
        private const val KEY_NOTIFICATIONS = "notifications_enabled"
        private const val KEY_DARK_THEME = "dark_theme_enabled"
    }
}
