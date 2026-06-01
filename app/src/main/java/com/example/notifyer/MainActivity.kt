package com.example.notifyer

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.widget.TextView

class MainActivity : AppCompatActivity() {

    private val CHANNEL_ID = "notifyer_channel"

    private lateinit var adapter: NotifyAdapter
    private lateinit var notifyList: ArrayList<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        createNotificationChannel()

        val btnAdd =
            findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(
                R.id.btnAdd
            )

        val recycler = findViewById<RecyclerView>(R.id.recyclerNotify)
        val txtEmpty = findViewById<TextView>(R.id.txtEmpty)

        val prefs = getSharedPreferences("notifyer_data", MODE_PRIVATE)

        val savedSet =
            prefs.getStringSet("notify_list", mutableSetOf())
                ?: mutableSetOf()

        notifyList = ArrayList(savedSet)

        adapter = NotifyAdapter(

            notifyList,

            { text ->
                sendNotification("Notifyer Saved", text)
            },

            { position ->

                val intent =
                    Intent(this, CreateTimerActivity::class.java)

                intent.putExtra("edit_index", position)
                intent.putExtra("edit_data", notifyList[position])

                startActivity(intent)
            },

            { position ->

                notifyList.removeAt(position)

                val prefs =
                    getSharedPreferences("notifyer_data", MODE_PRIVATE)

                prefs.edit()
                    .putStringSet("notify_list", notifyList.toSet())
                    .apply()

                adapter.notifyDataSetChanged()
                if (notifyList.isEmpty()) {
                    txtEmpty.visibility = android.view.View.VISIBLE
                } else {
                    txtEmpty.visibility = android.view.View.GONE
                }
            }
        )

        recycler.layoutManager = LinearLayoutManager(this)
        recycler.setHasFixedSize(true)
        recycler.adapter = adapter
        if (notifyList.isEmpty()) {
            txtEmpty.visibility = android.view.View.VISIBLE
        } else {
            txtEmpty.visibility = android.view.View.GONE
        }

        btnAdd.setOnClickListener {

            val options = arrayOf(
                "Countdown Timer",
                "Schedule Reminder"
            )

            android.app.AlertDialog.Builder(this)
                .setTitle("Choose Reminder Type")
                .setItems(options) { _, which ->

                    when (which) {

                        0 -> {
                            startActivity(
                                Intent(
                                    this,
                                    CreateTimerActivity::class.java
                                )
                            )
                        }

                        1 -> {
                            startActivity(
                                Intent(
                                    this,
                                    CreateScheduleActivity::class.java
                                )
                            )
                        }
                    }
                }
                .show()
        }
    }

    override fun onResume() {
        super.onResume()

        val prefs = getSharedPreferences("notifyer_data", MODE_PRIVATE)

        val savedSet =
            prefs.getStringSet("notify_list", mutableSetOf())
                ?: mutableSetOf()

        notifyList.clear()
        notifyList.addAll(savedSet)

        adapter.notifyDataSetChanged()
    }

    private fun sendNotification(title: String, message: String) {
        val manager =
            getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        val builder =
            androidx.core.app.NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(
                    androidx.core.app.NotificationCompat.PRIORITY_HIGH
                )

        manager.notify(1, builder.build())
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            val channel = NotificationChannel(
                CHANNEL_ID,
                "Notifyer Channel",
                NotificationManager.IMPORTANCE_HIGH
            )

            val manager =
                getSystemService(NotificationManager::class.java)

            manager.createNotificationChannel(channel)
        }
    }
}