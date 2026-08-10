package com.myexpense.tracker

import android.app.Application
import com.myexpense.tracker.util.CrashHandler

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        CrashHandler.install(this)
    }
}
