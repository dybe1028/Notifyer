package com.example.notifyer

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class CreateScheduleActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_schedule)

        val edtMessage =
            findViewById<EditText>(R.id.edtMessage)

        val edtDate =
            findViewById<EditText>(R.id.edtDate)

        val edtTime =
            findViewById<EditText>(R.id.edtTime)

        val btnSave =
            findViewById<Button>(R.id.btnSaveSchedule)

        btnSave.setOnClickListener {

            val message =
                edtMessage.text.toString().trim()

            val date =
                edtDate.text.toString().trim()

            val time =
                edtTime.text.toString().trim()

            if (
                message.isNotEmpty() &&
                date.isNotEmpty() &&
                time.isNotEmpty()
            ) {

                val data =
                    "schedule|$message|$date|$time"

                val prefs =
                    getSharedPreferences(
                        "notifyer_data",
                        MODE_PRIVATE
                    )

                val oldSet =
                    prefs.getStringSet(
                        "notify_list",
                        mutableSetOf()
                    ) ?: mutableSetOf()

                val list = oldSet.toMutableList()

                list.add(data)

                prefs.edit()
                    .putStringSet(
                        "notify_list",
                        list.toSet()
                    )
                    .apply()

                Toast.makeText(
                    this,
                    "Schedule Saved",
                    Toast.LENGTH_SHORT
                ).show()

                finish()
            }
        }
    }
}