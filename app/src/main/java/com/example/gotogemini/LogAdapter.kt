package com.example.gotogemini

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class LogAdapter : ListAdapter<LogEntry, LogAdapter.ViewHolder>(DiffCallback()) {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val timeText: TextView = view.findViewById(R.id.logTime)
        val tagText: TextView = view.findViewById(R.id.logTag)
        val messageText: TextView = view.findViewById(R.id.logMessage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_log, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val entry = getItem(position)
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.US)

        holder.timeText.text = sdf.format(Date(entry.timestamp))
        holder.tagText.text = entry.tag
        holder.messageText.text = entry.message

        val tagColor = when (entry.tag) {
            "Gemini" -> Color.parseColor("#4285F4")
            "YouTube" -> Color.parseColor("#FF0000")
            "Screenshot" -> Color.parseColor("#34A853")
            "System" -> Color.parseColor("#FBBC04")
            "Error" -> Color.parseColor("#EA4335")
            "Web" -> Color.parseColor("#9C27B0")
            else -> Color.parseColor("#9E9E9E")
        }
        holder.tagText.setTextColor(tagColor)
    }

    class DiffCallback : DiffUtil.ItemCallback<LogEntry>() {
        override fun areItemsTheSame(a: LogEntry, b: LogEntry) = a.timestamp == b.timestamp
        override fun areContentsTheSame(a: LogEntry, b: LogEntry) = a == b
    }
}
