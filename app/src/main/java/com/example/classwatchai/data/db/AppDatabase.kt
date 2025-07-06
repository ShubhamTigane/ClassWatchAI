package com.example.classwatchai.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [GestureLog::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun gestureLogDao(): GestureLogDao
}
