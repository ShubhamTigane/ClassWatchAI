package com.example.classwatchai.detection

object GestureFilter {
    private val recentGestures = mutableMapOf<String, Long>()
    private const val COOLDOWN_MS = 10_000L  // 10 seconds cooldown

    fun shouldLog(gesture: DetectedGesture): Boolean {
        val now = System.currentTimeMillis()
        val lastLoggedTime = recentGestures[gesture.type] ?: 0

        return if (now - lastLoggedTime > COOLDOWN_MS) {
            recentGestures[gesture.type] = now
            true
        } else {
            false
        }
    }
}