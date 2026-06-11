package com.dybe.notifyer

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat

object NotificationHelper {

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val channel = NotificationChannel(
            Constants.CHANNEL_ID,
            Constants.CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        )
        context.getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }

    /**
     * Posts a notification. A unique [notificationId] per reminder means each
     * reminder shows its own notification instead of overwriting the previous one.
     */
    fun show(context: Context, notificationId: Int, title: String, message: String) {
        ensureChannel(context)

        val notification = NotificationCompat.Builder(context, Constants.CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(notificationId, notification)
    }
}
