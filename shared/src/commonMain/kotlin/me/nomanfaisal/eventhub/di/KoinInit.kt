package me.nomanfaisal.eventhub.di

import me.nomanfaisal.eventhub.core.database.databaseModule
import me.nomanfaisal.eventhub.core.network.networkModule
import me.nomanfaisal.eventhub.feature.events.data.eventsDataModule
import me.nomanfaisal.eventhub.feature.events.ui.eventsUiModule
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin

/**
 * Platform entry points pass `androidContext(...)` etc. through [appDeclaration].
 * [isDebug] enables network logging; release builds must pass false.
 */
fun initKoin(isDebug: Boolean, appDeclaration: KoinApplication.() -> Unit) {
    startKoin {
        appDeclaration()
        modules(appModule, networkModule(isDebug), databaseModule, eventsDataModule, eventsUiModule)
    }
}
