package com.dybe.notifyer

import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class CreateAppTriggerActivity : AppCompatActivity() {

    private lateinit var repository: ReminderRepository
    private lateinit var edtApp: EditText
    private lateinit var edtMessage: EditText
    private lateinit var edtThreshold: EditText

    private var editing: Reminder? = null
    private var selectedPackage: String = ""
    private var selectedLabel: String = ""

    private data class AppInfo(val packageName: String, val label: String, val icon: Drawable)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_app_trigger)

        repository = ReminderRepository(this)

        edtApp = findViewById(R.id.edtApp)
        edtMessage = findViewById(R.id.edtMessage)
        edtThreshold = findViewById(R.id.edtThreshold)
        val btnSave = findViewById<Button>(R.id.btnSaveAppTrigger)

        edtApp.isFocusable = false
        edtApp.setOnClickListener { showAppPicker() }

        intent.getStringExtra(Constants.EXTRA_REMINDER_ID)?.let { id ->
            editing = repository.findById(id)
            editing?.let { reminder ->
                selectedPackage = reminder.packageName
                selectedLabel = reminder.appLabel
                edtApp.setText(reminder.appLabel)
                edtMessage.setText(reminder.message)
                edtThreshold.setText(reminder.appOpenThreshold.toString())
            }
        }

        btnSave.setOnClickListener { save() }
    }

    private fun save() {
        val message = edtMessage.text.toString().trim()
        if (selectedPackage.isEmpty() || message.isEmpty()) {
            Toast.makeText(this, R.string.error_invalid_input, Toast.LENGTH_SHORT).show()
            return
        }

        val threshold = edtThreshold.text.toString().trim().toIntOrNull()?.coerceAtLeast(1) ?: 1

        val current = editing
        val reminder = current?.copy(
            message = message,
            packageName = selectedPackage,
            appLabel = selectedLabel,
            appOpenThreshold = threshold,
            appOpenCount = 0
        ) ?: Reminder(
            type = ReminderType.APP_TRIGGER,
            message = message,
            packageName = selectedPackage,
            appLabel = selectedLabel,
            appOpenThreshold = threshold
        )

        if (current != null) repository.update(reminder) else repository.add(reminder)

        if (UsageAccess.isGranted(this)) {
            AppWatchService.sync(this)
            Toast.makeText(this, R.string.saved, Toast.LENGTH_SHORT).show()
            finish()
        } else {
            promptUsageAccess()
        }
    }

    /** Usage access cannot be granted from a dialog — send the user to Settings. */
    private fun promptUsageAccess() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.usage_access_title)
            .setMessage(R.string.usage_access_message)
            .setPositiveButton(R.string.action_open_settings) { _, _ ->
                UsageAccess.openSettings(this)
                finish()
            }
            .setNegativeButton(R.string.action_cancel) { _, _ -> finish() }
            .show()
    }

    private fun showAppPicker() {
        // Loading labels + icons for every launchable app off the main thread avoids jank.
        Thread {
            val apps = loadLaunchableApps()
            runOnUiThread { if (!isFinishing) showAppDialog(apps) }
        }.start()
    }

    private fun showAppDialog(apps: List<AppInfo>) {
        val adapter = object : ArrayAdapter<AppInfo>(this, 0, apps) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val row = convertView
                    ?: layoutInflater.inflate(R.layout.item_app, parent, false)
                val app = getItem(position)!!
                row.findViewById<ImageView>(R.id.appIcon).setImageDrawable(app.icon)
                row.findViewById<TextView>(R.id.appLabel).text = app.label
                return row
            }
        }

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.choose_app)
            .setAdapter(adapter) { _, which ->
                val app = apps[which]
                selectedPackage = app.packageName
                selectedLabel = app.label
                edtApp.setText(app.label)
            }
            .show()
    }

    private fun loadLaunchableApps(): List<AppInfo> {
        val pm = packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        @Suppress("DEPRECATION")
        val resolved = pm.queryIntentActivities(intent, 0)
        return resolved
            .map { AppInfo(it.activityInfo.packageName, it.loadLabel(pm).toString(), it.loadIcon(pm)) }
            .filter { it.packageName != packageName }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
    }
}
