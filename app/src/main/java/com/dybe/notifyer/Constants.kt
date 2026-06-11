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

    const val EXTRA_REMINDER_ID = "reminder_id"
    const val EXTRA_MESSAGE = "reminder_message"

    const val ACTION_FIRE = "com.dybe.notifyer.action.FIRE"
}
