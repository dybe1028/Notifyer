package com.dybe.notifyer

/**
 * Single source of truth for storage keys, channel ids and intent extras.
 * Replaces the magic strings that were duplicated across activities.
 */
object Constants {

    const val PREFS_NAME = "notifyer_data"

    /** New ordered, id-based storage (JSON array). */
    const val KEY_REMINDERS = "reminders_json"

    /** Legacy StringSet storage kept only for one-time migration. */
    const val KEY_LEGACY_LIST = "notify_list"

    const val CHANNEL_ID = "notifyer_channel"
    const val CHANNEL_NAME = "Reminders"

    /** Low-importance channel for the persistent app-watcher foreground notification. */
    const val WATCH_CHANNEL_ID = "notifyer_watch_channel"
    const val WATCH_CHANNEL_NAME = "App watcher"
    const val WATCH_NOTIFICATION_ID = 42001

    const val EXTRA_REMINDER_ID = "reminder_id"
    const val EXTRA_MESSAGE = "reminder_message"

    const val ACTION_FIRE = "com.dybe.notifyer.action.FIRE"
}
