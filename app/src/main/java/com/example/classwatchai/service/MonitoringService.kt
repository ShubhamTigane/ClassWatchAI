package com.example.classwatchai.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import com.example.classwatchai.R
import com.example.classwatchai.camera.CameraManager
import com.example.classwatchai.camera.DummyLifecycleOwner
import com.example.classwatchai.data.db.LocalGestureLogger
import com.example.classwatchai.detection.GestureDetector

class MonitoringService : LifecycleService() {

    private lateinit var cameraManager: CameraManager

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created")

        // Initialize local Room database
        LocalGestureLogger.init(applicationContext)

        // Initialize CameraManager for background gesture detection
      //  cameraManager = CameraManager(context = this, null)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        Log.d(TAG, "Service started")

        // Start as foreground service
        startForeground(NOTIFICATION_ID, createNotification())

        // Start camera stream for gesture detection
        try {
            CameraManager.initialize(
                context = applicationContext,
                owner = DummyLifecycleOwner(),
                preview = null
            )
            CameraManager.startCamera()
            // safe (will skip)
            GestureDetector.startMonitoring()          // NEW 🟢
            return START_STICKY

        } catch (e: Exception) {
            Log.e(TAG, "🔴 Failed to start camera", e)
        }


        return START_STICKY
    }


    override fun onDestroy() {
        super.onDestroy()
        GestureDetector.stopMonitoring()
        Log.d(TAG, "Service destroyed")
    }

    private fun createNotification(): Notification {
        val channelId = "classwatch_monitoring"
        val channelName = "ClassWatch Monitoring"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Monitoring Class")
            .setContentText("Detecting student behavior...")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    companion object {
        private const val TAG = "Myres MonitoringService"
        private const val NOTIFICATION_ID = 1
    }
}
