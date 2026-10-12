package me.nomanfaisal.eventhub

import android.app.Application
import android.content.pm.ApplicationInfo
import me.nomanfaisal.eventhub.di.initKoin
import org.koin.android.ext.koin.androidContext

class EventHubApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val isDebug = applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
        initKoin(isDebug) { androidContext(this@EventHubApplication) }
    }
}
