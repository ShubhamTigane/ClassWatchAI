package com.example.classwatchai.core

import android.content.Intent
import android.util.Log
import com.example.classwatchai.App

import com.google.android.datatransport.BuildConfig

class GlobalExceptionHandler (
    private val defaultHandler: Thread.UncaughtExceptionHandler?
) : Thread.UncaughtExceptionHandler {

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        // You can log it to file or Firebase Crashlytics
        Log.e("GlobalExceptionHandler", "Uncaught exception", throwable)
        android.os.Process.killProcess(android.os.Process.myPid())
        System.exit(1)
    }

    companion object { private const val TAG = "GlobalExceptionHandler" }
}