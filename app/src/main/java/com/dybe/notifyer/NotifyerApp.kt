package com.dybe.notifyer

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate

/** Applies the saved night mode at process start, before any activity is shown. */
class NotifyerApp : Application() {

    override fun onCreate() {
        super.onCreate()
        AppCompatDelegate.setDefaultNightMode(ThemePrefs.mode(this))
    }
}
