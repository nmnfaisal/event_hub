package me.nomanfaisal.eventhub.feature.events.domain

import kotlin.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import me.nomanfaisal.eventhub.core.model.DataError
import me.nomanfaisal.eventhub.core.model.EventId
import me.nomanfaisal.eventhub.core.model.Result
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GetEventsUseCaseTest {

    private val cachedAt = Instant.parse("2026-10-12T12:00:00Z")

    private fun event(
        id: String,
        rank: Int,
        startsAt: String = "2026-10-20T18:00:00Z",
        attraction: String = id,
        hero: String? = "https://img/$id.jpg",
        status: EventStatus = EventStatus.OnSale,
    ) = Event(
        id = EventId(id),
        name = id,
        startsAt = Instant.parse(startsAt),
        timeZoneId = "Europe/Berlin",
        hasSpecificTime = true,
        venueName = null,
        heroImageUrl = hero,
        thumbnailUrl = null,
        status = status,
        attractionKey = attraction,
        relevanceRank = rank,
        priceFrom = null,
    )

    private fun useCase(events: List<Event>, refreshError: DataError? = null) =
        GetEventsUseCase(FakeRepository(Result.Success(EventFeed(events, cachedAt, refreshError))))

    private suspend fun GetEventsUseCase.home(): HomeFeed =
        (invoke(EventCategory.All).first() as Result.Success).data

    @Test
    fun repeatedDatesOfOneActAppearOnceInTrending() = runTest {
        val eagles = (0 until 12).map { event("eagles$it", rank = it, attraction = "eagles") }
        val others = (0 until 6).map { event("act$it", rank = 12 + it) }
        val feed = useCase(eagles + others).home()

        assertEquals(5, feed.trending.size)
        assertEquals(1, feed.trending.count { it.attractionKey == "eagles" })
        assertEquals(listOf("eagles0", "act0", "act1", "act2", "act3"), feed.trending.map { it.id.value })
        // The other 11 Eagles dates are still listed under Coming up.
        assertEquals(11, feed.comingUp.count { it.attractionKey == "eagles" })
    }

    @Test
    fun fewerThanFiveQualifyShowsOnlyThose() = runTest {
        val feed = useCase(
            listOf(
                event("a", 0), event("b", 1),
                event("c", 2, hero = null), event("d", 3, hero = null), event("e", 4, hero = null),
            ),
        ).home()

        assertEquals(listOf("a", "b"), feed.trending.map { it.id.value })
        assertEquals(listOf("c", "d", "e"), feed.comingUp.map { it.id.value }.sorted())
    }

    @Test
    fun noHeroImagesMeansNoTrendingSection() = runTest {
        val feed = useCase(listOf(event("a", 0, hero = null), event("b", 1, hero = null))).home()

        assertTrue(feed.trending.isEmpty())
        assertEquals(2, feed.comingUp.size)
    }

    @Test
    fun comingUpIsOrderedByStartAndExcludesTrendingAndCancelled() = runTest {
        val feed = useCase(
            listOf(
                event("A", 0, startsAt = "2026-10-30T18:00:00Z"),
                event("B", 6, startsAt = "2026-10-14T18:00:00Z", hero = null),
                event("C", 7, startsAt = "2026-10-13T18:00:00Z", hero = null),
                event("D", 8, startsAt = "2026-10-12T18:00:00Z", hero = null, status = EventStatus.Cancelled),
            ),
        ).home()

        assertEquals(listOf("A"), feed.trending.map { it.id.value })
        assertEquals(listOf("C", "B"), feed.comingUp.map { it.id.value })
    }

    @Test
    fun cancelledEventsNeverBecomeTrending() = runTest {
        val feed = useCase(listOf(event("x", 0, status = EventStatus.Cancelled), event("y", 1))).home()

        assertEquals(listOf("y"), feed.trending.map { it.id.value })
    }

    @Test
    fun emptyFeedIsEmptyHomeNotAnError() = runTest {
        val result = GetEventsUseCase(FakeRepository(Result.Success(EventFeed(emptyList(), cachedAt, null))))
            .invoke(EventCategory.All).first()

        assertTrue(result is Result.Success && result.data.isEmpty)
    }

    @Test
    fun errorsAndRefreshErrorsPassThrough() = runTest {
        val error = GetEventsUseCase(FakeRepository(Result.Error(DataError.Network.NoInternet)))
            .invoke(EventCategory.All).first()
        assertEquals(Result.Error(DataError.Network.NoInternet), error)

        val feed = useCase(listOf(event("a", 0)), refreshError = DataError.Network.Timeout).home()
        assertEquals(DataError.Network.Timeout, feed.refreshError)
        assertEquals(cachedAt, feed.cachedAt)
    }

    private class FakeRepository(private val result: Result<EventFeed, DataError>) : EventRepository {
        override fun observeEvents(category: EventCategory, forceRefresh: Boolean): Flow<Result<EventFeed, DataError>> =
            flowOf(result)
    }
}
