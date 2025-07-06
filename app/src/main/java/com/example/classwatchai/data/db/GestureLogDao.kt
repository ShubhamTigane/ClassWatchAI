package com.example.classwatchai.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface GestureLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: GestureLog)

    @Query("SELECT * FROM gesture_logs ORDER BY timestamp DESC")
    suspend fun getAllLogs(): List<GestureLog>


    @Query("DELETE FROM gesture_logs")
    suspend fun clearLogs()


}
