package com.dybe.notifyer

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Renders timer / schedule / app-trigger reminders as a name + detail line.
 *
 *  - Normal: tap expands the row in place, revealing Run / Edit / Delete; long-press
 *    starts multi-select.
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

    private val dateTimeFormat = SimpleDateFormat("dd/MM/yyyy  HH:mm", Locale.getDefault())

    private var selectionMode = false
    private val selectedIds = linkedSetOf<String>()
    private val expandedIds = linkedSetOf<String>()

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtTitle: TextView = view.findViewById(R.id.txtTitle)
        val txtSubtitle: TextView = view.findViewById(R.id.txtSubtitle)
        val checkbox: CheckBox = view.findViewById(R.id.checkbox)
        val icon: ImageView = view.findViewById(R.id.icon)
        val actions: View = view.findViewById(R.id.actions)
        val btnRun: View = view.findViewById(R.id.btnRun)
        val btnEdit: View = view.findViewById(R.id.btnEdit)
        val btnDelete: View = view.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notify, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val reminder = items[position]
        val context = holder.itemView.context

        holder.txtTitle.text = reminder.message
        holder.txtSubtitle.text = subtitle(context, reminder)
        holder.icon.setImageResource(iconFor(reminder.type))

        holder.checkbox.visibility = if (selectionMode) View.VISIBLE else View.GONE
        holder.checkbox.isChecked = selectedIds.contains(reminder.id)

        holder.actions.visibility =
            if (!selectionMode && expandedIds.contains(reminder.id)) View.VISIBLE else View.GONE
        holder.btnRun.visibility = if (reminder.type == ReminderType.TIMER) View.VISIBLE else View.GONE

        holder.btnRun.setOnClickListener { collapse(reminder); onRun(reminder) }
        holder.btnEdit.setOnClickListener { collapse(reminder); onEdit(reminder) }
        holder.btnDelete.setOnClickListener { collapse(reminder); onDelete(reminder) }

        holder.itemView.setOnClickListener {
            if (selectionMode) toggleSelection(reminder) else toggleExpand(reminder, holder)
        }
        holder.itemView.setOnLongClickListener {
            if (selectionMode) toggleSelection(reminder) else startSelection(reminder)
            true
        }
    }

    override fun getItemCount(): Int = items.size

    fun selectedReminders(): List<Reminder> = items.filter { selectedIds.contains(it.id) }

    fun isSelectionMode(): Boolean = selectionMode

    fun exitSelectionMode() {
        if (!selectionMode) return
        selectionMode = false
        selectedIds.clear()
        notifyDataSetChanged()
    }

    // region Expand -----------------------------------------------------------

    /** Toggling visibility on the bound view lets the card's layout transition animate it. */
    private fun toggleExpand(reminder: Reminder, holder: ViewHolder) {
        val expand = !expandedIds.contains(reminder.id)
        if (expand) expandedIds.add(reminder.id) else expandedIds.remove(reminder.id)
        holder.actions.visibility = if (expand) View.VISIBLE else View.GONE
    }

    private fun collapse(reminder: Reminder) {
        expandedIds.remove(reminder.id)
    }

    // endregion

    // region Selection --------------------------------------------------------

    private fun startSelection(reminder: Reminder) {
        selectionMode = true
        expandedIds.clear()
        selectedIds.add(reminder.id)
        notifyDataSetChanged()
        onSelectionModeStart()
        onSelectionChanged(selectedIds.size)
    }

    private fun toggleSelection(reminder: Reminder) {
        if (!selectedIds.add(reminder.id)) selectedIds.remove(reminder.id)
        notifyDataSetChanged()
        onSelectionChanged(selectedIds.size)
    }

    // endregion

    private fun iconFor(type: ReminderType): Int = when (type) {
        ReminderType.TIMER -> R.drawable.ic_timer
        ReminderType.SCHEDULE -> R.drawable.ic_schedule
        ReminderType.APP_TRIGGER -> R.drawable.ic_app_trigger
    }

    /** The detail line shown under the reminder name. */
    private fun subtitle(context: Context, reminder: Reminder): String = when (reminder.type) {
        ReminderType.TIMER -> {
            val duration = formatDuration(context, reminder.seconds)
            if (reminder.triggerAtMillis > System.currentTimeMillis()) {
                context.getString(R.string.sub_timer_scheduled, duration)
            } else {
                duration
            }
        }

        ReminderType.SCHEDULE -> {
            val dateTime = dateTimeFormat.format(Date(reminder.triggerAtMillis))
            if (reminder.repeat == RepeatMode.NONE) {
                dateTime
            } else {
                context.getString(
                    R.string.sub_schedule_repeat, dateTime, repeatLabel(context, reminder.repeat)
                )
            }
        }

        ReminderType.APP_TRIGGER ->
            if (reminder.appOpenThreshold > 1) {
                context.getString(R.string.sub_app_count, reminder.appLabel, reminder.appOpenThreshold)
            } else {
                reminder.appLabel
            }
    }

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
}
