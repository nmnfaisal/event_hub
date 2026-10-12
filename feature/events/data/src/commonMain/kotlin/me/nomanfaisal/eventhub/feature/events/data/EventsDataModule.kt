package me.nomanfaisal.eventhub.feature.events.data

import me.nomanfaisal.eventhub.core.database.withAppDefaults
import me.nomanfaisal.eventhub.feature.events.data.db.EventsDatabase
import me.nomanfaisal.eventhub.feature.events.data.db.eventsDatabaseBuilder
import me.nomanfaisal.eventhub.feature.events.domain.EventRepository
import org.koin.dsl.module

/** Needs `HttpClient` (networkModule) and `DatabasePlatform` (databaseModule). */
val eventsDataModule = module {
    single<EventsDatabase> { eventsDatabaseBuilder(get()).withAppDefaults().build() }
    single { TicketmasterEventsApi(get()) }
    single<EventRepository> { EventRepositoryImpl(get(), get<EventsDatabase>().eventsDao()) }
}
