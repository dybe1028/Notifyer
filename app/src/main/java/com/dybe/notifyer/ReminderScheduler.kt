package com.dybe.notifyer

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

/**
 * Schedules reminders with AlarmManager so they fire even when the app is closed.
 *
 * Falls back to an inexact alarm when the user has not granted the exact-alarm
 * permission (Android 12+), instead of crashing with a SecurityException.
 */
object ReminderScheduler {

    fun schedule(context: Context, reminder: Reminder) {
        if (reminder.triggerAtMillis <= 0L) return

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = buildPendingIntent(context, reminder)

        val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            alarmManager.canScheduleExactAlarms()

        if (canExact) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP, reminder.triggerAtMillis, pendingIntent
            )
        } else {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP, reminder.triggerAtMillis, pendingIntent
            )
        }
    }

    fun cancel(context: Context, reminder: Reminder) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(buildPendingIntent(context, reminder))
    }

    private fun buildPendingIntent(context: Context, reminder: Reminder): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = Constants.ACTION_FIRE
            putExtra(Constants.EXTRA_REMINDER_ID, reminder.id)
            putExtra(Constants.EXTRA_MESSAGE, reminder.message)
        }
        return PendingIntent.getBroadcast(
            context,
            reminder.requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
