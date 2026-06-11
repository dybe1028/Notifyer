package com.dybe.notifyer

import android.content.Context
import org.json.JSONArray
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Ordered, id-based persistence for reminders.
 *
 * Fixes the original StringSet design which lost duplicates, reordered the list
 * on every read and made position-based edit/delete unreliable.
 */
class ReminderRepository(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)

    init {
        migrateLegacyDataIfNeeded()
    }

    fun getAll(): MutableList<Reminder> {
        val raw = prefs.getString(Constants.KEY_REMINDERS, null) ?: return mutableListOf()
        val list = mutableListOf<Reminder>()
        val array = runCatching { JSONArray(raw) }.getOrNull() ?: return list
        for (i in 0 until array.length()) {
            runCatching { Reminder.fromJson(array.getJSONObject(i)) }
                .getOrNull()
                ?.let { list.add(it) }
        }
        return list
    }

    fun findById(id: String): Reminder? = getAll().firstOrNull { it.id == id }

    fun add(reminder: Reminder) {
        val list = getAll()
        list.add(reminder)
        saveAll(list)
    }

    fun update(reminder: Reminder) {
        val list = getAll()
        val index = list.indexOfFirst { it.id == reminder.id }
        if (index == -1) return
        list[index] = reminder
        saveAll(list)
    }

    fun delete(id: String) {
        val list = getAll()
        list.removeAll { it.id == id }
        saveAll(list)
    }

    private fun saveAll(list: List<Reminder>) {
        val array = JSONArray()
        list.forEach { array.put(it.toJson()) }
        prefs.edit().putString(Constants.KEY_REMINDERS, array.toString()).apply()
    }

    // region Legacy migration -------------------------------------------------

    private fun migrateLegacyDataIfNeeded() {
        if (prefs.contains(Constants.KEY_REMINDERS)) return
        val legacy = prefs.getStringSet(Constants.KEY_LEGACY_LIST, null) ?: return

        val migrated = legacy.mapNotNull { parseLegacy(it) }
        saveAll(migrated)
        prefs.edit().remove(Constants.KEY_LEGACY_LIST).apply()
    }

    private fun parseLegacy(raw: String): Reminder? {
        val parts = raw.split("|")
        return when {
            parts.size >= 4 && parts[0] == "schedule" -> Reminder(
                type = ReminderType.SCHEDULE,
                message = parts[1],
                triggerAtMillis = parseLegacyDateTime(parts[2], parts[3])
            )

            parts.size >= 2 -> {
                val seconds = parts[1].toLongOrNull() ?: return null
                Reminder(type = ReminderType.TIMER, message = parts[0], seconds = seconds)
            }

            else -> null
        }
    }

    private fun parseLegacyDateTime(date: String, time: String): Long = try {
        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            .parse("$date $time")?.time ?: 0L
    } catch (e: ParseException) {
        0L
    }

    // endregion
}
