package me.nomanfaisal.eventhub.feature.events.data

import app.cash.turbine.test
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runTest
import me.nomanfaisal.eventhub.core.model.DataError
import me.nomanfaisal.eventhub.core.model.EventId
import me.nomanfaisal.eventhub.core.model.Result
import me.nomanfaisal.eventhub.core.network.createHttpClient
import me.nomanfaisal.eventhub.feature.events.domain.EventCategory
import me.nomanfaisal.eventhub.feature.events.domain.EventFeed
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class EventRepositoryImplTest : RoomTest() {
    private val json = headersOf(HttpHeaders.ContentType, "application/json")
    private val start = Instant.parse("2026-10-12T18:00:00Z")

    /** A scripted server: [respond] decides each answer and every request is counted. */
    private class Server(var respond: suspend () -> Pair<HttpStatusCode, String>) {
        var requests = 0
        val categories = mutableListOf<String?>()
    }

    private fun event(id: String, startsAt: String) = eventJson(
        id = id, name = id,
        dates = """"dates":{"start":{"dateTime":"$startsAt"},"timezone":"Europe/Berlin","status":{"code":"onsale"}}""",
    )

    private fun repo(server: Server, clock: FakeClock): EventRepositoryImpl {
        val client = createHttpClient(
            MockEngine { request ->
                server.requests++
                server.categories += request.url.parameters["classificationName"]
                val (status, body) = server.respond()
                respond(body, status, json)
            },
            apiKey = { "k" },
        )
        return EventRepositoryImpl(TicketmasterEventsApi(client), inMemoryEventsDb().eventsDao(), clock)
    }

    private fun ok(vararg events: String) = HttpStatusCode.OK to responseOf(*events)
    private val emptyOk = HttpStatusCode.OK to """{"page":{"totalElements":0}}"""

    private suspend fun app.cash.turbine.ReceiveTurbine<Result<EventFeed, DataError>>.nextFeed(): EventFeed =
        assertIs<Result.Success<EventFeed>>(awaitItem()).data

    private suspend fun app.cash.turbine.ReceiveTurbine<Result<EventFeed, DataError>>.settledFeed(): EventFeed {
        var feed = nextFeed()
        while (feed.isRefreshing) feed = nextFeed()
        return feed
    }

    @Test // AC-21, AC-23
    fun neverLoadedEmitsNothingUntilFetchThenSuccess() = runTest {
        val server = Server { ok(event("a", "2026-10-13T18:00:00Z")) }
        val repo = repo(server, FakeClock(start))

        repo.observeEvents(EventCategory.All, forceRefresh = false).test {
            val feed = settledFeed()
            assertEquals(listOf(EventId("a")), feed.events.map { it.id })
            assertNull(feed.refreshError)
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(1, server.requests)
    }

    @Test // AC-5, AC-23
    fun successfulEmptyLoadIsSuccessWithEmptyListNotError() = runTest {
        val repo = repo(Server { emptyOk }, FakeClock(start))
        repo.observeEvents(EventCategory.Film, false).test {
            val feed = settledFeed()
            assertEquals(emptyList(), feed.events)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test // never-loaded failure
    fun neverLoadedFailureIsError() = runTest {
        val repo = repo(Server { HttpStatusCode.InternalServerError to "{}" }, FakeClock(start))
        repo.observeEvents(EventCategory.All, false).test {
            assertEquals(Result.Error(DataError.Network.Server), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test // AC-16
    fun freshCacheMakesNoRequest() = runTest {
        val server = Server { ok(event("a", "2026-10-13T18:00:00Z")) }
        val clock = FakeClock(start)
        val repo = repo(server, clock)
        repo.observeEvents(EventCategory.All, false).test { settledFeed(); cancelAndIgnoreRemainingEvents() }

        clock.advance(10.minutes)
        repo.observeEvents(EventCategory.All, false).test {
            val feed = nextFeed()
            assertEquals(false, feed.isRefreshing)
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(1, server.requests)
    }

    @Test // AC-17
    fun staleCacheShowsCachedThenReplaces() = runTest {
        val server = Server { ok(event("old", "2026-10-13T18:00:00Z")) }
        val clock = FakeClock(start)
        val repo = repo(server, clock)
        repo.observeEvents(EventCategory.All, false).test { settledFeed(); cancelAndIgnoreRemainingEvents() }

        clock.advance(20.minutes)
        val gate = CompletableDeferred<Unit>()
        server.respond = { gate.await(); ok(event("new", "2026-10-14T18:00:00Z")) }
        repo.observeEvents(EventCategory.All, false).test {
            val first = nextFeed()
            assertEquals(listOf(EventId("old")), first.events.map { it.id })
            assertTrue(first.isRefreshing)
            gate.complete(Unit)
            val settled = settledFeed()
            assertEquals(listOf(EventId("new")), settled.events.map { it.id })
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(2, server.requests)
    }

    @Test // AC-18
    fun forceRefreshIgnoresFreshness() = runTest {
        val server = Server { ok(event("a", "2026-10-13T18:00:00Z")) }
        val clock = FakeClock(start)
        val repo = repo(server, clock)
        repo.observeEvents(EventCategory.All, false).test { settledFeed(); cancelAndIgnoreRemainingEvents() }

        clock.advance(1.minutes)
        val gate = CompletableDeferred<Unit>()
        server.respond = { gate.await(); ok(event("a", "2026-10-13T18:00:00Z")) }
        repo.observeEvents(EventCategory.All, forceRefresh = true).test {
            assertTrue(nextFeed().isRefreshing)
            gate.complete(Unit)
            settledFeed()
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(2, server.requests)
    }

    @Test // refresh failure with cache
    fun failedRefreshKeepsCacheAndReportsError() = runTest {
        val server = Server { ok(event("a", "2026-10-13T18:00:00Z")) }
        val clock = FakeClock(start)
        val repo = repo(server, clock)
        repo.observeEvents(EventCategory.All, false).test { settledFeed(); cancelAndIgnoreRemainingEvents() }

        clock.advance(20.minutes)
        server.respond = { HttpStatusCode.Unauthorized to "{}" }
        repo.observeEvents(EventCategory.All, false).test {
            var feed = nextFeed()
            while (feed.refreshError == null) feed = nextFeed()
            assertEquals(DataError.Network.Unauthorized, feed.refreshError)
            assertEquals(listOf(EventId("a")), feed.events.map { it.id })
            assertEquals(start, feed.cachedAt)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test // AC-19
    fun eventThatStartedSinceCachingIsHiddenOffline() = runTest {
        val server = Server { ok(event("soon", "2026-10-12T19:00:00Z"), event("later", "2026-10-13T19:00:00Z")) }
        val clock = FakeClock(start)
        val repo = repo(server, clock)
        repo.observeEvents(EventCategory.All, false).test {
            assertEquals(2, settledFeed().events.size)
            cancelAndIgnoreRemainingEvents()
        }

        clock.current = Instant.parse("2026-10-12T19:05:00Z")
        server.respond = { HttpStatusCode.OK to "{" } // would fail if asked, but cache is 65 minutes old
        repo.observeEvents(EventCategory.All, false).test {
            var feed = nextFeed()
            while (feed.refreshError == null && feed.isRefreshing) feed = nextFeed()
            assertEquals(listOf(EventId("later")), feed.events.map { it.id })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test // AC-20
    fun allPastCacheWithFailedRefreshIsSuccessWithEmptyEvents() = runTest {
        val server = Server { ok(event("a", "2026-10-12T19:00:00Z")) }
        val clock = FakeClock(start)
        val repo = repo(server, clock)
        repo.observeEvents(EventCategory.All, false).test { settledFeed(); cancelAndIgnoreRemainingEvents() }

        clock.current = Instant.parse("2026-10-12T20:00:00Z")
        server.respond = { HttpStatusCode.ServiceUnavailable to "{}" }
        repo.observeEvents(EventCategory.All, false).test {
            var feed = nextFeed()
            while (feed.refreshError == null) feed = nextFeed()
            assertEquals(emptyList(), feed.events)
            assertEquals(DataError.Network.Server, feed.refreshError)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test // AC-14 at repository level
    fun categoriesAreFetchedAndCachedSeparately() = runTest {
        val server = Server { ok(event("a", "2026-10-13T18:00:00Z")) }
        val repo = repo(server, FakeClock(start))
        repo.observeEvents(EventCategory.Music, false).test { settledFeed(); cancelAndIgnoreRemainingEvents() }
        repo.observeEvents(EventCategory.Sports, false).test { settledFeed(); cancelAndIgnoreRemainingEvents() }
        repo.observeEvents(EventCategory.Music, false).test { assertEquals(false, nextFeed().isRefreshing); cancelAndIgnoreRemainingEvents() }

        assertEquals(listOf<String?>("Music", "Sports"), server.categories)
    }
}
