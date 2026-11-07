package com.bopape.studentassistant.data.model

/**
 * The user's profile and settings as stored in Cloud Firestore (users/{uid}) via
 * the REST API, so preferences follow the student to any device they sign in on.
 * [language] is a language code: "en", "nso" (Sepedi) or "zu" (isiZulu).
 * [photoUrl] points at the profile photo in Cloudinary blob storage.
 */
data class UserProfile(
    val displayName: String? = null,
    val email: String? = null,
    val language: String? = null,
    val darkTheme: Boolean? = null,
    val notificationsEnabled: Boolean? = null,
    val photoUrl: String? = null
)

/** Body for POST /api/devices - registers this phone's FCM token for push notifications. */
data class DeviceRegistration(
    val token: String,
    val platform: String = "android"
)

/** Response of the notification test/reminder endpoints. */
data class NotificationResult(
    val sent: Int = 0,
    val failed: Int = 0,
    val reminders: Int = 0
)
