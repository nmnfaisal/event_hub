package me.nomanfaisal.eventhub

import android.app.Application
import me.nomanfaisal.eventhub.di.initKoin
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import me.nomanfaisal.eventhub.di.appModule

class EventHubApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@EventHubApplication)
            modules(appModule)
        }
    }
}
