package com.dybe.notifyer

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MainActivity : AppCompatActivity() {

    private lateinit var repository: ReminderRepository
    private lateinit var adapter: NotifyAdapter
    private lateinit var emptyView: View
    private lateinit var recycler: RecyclerView

    private var actionMode: ActionMode? = null

    private val reminders = mutableListOf<Reminder>()

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* best effort */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        repository = ReminderRepository(this)
        NotificationHelper.ensureChannel(this)
        requestNotificationPermissionIfNeeded()
        rescheduleFutureReminders()

        emptyView = findViewById(R.id.emptyState)
        recycler = findViewById(R.id.recyclerNotify)
        val btnAdd = findViewById<FloatingActionButton>(R.id.btnAdd)

        // Footer version comes from the single source of truth (gradle versionName).
        findViewById<TextView>(R.id.txtCredit).text =
            getString(R.string.app_credit, BuildConfig.VERSION_NAME)

        adapter = NotifyAdapter(
            items = reminders,
            onRun = { reminder -> runTimer(reminder) },
            onEdit = { reminder -> editReminder(reminder) },
            onDelete = { reminder -> confirmDelete(reminder) },
            onSelectionModeStart = { startSelectionActionMode() },
            onSelectionChanged = { count -> updateActionMode(count) }
        )

        recycler.layoutManager = LinearLayoutManager(this)
        recycler.setHasFixedSize(true)
        recycler.adapter = adapter

        btnAdd.setOnClickListener { showCreateTypeDialog() }
    }

    override fun onResume() {
        super.onResume()
        reloadReminders()
    }

    private fun reloadReminders() {
        reminders.clear()
        reminders.addAll(repository.getAll())
        adapter.notifyDataSetChanged()
        emptyView.visibility = if (reminders.isEmpty()) View.VISIBLE else View.GONE
        if (reminders.isNotEmpty()) recycler.scheduleLayoutAnimation()
    }

    /** A timer is "remind me in N seconds": schedule a background alarm at now + N. */
    private fun runTimer(reminder: Reminder) {
        if (reminder.type != ReminderType.TIMER) return

        val triggerAt = System.currentTimeMillis() + reminder.seconds * 1000
        val scheduled = reminder.copy(triggerAtMillis = triggerAt)

        repository.update(scheduled)
        ReminderScheduler.schedule(this, scheduled)

        Toast.makeText(
            this,
            getString(R.string.reminder_scheduled_in, reminder.seconds),
            Toast.LENGTH_SHORT
        ).show()
        reloadReminders()
    }

    private fun editReminder(reminder: Reminder) {
        val target = when (reminder.type) {
            ReminderType.TIMER -> CreateTimerActivity::class.java
            ReminderType.SCHEDULE -> CreateScheduleActivity::class.java
        }
        startActivity(
            Intent(this, target).putExtra(Constants.EXTRA_REMINDER_ID, reminder.id)
        )
    }

    private fun confirmDelete(reminder: Reminder) {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.delete_title)
            .setMessage(R.string.delete_message)
            .setPositiveButton(R.string.action_delete) { _, _ ->
                ReminderScheduler.cancel(this, reminder)
                repository.delete(reminder.id)
                reloadReminders()
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    // region Multi-select -----------------------------------------------------

    private fun startSelectionActionMode() {
        if (actionMode == null) {
            actionMode = startSupportActionMode(selectionCallback)
        }
    }

    private fun updateActionMode(count: Int) {
        if (count == 0) {
            actionMode?.finish()
        } else {
            actionMode?.title = getString(R.string.selected_count, count)
        }
    }

    private fun confirmDeleteSelected() {
        val selected = adapter.selectedReminders()
        if (selected.isEmpty()) return

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.delete_title)
            .setMessage(getString(R.string.delete_selected_message, selected.size))
            .setPositiveButton(R.string.action_delete) { _, _ ->
                selected.forEach { reminder ->
                    ReminderScheduler.cancel(this, reminder)
                    repository.delete(reminder.id)
                }
                actionMode?.finish()
                reloadReminders()
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    private val selectionCallback = object : ActionMode.Callback {
        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            mode.menuInflater.inflate(R.menu.menu_selection, menu)
            return true
        }

        override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean = false

        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean =
            when (item.itemId) {
                R.id.action_delete -> {
                    confirmDeleteSelected()
                    true
                }
                else -> false
            }

        override fun onDestroyActionMode(mode: ActionMode) {
            adapter.exitSelectionMode()
            actionMode = null
        }
    }

    // endregion

    private fun showCreateTypeDialog() {
        val options = arrayOf(
            getString(R.string.type_timer),
            getString(R.string.type_schedule)
        )
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.choose_type_title)
            .setItems(options) { _, which ->
                val target = if (which == 0) {
                    CreateTimerActivity::class.java
                } else {
                    CreateScheduleActivity::class.java
                }
                startActivity(Intent(this, target))
            }
            .show()
    }

    /** Ensures alarms exist for every reminder due in the future (after upgrade/migration). */
    private fun rescheduleFutureReminders() {
        val now = System.currentTimeMillis()
        repository.getAll()
            .filter { it.triggerAtMillis > now }
            .forEach { ReminderScheduler.schedule(this, it) }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

        val granted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED

        if (!granted) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
