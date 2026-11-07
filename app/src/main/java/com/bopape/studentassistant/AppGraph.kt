package com.bopape.studentassistant

import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate
import com.bopape.studentassistant.data.local.AppDatabase
import com.bopape.studentassistant.util.NetworkMonitor
import com.google.firebase.auth.FirebaseAuth

/**
 * A tiny service locator: holds the application context and the shared
 * singletons (Room database, network monitor) so repositories can be created
 * with a plain constructor from any screen. Initialised in [StudentAssistantApp].
 */
object AppGraph {

    lateinit var context: Context
        private set

    val database: AppDatabase by lazy { AppDatabase.get(context) }
    val networkMonitor: NetworkMonitor by lazy { NetworkMonitor(context) }

    /** The signed-in Firebase user's UID, or null when signed out. */
    val currentUserId: String?
        get() = FirebaseAuth.getInstance().currentUser?.uid

    fun init(appContext: Context) {
        context = appContext.applicationContext
    }

    /**
     * The application context does not follow the per-app language chosen in
     * Settings, so code outside an Activity (repositories, workers, notifications)
     * uses this to load strings in the user's chosen language.
     */
    fun localizedContext(): Context {
        val locales = AppCompatDelegate.getApplicationLocales()
        if (locales.isEmpty) return context
        val config = Configuration(context.resources.configuration)
        config.setLocale(locales[0])
        return context.createConfigurationContext(config)
    }
}
