package me.nomanfaisal.eventhub.feature.events.data

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import me.nomanfaisal.eventhub.feature.events.data.db.EventsDatabase

actual abstract class RoomTest actual constructor()

internal actual fun inMemoryEventsDb(): EventsDatabase =
    Room.inMemoryDatabaseBuilder<EventsDatabase>()
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.Default)
        .build()
