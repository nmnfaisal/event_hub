package me.nomanfaisal.eventhub.feature.events.data

import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import me.nomanfaisal.eventhub.feature.events.data.db.EventEntity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class EventsDaoTest : RoomTest() {
    private fun entity(category: String, id: String, startsAt: Long, rank: Int = 0) = EventEntity(
        category = category, eventId = id, name = id, startsAtEpochMs = startsAt, timeZoneId = null,
        hasSpecificTime = true, venueName = null, heroImageUrl = null, thumbnailUrl = null,
        status = "OnSale", attractionKey = id, relevanceRank = rank, priceMinMinor = null, priceCurrency = null,
    )

    @Test
    fun pastEventsAreFilteredOnRead() = runTest {
        val dao = inMemoryEventsDb().eventsDao()
        dao.replaceCategory("Music", listOf(entity("Music", "past", 100), entity("Music", "future", 300, rank = 1)), 50)

        dao.observeSnapshot("Music").test {
            assertEquals(listOf("future"), awaitItem()!!.visibleEvents(nowEpochMs = 200).map { it.eventId })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun resultsAreOrderedByRelevanceRank() = runTest {
        val dao = inMemoryEventsDb().eventsDao()
        dao.replaceCategory("All", listOf(entity("All", "b", 500, rank = 1), entity("All", "a", 900, rank = 0)), 1)
        dao.observeSnapshot("All").test {
            assertEquals(listOf("a", "b"), awaitItem()!!.visibleEvents(0).map { it.eventId })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test // AC-14
    fun replacingOneCategoryLeavesOthersAndTheirFetchTimeUntouched() = runTest {
        val dao = inMemoryEventsDb().eventsDao()
        dao.replaceCategory("Music", listOf(entity("Music", "m1", 500)), fetchedAtEpochMs = 10)
        dao.replaceCategory("Sports", listOf(entity("Sports", "s1", 500)), fetchedAtEpochMs = 20)

        dao.replaceCategory("Sports", listOf(entity("Sports", "s2", 500)), fetchedAtEpochMs = 30)

        assertEquals(10, dao.fetchRow("Music")?.fetchedAtEpochMs)
        assertEquals(30, dao.fetchRow("Sports")?.fetchedAtEpochMs)
        dao.observeSnapshot("Music").test {
            assertEquals(listOf("m1"), awaitItem()!!.events.map { it.eventId })
            cancelAndIgnoreRemainingEvents()
        }
        dao.observeSnapshot("Sports").test {
            assertEquals(listOf("s2"), awaitItem()!!.events.map { it.eventId })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun sameEventCanBeCachedUnderTwoCategories() = runTest {
        val dao = inMemoryEventsDb().eventsDao()
        dao.replaceCategory("All", listOf(entity("All", "x", 500)), 1)
        dao.replaceCategory("Music", listOf(entity("Music", "x", 500)), 1)
        dao.observeSnapshot("All").test { assertEquals(1, awaitItem()!!.events.size); cancelAndIgnoreRemainingEvents() }
    }

    @Test
    fun snapshotIsNullUntilLoadedThenEmptyEventsNotNull() = runTest {
        val dao = inMemoryEventsDb().eventsDao()
        dao.observeSnapshot("Film").test {
            assertNull(awaitItem())
            dao.replaceCategory("Film", emptyList(), 5)
            val loaded = awaitItem()!!
            assertEquals(emptyList(), loaded.events)
            assertEquals(5, loaded.fetch.fetchedAtEpochMs)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun neverLoadedHasNoFetchRowButEmptyLoadDoes() = runTest {
        val dao = inMemoryEventsDb().eventsDao()
        assertNull(dao.fetchRow("Film"))
        dao.replaceCategory("Film", emptyList(), 5)
        assertEquals(5, dao.fetchRow("Film")?.fetchedAtEpochMs)
    }
}
