package com.example.classwatchai.utils

import java.text.SimpleDateFormat
import java.util.*

object TimestampUtils {

    // Example format: "27 Jun 2025, 10:45 AM"
    private val formatter = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())

    fun formatTimestamp(timestamp: Long): String {
        return formatter.format(Date(timestamp))
    }
}