package com.bopape.studentassistant.util

import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

/**
 * Multi-language support (Final PoE): English plus two South African languages,
 * Sepedi (Northern Sotho, ISO 639 code "nso") and isiZulu ("zu").
 *
 * Every visible string lives in res/values (English), res/values-b+nso (Sepedi)
 * and res/values-zu (isiZulu). Choosing a language in Settings calls
 * AppCompatDelegate.setApplicationLocales(), which switches the whole app
 * immediately and remembers the choice across restarts - on Android 13+ it also
 * appears under the system "App languages" setting.
 *
 * Reference: Android Developers (2026) Per-app language preferences.
 * https://developer.android.com/guide/topics/resources/app-languages
 */
object LocaleHelper {

    const val ENGLISH = "en"
    const val SEPEDI = "nso"
    const val ISIZULU = "zu"

    /** Language codes in the same order as R.array.language_options. */
    val supportedCodes = listOf(ENGLISH, SEPEDI, ISIZULU)

    /** The language currently applied to the app ("en" if none was chosen). */
    fun currentCode(): String {
        val locales = AppCompatDelegate.getApplicationLocales()
        val tag = if (locales.isEmpty) ENGLISH else locales[0]?.language ?: ENGLISH
        return normalise(tag)
    }

    /** Switches the app's language. Activities are recreated by AppCompat automatically. */
    fun apply(code: String) {
        val normalised = normalise(code)
        if (normalised == currentCode() && !AppCompatDelegate.getApplicationLocales().isEmpty) return
        Log.i("LocaleHelper", "Switching app language to $normalised")
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(normalised))
    }

    /** Maps older stored values (Part 2 saved display names) and unknown tags to a supported code. */
    fun normalise(value: String?): String = when (value?.lowercase()) {
        "nso", "sepedi", "northern sotho" -> SEPEDI
        "zu", "isizulu", "zulu" -> ISIZULU
        else -> ENGLISH
    }
}
