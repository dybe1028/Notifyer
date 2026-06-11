package com.dybe.notifyer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Alarms are cleared on reboot. This re-registers every still-pending reminder
 * so scheduled notifications survive a restart.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val now = System.currentTimeMillis()
        ReminderRepository(context).getAll()
            .filter { it.triggerAtMillis > now }
            .forEach { ReminderScheduler.schedule(context, it) }
    }
}
