package com.dybe.notifyer

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

/**
 * Persists and resolves the two theme choices: night mode (Light/Dark/System) and
 * the accent palette. Both live in the same SharedPreferences file as reminders.
 */
object ThemePrefs {

    private const val KEY_MODE = "theme_mode"
    private const val KEY_ACCENT = "theme_accent"

    const val ACCENT_BLUE = 0
    const val ACCENT_GREEN = 1
    const val ACCENT_PURPLE = 2
    const val ACCENT_ORANGE = 3

    private fun prefs(context: Context) =
        context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)

    fun mode(context: Context): Int =
        prefs(context).getInt(KEY_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)

    fun setMode(context: Context, mode: Int) =
        prefs(context).edit().putInt(KEY_MODE, mode).apply()

    fun accent(context: Context): Int =
        prefs(context).getInt(KEY_ACCENT, ACCENT_BLUE)

    fun setAccent(context: Context, accent: Int) =
        prefs(context).edit().putInt(KEY_ACCENT, accent).apply()

    fun accentOverlayRes(context: Context): Int = when (accent(context)) {
        ACCENT_GREEN -> R.style.ThemeOverlay_Notifyer_Green
        ACCENT_PURPLE -> R.style.ThemeOverlay_Notifyer_Purple
        ACCENT_ORANGE -> R.style.ThemeOverlay_Notifyer_Orange
        else -> R.style.ThemeOverlay_Notifyer_Blue
    }
}
