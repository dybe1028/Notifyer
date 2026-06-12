package com.dybe.notifyer

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

/**
 * Applies the chosen accent overlay before the content is inflated, so every screen
 * picks up the user's accent colour. Night mode is applied globally in [NotifyerApp].
 */
abstract class BaseActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        theme.applyStyle(ThemePrefs.accentOverlayRes(this), true)
        super.onCreate(savedInstanceState)
    }
}
