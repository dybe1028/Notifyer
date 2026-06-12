package com.dybe.notifyer

import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/** The Light/Dark/System + accent chooser, usable from any activity. */
object ThemeDialog {

    fun show(activity: AppCompatActivity) {
        val view = activity.layoutInflater.inflate(R.layout.dialog_theme, null)
        val modeGroup = view.findViewById<MaterialButtonToggleGroup>(R.id.modeGroup)

        val startMode = ThemePrefs.mode(activity)
        val startAccent = ThemePrefs.accent(activity)

        modeGroup.check(
            when (startMode) {
                AppCompatDelegate.MODE_NIGHT_NO -> R.id.modeLight
                AppCompatDelegate.MODE_NIGHT_YES -> R.id.modeDark
                else -> R.id.modeSystem
            }
        )
        modeGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            ThemePrefs.setMode(
                activity,
                when (checkedId) {
                    R.id.modeLight -> AppCompatDelegate.MODE_NIGHT_NO
                    R.id.modeDark -> AppCompatDelegate.MODE_NIGHT_YES
                    else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                }
            )
        }

        val swatches = listOf(
            R.id.swatchBlue to ThemePrefs.ACCENT_BLUE,
            R.id.swatchGreen to ThemePrefs.ACCENT_GREEN,
            R.id.swatchPurple to ThemePrefs.ACCENT_PURPLE,
            R.id.swatchOrange to ThemePrefs.ACCENT_ORANGE
        )
        fun renderSelection(selected: Int) {
            swatches.forEach { (viewId, accent) ->
                view.findViewById<ImageView>(viewId)
                    .setImageResource(if (accent == selected) R.drawable.ic_check else 0)
            }
        }
        renderSelection(startAccent)
        swatches.forEach { (viewId, accent) ->
            view.findViewById<ImageView>(viewId).setOnClickListener {
                ThemePrefs.setAccent(activity, accent)
                renderSelection(accent)
            }
        }

        MaterialAlertDialogBuilder(activity)
            .setTitle(R.string.theme_title)
            .setView(view)
            .setPositiveButton(R.string.action_done, null)
            .setOnDismissListener {
                if (ThemePrefs.mode(activity) != startMode) {
                    AppCompatDelegate.setDefaultNightMode(ThemePrefs.mode(activity))
                } else if (ThemePrefs.accent(activity) != startAccent) {
                    activity.recreate()
                }
            }
            .show()
    }
}
