package com.example.notifyer

import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class NotifyAdapter(
    private val items: ArrayList<String>,
    private val onRun: (String) -> Unit,
    private val onEdit: (Int) -> Unit,
    private val onDelete: (Int) -> Unit
) : RecyclerView.Adapter<NotifyAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtItem: TextView = view.findViewById(R.id.txtItem)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {

        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notify, parent, false)

        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val raw = items[position]

        val parts = raw.split("|")

        if (parts.size < 2) {
            holder.txtItem.text = raw
            return
        }

        val message = parts[0]
        val second = parts[1].toLongOrNull() ?: 0L

        holder.txtItem.text = "$message ($second s)"

        holder.itemView.setOnClickListener {

            val popup = PopupMenu(holder.itemView.context, holder.itemView)

            popup.menu.add("Run")
            popup.menu.add("Edit")
            popup.menu.add("Delete")

            popup.setOnMenuItemClickListener {

                when (it.title.toString()) {

                    "Run" -> {

                        object : CountDownTimer(second * 1000, 1000) {

                            override fun onTick(millisUntilFinished: Long) {

                                val remain = millisUntilFinished / 1000

                                holder.txtItem.text =
                                    "$message ($remain s)"
                            }

                            override fun onFinish() {

                                holder.txtItem.text =
                                    "$message (Done)"

                                onRun(message)
                            }

                        }.start()
                    }

                    "Edit" -> {
                        onEdit(position)
                    }

                    "Delete" -> {

                        android.app.AlertDialog.Builder(holder.itemView.context)
                            .setTitle("Delete Timer")
                            .setMessage("Are you sure?")
                            .setPositiveButton("Delete") { _, _ ->
                                onDelete(position)
                            }
                            .setNegativeButton("Cancel", null)
                            .show()
                    }
                }

                true
            }

            popup.show()
        }
    }

    override fun getItemCount(): Int = items.size
}