package me.nomanfaisal.eventhub.feature.events.data.db

import androidx.room.Room
import androidx.room.RoomDatabase
import me.nomanfaisal.eventhub.core.database.DatabasePlatform

internal actual fun eventsDatabaseBuilder(platform: DatabasePlatform): RoomDatabase.Builder<EventsDatabase> =
    Room.databaseBuilder<EventsDatabase>(platform.filePath(EVENTS_DATABASE_NAME))
