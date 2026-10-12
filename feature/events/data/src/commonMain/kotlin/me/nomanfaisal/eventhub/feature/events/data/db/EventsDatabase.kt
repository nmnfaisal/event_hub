package me.nomanfaisal.eventhub.feature.events.data.db

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor

@Database(entities = [EventEntity::class, CategoryFetchEntity::class], version = 1, exportSchema = true)
@ConstructedBy(EventsDatabaseConstructor::class)
internal abstract class EventsDatabase : RoomDatabase() {
    abstract fun eventsDao(): EventsDao
}

// Room's KSP processor generates the actual for each target.
@Suppress("KotlinNoActualForExpect", "NO_ACTUAL_FOR_EXPECT")
internal expect object EventsDatabaseConstructor : RoomDatabaseConstructor<EventsDatabase> {
    override fun initialize(): EventsDatabase
}

internal const val EVENTS_DATABASE_NAME = "events.db"
