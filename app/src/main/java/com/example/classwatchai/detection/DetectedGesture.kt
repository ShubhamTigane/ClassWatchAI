package com.example.classwatchai.detection

data class DetectedGesture(
    val type: String,
    val confidence: Float,
    val timestamp: Long = System.currentTimeMillis(),
    val duration: Int? = null,
    val note: String? = null
)
