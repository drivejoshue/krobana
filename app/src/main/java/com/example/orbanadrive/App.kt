package com.example.orbanadrive

import android.app.Application
import android.content.pm.ApplicationInfo
import timber.log.Timber

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        if ((applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
            Timber.plant(Timber.DebugTree())
        }
    }
}