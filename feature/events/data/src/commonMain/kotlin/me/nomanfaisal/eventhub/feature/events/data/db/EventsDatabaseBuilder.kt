package me.nomanfaisal.eventhub.feature.events.data.db

import androidx.room.RoomDatabase
import me.nomanfaisal.eventhub.core.database.DatabasePlatform

internal expect fun eventsDatabaseBuilder(platform: DatabasePlatform): RoomDatabase.Builder<EventsDatabase>
