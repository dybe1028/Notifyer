package com.dybe.notifyer

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointForward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class CreateScheduleActivity : AppCompatActivity() {

    private lateinit var repository: ReminderRepository
    private val selected: Calendar = Calendar.getInstance()
    private var hasDate = false
    private var hasTime = false
    private var editing: Reminder? = null

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_schedule)

        repository = ReminderRepository(this)

        val edtMessage = findViewById<EditText>(R.id.edtMessage)
        val edtDate = findViewById<EditText>(R.id.edtDate)
        val edtTime = findViewById<EditText>(R.id.edtTime)
        val btnSave = findViewById<Button>(R.id.btnSaveSchedule)
        val spinnerRepeat = findViewById<Spinner>(R.id.spinnerRepeat)

        spinnerRepeat.adapter = ArrayAdapter.createFromResource(
            this, R.array.repeat_modes, android.R.layout.simple_spinner_item
        ).apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }

        // Pickers guarantee valid date/time instead of free-text parsing.
        edtDate.isFocusable = false
        edtTime.isFocusable = false

        intent.getStringExtra(Constants.EXTRA_REMINDER_ID)?.let { id ->
            editing = repository.findById(id)
            editing?.let { reminder ->
                edtMessage.setText(reminder.message)
                spinnerRepeat.setSelection(reminder.repeat.ordinal)
                if (reminder.triggerAtMillis > 0L) {
                    selected.timeInMillis = reminder.triggerAtMillis
                    hasDate = true
                    hasTime = true
                    edtDate.setText(dateFormat.format(selected.time))
                    edtTime.setText(timeFormat.format(selected.time))
                }
            }
        }

        edtDate.setOnClickListener {
            val constraints = CalendarConstraints.Builder()
                .setValidator(DateValidatorPointForward.now())
                .build()

            val picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(getString(R.string.hint_date))
                .setSelection(maxOf(selectedDateUtc(), MaterialDatePicker.todayInUtcMilliseconds()))
                .setCalendarConstraints(constraints)
                .build()

            picker.addOnPositiveButtonClickListener { utcMillis ->
                val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                utc.timeInMillis = utcMillis
                selected.set(Calendar.YEAR, utc.get(Calendar.YEAR))
                selected.set(Calendar.MONTH, utc.get(Calendar.MONTH))
                selected.set(Calendar.DAY_OF_MONTH, utc.get(Calendar.DAY_OF_MONTH))
                hasDate = true
                edtDate.setText(dateFormat.format(selected.time))
            }
            picker.show(supportFragmentManager, "date_picker")
        }

        edtTime.setOnClickListener {
            val picker = MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_24H)
                .setHour(selected.get(Calendar.HOUR_OF_DAY))
                .setMinute(selected.get(Calendar.MINUTE))
                .setTitleText(getString(R.string.hint_time))
                .build()

            picker.addOnPositiveButtonClickListener {
                selected.set(Calendar.HOUR_OF_DAY, picker.hour)
                selected.set(Calendar.MINUTE, picker.minute)
                selected.set(Calendar.SECOND, 0)
                selected.set(Calendar.MILLISECOND, 0)
                hasTime = true
                edtTime.setText(timeFormat.format(selected.time))
            }
            picker.show(supportFragmentManager, "time_picker")
        }

        btnSave.setOnClickListener {
            val message = edtMessage.text.toString().trim()

            if (message.isEmpty() || !hasDate || !hasTime) {
                Toast.makeText(this, R.string.error_invalid_input, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (selected.timeInMillis <= System.currentTimeMillis()) {
                Toast.makeText(this, R.string.error_past_time, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val repeat = RepeatMode.entries[spinnerRepeat.selectedItemPosition]
            val current = editing
            val reminder = current?.copy(
                message = message,
                triggerAtMillis = selected.timeInMillis,
                repeat = repeat
            ) ?: Reminder(
                type = ReminderType.SCHEDULE,
                message = message,
                triggerAtMillis = selected.timeInMillis,
                repeat = repeat
            )

            if (current != null) {
                ReminderScheduler.cancel(this, current)
                repository.update(reminder)
            } else {
                repository.add(reminder)
            }
            ReminderScheduler.schedule(this, reminder)

            Toast.makeText(this, R.string.schedule_saved, Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    /** Current selected date as UTC-midnight millis, the format MaterialDatePicker expects. */
    private fun selectedDateUtc(): Long {
        val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        utc.clear()
        utc.set(
            selected.get(Calendar.YEAR),
            selected.get(Calendar.MONTH),
            selected.get(Calendar.DAY_OF_MONTH)
        )
        return utc.timeInMillis
    }
}
