package com.dybe.notifyer

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.PopupWindow
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Renders both timer and schedule reminders.
 *
 * Two interaction modes:
 *  - Normal: tap opens the Run/Edit/Delete popup (unchanged); long-press starts
 *    multi-select.
 *  - Selection: a checkbox is shown and tap/long-press toggle the row. The host
 *    activity drives an ActionMode via [onSelectionModeStart] / [onSelectionChanged].
 */
class NotifyAdapter(
    private val items: List<Reminder>,
    private val onRun: (Reminder) -> Unit,
    private val onEdit: (Reminder) -> Unit,
    private val onDelete: (Reminder) -> Unit,
    private val onSelectionModeStart: () -> Unit,
    private val onSelectionChanged: (Int) -> Unit
) : RecyclerView.Adapter<NotifyAdapter.ViewHolder>() {

    private val dateTimeFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    private var selectionMode = false
    private val selectedIds = linkedSetOf<String>()

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtItem: TextView = view.findViewById(R.id.txtItem)
        val checkbox: CheckBox = view.findViewById(R.id.checkbox)
        val icon: ImageView = view.findViewById(R.id.icon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notify, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val reminder = items[position]
        holder.txtItem.text = describe(holder.itemView.context, reminder)
        holder.icon.setImageResource(
            when (reminder.type) {
                ReminderType.TIMER -> R.drawable.ic_timer
                ReminderType.SCHEDULE -> R.drawable.ic_schedule
                ReminderType.APP_TRIGGER -> R.drawable.ic_app_trigger
            }
        )

        holder.checkbox.visibility = if (selectionMode) View.VISIBLE else View.GONE
        holder.checkbox.isChecked = selectedIds.contains(reminder.id)

        holder.itemView.setOnClickListener { anchor ->
            if (selectionMode) toggle(reminder) else showActionsPopup(anchor, reminder)
        }
        holder.itemView.setOnLongClickListener {
            if (selectionMode) toggle(reminder) else startSelection(reminder)
            true
        }
    }

    override fun getItemCount(): Int = items.size

    // region Selection --------------------------------------------------------

    fun isSelectionMode(): Boolean = selectionMode

    fun selectedReminders(): List<Reminder> = items.filter { selectedIds.contains(it.id) }

    /** Called by the host when the ActionMode is dismissed. */
    fun exitSelectionMode() {
        if (!selectionMode) return
        selectionMode = false
        selectedIds.clear()
        notifyDataSetChanged()
    }

    private fun startSelection(reminder: Reminder) {
        selectionMode = true
        selectedIds.add(reminder.id)
        notifyDataSetChanged()
        onSelectionModeStart()
        onSelectionChanged(selectedIds.size)
    }

    private fun toggle(reminder: Reminder) {
        if (!selectedIds.add(reminder.id)) selectedIds.remove(reminder.id)
        notifyDataSetChanged()
        onSelectionChanged(selectedIds.size)
    }

    // endregion

    private fun describe(context: Context, reminder: Reminder): String = when (reminder.type) {
        ReminderType.TIMER ->
            if (reminder.triggerAtMillis > System.currentTimeMillis()) {
                context.getString(R.string.item_timer_running, reminder.message)
            } else {
                context.getString(
                    R.string.item_timer, reminder.message, formatDuration(context, reminder.seconds)
                )
            }

        ReminderType.SCHEDULE -> {
            val time = dateTimeFormat.format(Date(reminder.triggerAtMillis))
            if (reminder.repeat == RepeatMode.NONE) {
                context.getString(R.string.item_schedule, reminder.message, time)
            } else {
                context.getString(
                    R.string.item_schedule_repeat,
                    reminder.message, time, repeatLabel(context, reminder.repeat)
                )
            }
        }

        ReminderType.APP_TRIGGER ->
            if (reminder.appOpenThreshold > 1) {
                context.getString(
                    R.string.item_app_trigger_count,
                    reminder.appLabel, reminder.appOpenThreshold, reminder.message
                )
            } else {
                context.getString(R.string.item_app_trigger, reminder.appLabel, reminder.message)
            }
    }

    /** Formats canonical seconds into the largest exact unit (h / m / s). */
    private fun formatDuration(context: Context, seconds: Long): String = when {
        seconds >= 3600 && seconds % 3600 == 0L ->
            context.getString(R.string.dur_hours, seconds / 3600)
        seconds >= 60 && seconds % 60 == 0L ->
            context.getString(R.string.dur_minutes, seconds / 60)
        else ->
            context.getString(R.string.dur_seconds, seconds)
    }

    private fun repeatLabel(context: Context, repeat: RepeatMode): String = when (repeat) {
        RepeatMode.WEEKLY -> context.getString(R.string.repeat_weekly)
        else -> context.getString(R.string.repeat_daily)
    }

    /** A small Material card of actions, dropped right under the tapped item. */
    private fun showActionsPopup(anchor: View, reminder: Reminder) {
        val context = anchor.context
        val content = LayoutInflater.from(context).inflate(R.layout.popup_actions, null)

        val popup = PopupWindow(
            content,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        ).apply {
            isOutsideTouchable = true
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }

        val runRow = content.findViewById<View>(R.id.actionRun)
        runRow.visibility = if (reminder.type == ReminderType.TIMER) View.VISIBLE else View.GONE
        runRow.setOnClickListener { popup.dismiss(); onRun(reminder) }
        content.findViewById<View>(R.id.actionEdit).setOnClickListener { popup.dismiss(); onEdit(reminder) }
        content.findViewById<View>(R.id.actionDelete).setOnClickListener { popup.dismiss(); onDelete(reminder) }

        val density = context.resources.displayMetrics.density
        popup.showAsDropDown(anchor, (20 * density).toInt(), (-6 * density).toInt(), Gravity.START)
    }
}
