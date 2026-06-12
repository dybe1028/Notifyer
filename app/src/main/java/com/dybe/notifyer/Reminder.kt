package com.dybe.notifyer

import org.json.JSONObject
import java.util.UUID

enum class ReminderType { TIMER, SCHEDULE, APP_TRIGGER }

enum class RepeatMode { NONE, DAILY, WEEKLY }

/**
 * A single reminder. Stored with a stable [id] so edit/delete never depend on
 * the position inside an (unordered) collection again.
 *
 * @property seconds          duration for a TIMER reminder (canonical, always seconds)
 * @property triggerAtMillis  absolute epoch millis the alarm should fire; 0 = not scheduled
 * @property repeat           recurrence for a SCHEDULE reminder; NONE = one-shot
 * @property packageName      watched app for an APP_TRIGGER reminder; "" = none
 * @property appLabel         human-readable label of the watched app (cached)
 * @property appOpenThreshold fire once every N opens of the watched app (1 = every time)
 * @property appOpenCount     running open counter, reset to 0 after each fire
 */
data class Reminder(
    val id: String = UUID.randomUUID().toString(),
    val type: ReminderType,
    val message: String,
    val seconds: Long = 0L,
    val triggerAtMillis: Long = 0L,
    val repeat: RepeatMode = RepeatMode.NONE,
    val packageName: String = "",
    val appLabel: String = "",
    val appOpenThreshold: Int = 1,
    val appOpenCount: Int = 0
) {

    /** Stable request code for the AlarmManager PendingIntent and the notification id. */
    val requestCode: Int get() = id.hashCode()

    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("type", type.name)
        put("message", message)
        put("seconds", seconds)
        put("triggerAtMillis", triggerAtMillis)
        put("repeat", repeat.name)
        put("packageName", packageName)
        put("appLabel", appLabel)
        put("appOpenThreshold", appOpenThreshold)
        put("appOpenCount", appOpenCount)
    }

    companion object {
        fun fromJson(json: JSONObject): Reminder = Reminder(
            id = json.optString("id", UUID.randomUUID().toString()),
            type = runCatching { ReminderType.valueOf(json.optString("type")) }
                .getOrDefault(ReminderType.TIMER),
            message = json.optString("message", ""),
            seconds = json.optLong("seconds", 0L),
            triggerAtMillis = json.optLong("triggerAtMillis", 0L),
            repeat = runCatching { RepeatMode.valueOf(json.optString("repeat")) }
                .getOrDefault(RepeatMode.NONE),
            packageName = json.optString("packageName", ""),
            appLabel = json.optString("appLabel", ""),
            appOpenThreshold = json.optInt("appOpenThreshold", 1).coerceAtLeast(1),
            appOpenCount = json.optInt("appOpenCount", 0)
        )
    }
}
