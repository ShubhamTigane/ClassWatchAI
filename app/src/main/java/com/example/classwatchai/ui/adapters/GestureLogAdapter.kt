package com.example.classwatchai.ui.adapters

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.classwatchai.R
import com.example.classwatchai.data.db.GestureLog
import com.example.classwatchai.utils.TimestampUtils
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class GestureLogAdapter(
    private var logs: List<GestureLog>
) : RecyclerView.Adapter<GestureLogAdapter.LogVH>() {

    inner class LogVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgThumb: ImageView = itemView.findViewById(R.id.imgThumb)
        val txtType : TextView  = itemView.findViewById(R.id.txtType)
        val txtInfo : TextView  = itemView.findViewById(R.id.txtInfo)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogVH =
        LogVH(LayoutInflater.from(parent.context)
            .inflate(R.layout.item_gesture_log, parent, false))

    override fun onBindViewHolder(holder: LogVH, position: Int) {
        val log = logs[position]
        holder.txtType.text = log.gesture_type.replace('_',' ').lowercase().capitalize()
        val time   = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(log.timestamp))
        val durTxt = log.duration?.let { " for ${it}ms" } ?: ""
        holder.txtInfo.text = "at $time  •  conf ${"%.2f".format(log.confidence)}$durTxt"

        // thumbnail
        if (log.framePath != null && File(log.framePath).exists()) {
            holder.imgThumb.setImageBitmap(BitmapFactory.decodeFile(log.framePath))
        } else {
            holder.imgThumb.setImageResource(R.drawable.ic_no_image) // small placeholder
        }
    }

    override fun getItemCount() = logs.size

    fun updateLogs(newLogs: List<GestureLog>) {
        logs = newLogs
        notifyDataSetChanged()
    }
}
