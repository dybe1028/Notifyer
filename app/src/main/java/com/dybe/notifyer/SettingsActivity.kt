package com.dybe.notifyer

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class SettingsActivity : BaseActivity() {

    private lateinit var repository: ReminderRepository
    private var versionTaps = 0

    private val exportLauncher =
        registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
            uri?.let { writeExport(it) }
        }

    private val importLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let { readImport(it) }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        applySystemBarInsets()

        repository = ReminderRepository(this)

        findViewById<TextView>(R.id.versionValue).text =
            getString(R.string.version_format, BuildConfig.VERSION_NAME)
        updateLanguageValue()

        findViewById<View>(R.id.rowTheme).setOnClickListener { ThemeDialog.show(this) }
        findViewById<View>(R.id.rowLanguage).setOnClickListener { showLanguageDialog() }
        findViewById<View>(R.id.rowSound).setOnClickListener { openNotificationSettings() }
        findViewById<View>(R.id.rowExport).setOnClickListener {
            exportLauncher.launch("notifyer-reminders.json")
        }
        findViewById<View>(R.id.rowImport).setOnClickListener {
            importLauncher.launch(arrayOf("application/json"))
        }
        findViewById<View>(R.id.rowClear).setOnClickListener { confirmClearAll() }
        findViewById<View>(R.id.rowGithub).setOnClickListener { openUrl(GITHUB_URL) }
        findViewById<View>(R.id.rowVersion).setOnClickListener { onVersionTapped() }
    }

    /** Easter egg: spam-tapping the version row eventually nags you to be patient. */
    private fun onVersionTapped() {
        versionTaps++
        if (versionTaps >= VERSION_TAP_THRESHOLD) {
            versionTaps = 0
            toast(getString(R.string.easter_dev))
        }
    }

    override fun onResume() {
        super.onResume()
        updateLanguageValue()
    }

    // region Appearance -------------------------------------------------------

    private fun updateLanguageValue() {
        val locales = AppCompatDelegate.getApplicationLocales()
        val language = if (locales.isEmpty) "" else locales[0]?.language.orEmpty()
        findViewById<TextView>(R.id.langValue).text = when (language) {
            "vi" -> getString(R.string.lang_vi)
            "en" -> getString(R.string.lang_en)
            else -> getString(R.string.lang_system)
        }
    }

    private fun showLanguageDialog() {
        val options = arrayOf(
            getString(R.string.lang_system),
            getString(R.string.lang_en),
            getString(R.string.lang_vi)
        )
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.setting_language)
            .setItems(options) { _, which ->
                val locales = when (which) {
                    1 -> LocaleListCompat.forLanguageTags("en")
                    2 -> LocaleListCompat.forLanguageTags("vi")
                    else -> LocaleListCompat.getEmptyLocaleList()
                }
                AppCompatDelegate.setApplicationLocales(locales)
            }
            .show()
    }

    // endregion

    // region Notifications ----------------------------------------------------

    private fun openNotificationSettings() {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                .putExtra(Settings.EXTRA_CHANNEL_ID, Constants.CHANNEL_ID)
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                .setData(Uri.fromParts("package", packageName, null))
        }
        runCatching { startActivity(intent) }.onFailure { toast(getString(R.string.error_no_app)) }
    }

    // endregion

    // region Data -------------------------------------------------------------

    private fun writeExport(uri: Uri) {
        runCatching {
            contentResolver.openOutputStream(uri)?.use { it.write(repository.exportJson().toByteArray()) }
        }.onSuccess { toast(getString(R.string.export_done)) }
            .onFailure { toast(getString(R.string.export_failed)) }
    }

    private fun readImport(uri: Uri) {
        val text = runCatching {
            contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
        }.getOrNull()

        val count = text?.let { repository.importJson(it) } ?: -1
        if (count < 0) {
            toast(getString(R.string.import_failed))
            return
        }
        AppWatchService.sync(this)
        toast(getString(R.string.import_done, count))
    }

    private fun confirmClearAll() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.clear_all_title)
            .setMessage(R.string.clear_all_message)
            .setPositiveButton(R.string.action_delete) { _, _ ->
                repository.clearAll()
                AppWatchService.sync(this)
                toast(getString(R.string.clear_all_done))
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    // endregion

    private fun openUrl(url: String) {
        runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
            .onFailure { toast(getString(R.string.error_no_app)) }
    }

    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

    private fun applySystemBarInsets() {
        val root = findViewById<View>(R.id.rootSettings)
        val base = (24 * resources.displayMetrics.density).toInt()
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(base, bars.top + base, base, bars.bottom + base)
            insets
        }
    }

    companion object {
        private const val GITHUB_URL = "https://github.com/dybe1028/Notifyer"
        private const val VERSION_TAP_THRESHOLD = 7
    }
}
