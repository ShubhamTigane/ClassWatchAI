package com.example.classwatchai.data.db

import android.content.Context
import androidx.room.Room


object LocalGestureLogger {

    private var database: AppDatabase? = null

    fun init(context: Context) {
        if (database == null) {
            database = Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "gesture_logs_db"
            ).build()
        }
    }

    suspend fun insert(log: GestureLog) {
        database?.gestureLogDao()?.insert(log)
    }

    suspend fun getAllLogs(): List<GestureLog> {
        return database?.gestureLogDao()?.getAllLogs() ?: emptyList()
    }

    suspend fun clearAll() {
        database?.gestureLogDao()?.clearLogs()
    }

}