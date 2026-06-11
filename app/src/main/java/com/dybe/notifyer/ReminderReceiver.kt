package com.dybe.notifyer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import java.util.Calendar

/**
 * Fired by AlarmManager when a reminder is due. Posts the notification, then either
 * clears a one-shot reminder or re-arms the next occurrence for a recurring one.
 */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getStringExtra(Constants.EXTRA_REMINDER_ID) ?: return
        val message = intent.getStringExtra(Constants.EXTRA_MESSAGE).orEmpty()

        NotificationHelper.show(
            context = context,
            notificationId = id.hashCode(),
            title = context.getString(R.string.notification_title),
            message = message
        )

        val repository = ReminderRepository(context)
        val reminder = repository.findById(id) ?: return

        if (reminder.repeat == RepeatMode.NONE) {
            repository.update(reminder.copy(triggerAtMillis = 0L))
        } else {
            val updated = reminder.copy(
                triggerAtMillis = nextOccurrence(reminder.triggerAtMillis, reminder.repeat)
            )
            repository.update(updated)
            ReminderScheduler.schedule(context, updated)
        }
    }

    /** Advances [fromMillis] by the repeat interval until it lands in the future. */
    private fun nextOccurrence(fromMillis: Long, repeat: RepeatMode): Long {
        val daysToAdd = if (repeat == RepeatMode.WEEKLY) 7 else 1
        val calendar = Calendar.getInstance().apply { timeInMillis = fromMillis }
        val now = System.currentTimeMillis()
        do {
            calendar.add(Calendar.DAY_OF_MONTH, daysToAdd)
        } while (calendar.timeInMillis <= now)
        return calendar.timeInMillis
    }
}
