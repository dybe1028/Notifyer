package com.dybe.notifyer

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.google.android.material.textfield.MaterialAutoCompleteTextView

class CreateTimerActivity : BaseActivity() {

    private lateinit var repository: ReminderRepository
    private var editing: Reminder? = null
    private var unitIndex = 0

    /** Unit index -> multiplier to convert the entered amount into canonical seconds. */
    private val unitMultipliers = longArrayOf(1L, 60L, 3600L) // seconds, minutes, hours

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_timer)

        repository = ReminderRepository(this)

        val edtMessage = findViewById<EditText>(R.id.edtMessage)
        val edtAmount = findViewById<EditText>(R.id.edtAmount)
        val unitDropdown = findViewById<MaterialAutoCompleteTextView>(R.id.spinnerUnit)
        val btnSave = findViewById<Button>(R.id.btnSaveTimer)

        val units = resources.getStringArray(R.array.time_units)
        unitDropdown.setText(units[0], false)
        unitDropdown.setOnItemClickListener { _, _, position, _ -> unitIndex = position }

        intent.getStringExtra(Constants.EXTRA_REMINDER_ID)?.let { id ->
            editing = repository.findById(id)
            editing?.let { reminder ->
                edtMessage.setText(reminder.message)
                unitIndex = largestUnitIndex(reminder.seconds)
                edtAmount.setText((reminder.seconds / unitMultipliers[unitIndex]).toString())
                unitDropdown.setText(units[unitIndex], false)
            }
        }

        btnSave.setOnClickListener {
            val message = edtMessage.text.toString().trim()
            val amount = edtAmount.text.toString().trim().toLongOrNull()

            if (message.isEmpty() || amount == null || amount <= 0) {
                Toast.makeText(this, R.string.error_invalid_input, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val seconds = amount * unitMultipliers[unitIndex]

            val current = editing
            if (current != null) {
                repository.update(current.copy(message = message, seconds = seconds))
            } else {
                repository.add(
                    Reminder(type = ReminderType.TIMER, message = message, seconds = seconds)
                )
            }

            Toast.makeText(this, R.string.saved, Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    /** Picks the largest unit that divides [seconds] exactly, for a clean edit round-trip. */
    private fun largestUnitIndex(seconds: Long): Int = when {
        seconds >= 3600 && seconds % 3600 == 0L -> 2
        seconds >= 60 && seconds % 60 == 0L -> 1
        else -> 0
    }
}
