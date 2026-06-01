package com.example.notifyer

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class CreateTimerActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_timer)

        val edtMessage = findViewById<EditText>(R.id.edtMessage)
        val edtSecond = findViewById<EditText>(R.id.edtSecond)
        val btnSave = findViewById<Button>(R.id.btnSaveTimer)

        val editIndex = intent.getIntExtra("edit_index", -1)
        val editData = intent.getStringExtra("edit_data")

        if (editIndex != -1 && editData != null) {

            val parts = editData.split("|")

            if (parts.size >= 2) {
                edtMessage.setText(parts[0])
                edtSecond.setText(parts[1])
            }
        }

        btnSave.setOnClickListener {

            val text = edtMessage.text.toString().trim()
            val sec = edtSecond.text.toString().trim()

            val second = sec.toLongOrNull()

            if (text.isNotEmpty() && second != null && second > 0) {

                val prefs =
                    getSharedPreferences("notifyer_data", MODE_PRIVATE)

                val oldSet =
                    prefs.getStringSet("notify_list", mutableSetOf())
                        ?: mutableSetOf()

                val list = oldSet.toMutableList()

                if (editIndex != -1 && editIndex < list.size) {
                    list[editIndex] = "$text|$second"
                } else {
                    list.add("$text|$second")
                }

                prefs.edit()
                    .putStringSet("notify_list", list.toSet())
                    .apply()

                Toast.makeText(
                    this,
                    "Saved",
                    Toast.LENGTH_SHORT
                ).show()

                finish()
            } else {

                Toast.makeText(
                    this,
                    "Invalid input",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}