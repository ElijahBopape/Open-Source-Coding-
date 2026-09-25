package com.bopape.studentassistant

import android.app.Application
import android.util.Log

/**
 * Application entry point. Firebase initialises itself automatically from
 * google-services.json via its ContentProvider, so this class only exists to
 * confirm startup in the log for debugging.
 */
class StudentAssistantApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Log.i("StudentAssistantApp", "Application started")
    }
}
