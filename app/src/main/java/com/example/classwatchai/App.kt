package com.example.classwatchai

import android.app.Application
import com.example.classwatchai.core.GlobalExceptionHandler

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this              // tiny singleton for easy context access

        // Set our handler as the default one.
        val oldHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler(
            GlobalExceptionHandler(oldHandler)
        )
    }

    companion object {
        lateinit var instance: App   // initialized in onCreate
            private set
    }
}