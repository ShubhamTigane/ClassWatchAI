package com.example.classwatchai.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "gesture_logs")
data class GestureLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long,
    val gesture_type: String,
    val confidence: Float,
    val duration: Int? = null,
    val framePath: String? = null,
    val note: String? = null
)

