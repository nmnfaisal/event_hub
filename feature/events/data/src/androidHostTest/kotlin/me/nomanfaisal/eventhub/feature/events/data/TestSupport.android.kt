package me.nomanfaisal.eventhub.feature.events.data

import androidx.room.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.Dispatchers
import me.nomanfaisal.eventhub.feature.events.data.db.EventsDatabase
import org.junit.runner.RunWith
import org.robolectric.RuntimeEnvironment

@RunWith(AndroidJUnit4::class)
actual abstract class RoomTest actual constructor()

internal actual fun inMemoryEventsDb(): EventsDatabase =
    Room.inMemoryDatabaseBuilder<EventsDatabase>(RuntimeEnvironment.getApplication())
        .setDriver(AndroidSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.Default)
        .build()
