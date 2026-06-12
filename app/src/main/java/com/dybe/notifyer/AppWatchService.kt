package com.dybe.notifyer

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

/**
 * Foreground service that polls UsageStats to detect when a watched app comes to
 * the foreground, then fires the matching APP_TRIGGER reminder.
 *
 * Runs only while there is at least one app-trigger AND usage access is granted.
 */
class AppWatchService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var usageStats: UsageStatsManager

    private var triggers: MutableMap<String, Reminder> = mutableMapOf()
    private var lastForegroundPackage: String? = null
    private var lastQueryTime = System.currentTimeMillis()

    private val pollRunnable = object : Runnable {
        override fun run() {
            checkForeground()
            handler.postDelayed(this, POLL_INTERVAL_MS)
        }
    }

    override fun onCreate() {
        super.onCreate()
        usageStats = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        startAsForeground()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        reloadTriggers()
        if (triggers.isEmpty()) {
            stopSelf()
            return START_NOT_STICKY
        }
        handler.removeCallbacks(pollRunnable)
        lastQueryTime = System.currentTimeMillis()
        handler.post(pollRunnable)
        return START_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacks(pollRunnable)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun reloadTriggers() {
        triggers = ReminderRepository(this).getAll()
            .filter { it.type == ReminderType.APP_TRIGGER && it.packageName.isNotEmpty() }
            .associateBy { it.packageName }
            .toMutableMap()
    }

    private fun checkForeground() {
        val now = System.currentTimeMillis()
        val events = usageStats.queryEvents(lastQueryTime, now)
        lastQueryTime = now

        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            @Suppress("DEPRECATION")
            if (event.eventType != UsageEvents.Event.MOVE_TO_FOREGROUND) continue

            val pkg = event.packageName ?: continue
            if (pkg == lastForegroundPackage) continue
            lastForegroundPackage = pkg
            if (pkg == packageName) continue // ignore our own app coming forward

            triggers[pkg]?.let { trigger -> onWatchedAppOpened(pkg, trigger) }
        }
    }

    /** Counts the open; fires (and resets the counter) once every [Reminder.appOpenThreshold] opens. */
    private fun onWatchedAppOpened(pkg: String, trigger: Reminder) {
        val next = trigger.appOpenCount + 1
        val updated = if (next >= trigger.appOpenThreshold) {
            fire(trigger)
            trigger.copy(appOpenCount = 0)
        } else {
            trigger.copy(appOpenCount = next)
        }
        triggers[pkg] = updated
        ReminderRepository(this).update(updated)
    }

    private fun fire(trigger: Reminder) {
        NotificationHelper.show(
            context = this,
            notificationId = trigger.id.hashCode(),
            title = getString(R.string.notification_title),
            message = trigger.message
        )
    }

    private fun startAsForeground() {
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                Constants.WATCH_NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(Constants.WATCH_NOTIFICATION_ID, notification)
        }
    }

    private fun buildNotification(): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                Constants.WATCH_CHANNEL_ID,
                Constants.WATCH_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }

        val openApp = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, Constants.WATCH_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(getString(R.string.watch_service_title))
            .setContentText(getString(R.string.watch_service_text))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setContentIntent(openApp)
            .build()
    }

    companion object {
        private const val POLL_INTERVAL_MS = 2000L

        /** Starts the watcher when it is needed, stops it otherwise. Safe to call anytime. */
        fun sync(context: Context) {
            val hasTriggers = ReminderRepository(context).getAll()
                .any { it.type == ReminderType.APP_TRIGGER && it.packageName.isNotEmpty() }
            val intent = Intent(context, AppWatchService::class.java)

            if (hasTriggers && UsageAccess.isGranted(context)) {
                ContextCompat.startForegroundService(context, intent)
            } else {
                context.stopService(intent)
            }
        }
    }
}
