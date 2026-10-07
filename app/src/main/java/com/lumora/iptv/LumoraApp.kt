package com.lumora.iptv

import android.app.Application
import com.lumora.iptv.util.AppLogger
import com.lumora.iptv.util.GlobalCrashHandler

class LumoraApp : Application() {
    override fun onCreate() {
        super.onCreate()
        GlobalCrashHandler.install(this)
        AppLogger.i("LumoraApp", "Lumora IPTV Application Initialized successfully")
    }
}
