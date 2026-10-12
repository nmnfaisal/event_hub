package me.nomanfaisal.eventhub.feature.events.data

import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Instant
import me.nomanfaisal.eventhub.feature.events.data.db.EventsDatabase

/** Lets Android host tests run under Robolectric; a no-op on iOS. */
expect abstract class RoomTest()

/** A fresh in-memory database. Android uses the framework driver, since bundled SQLite has no host JNI. */
internal expect fun inMemoryEventsDb(): EventsDatabase

internal class FakeClock(var current: Instant) : Clock {
    override fun now(): Instant = current
    fun advance(by: Duration) { current += by }
}
