package com.dybe.notifyer

import org.json.JSONObject
import java.util.UUID

enum class ReminderType { TIMER, SCHEDULE }

enum class RepeatMode { NONE, DAILY, WEEKLY }

/**
 * A single reminder. Stored with a stable [id] so edit/delete never depend on
 * the position inside an (unordered) collection again.
 *
 * @property seconds          duration for a TIMER reminder (canonical, always seconds)
 * @property triggerAtMillis  absolute epoch millis the alarm should fire; 0 = not scheduled
 * @property repeat           recurrence for a SCHEDULE reminder; NONE = one-shot
 */
data class Reminder(
    val id: String = UUID.randomUUID().toString(),
    val type: ReminderType,
    val message: String,
    val seconds: Long = 0L,
    val triggerAtMillis: Long = 0L,
    val repeat: RepeatMode = RepeatMode.NONE
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
                .getOrDefault(RepeatMode.NONE)
        )
    }
}
