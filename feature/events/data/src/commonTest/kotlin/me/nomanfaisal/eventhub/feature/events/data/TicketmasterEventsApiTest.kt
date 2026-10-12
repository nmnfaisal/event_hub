package me.nomanfaisal.eventhub.feature.events.data

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlin.time.Instant
import kotlinx.coroutines.test.runTest
import me.nomanfaisal.eventhub.core.model.DataError
import me.nomanfaisal.eventhub.core.model.Result
import me.nomanfaisal.eventhub.core.network.createHttpClient
import me.nomanfaisal.eventhub.feature.events.domain.EventCategory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertFalse
import kotlin.test.assertNull

class TicketmasterEventsApiTest {
    private val json = headersOf(HttpHeaders.ContentType, "application/json")
    private val now = Instant.parse("2026-10-12T08:05:09.123Z")

    private fun api(seen: MutableList<HttpRequestData>, body: String = responseOf(), status: HttpStatusCode = HttpStatusCode.OK) =
        TicketmasterEventsApi(
            createHttpClient(MockEngine { request -> seen += request; respond(body, status, json) }, apiKey = { "k" }),
        )

    @Test // AC-1
    fun requestForAllOmitsClassificationName() = runTest {
        val seen = mutableListOf<HttpRequestData>()
        api(seen).fetchEvents(EventCategory.All, now)

        val p = seen.single().url.parameters
        assertEquals("Berlin", p["city"])
        assertEquals("DE", p["countryCode"])
        assertEquals("relevance,desc", p["sort"])
        assertEquals("50", p["size"])
        assertEquals("0", p["page"])
        assertEquals("2026-10-12T08:05:09Z", p["startDateTime"])
        assertEquals("k", p["apikey"])
        assertFalse(p.contains("classificationName"))
    }

    @Test // AC-2
    fun requestForCategorySendsClassificationName() = runTest {
        val seen = mutableListOf<HttpRequestData>()
        api(seen).fetchEvents(EventCategory.ArtsAndTheatre, now)

        val url = seen.single().url
        assertEquals("Arts & Theatre", url.parameters["classificationName"])
        // The ampersand must be encoded or it would split the parameter.
        assertFalse(url.encodedQuery.contains("Arts & Theatre"))
        assertEquals("50", url.parameters["size"])
    }

    @Test
    fun startDateTimeHasNoMilliseconds() {
        assertEquals("2026-01-02T03:04:05Z", Instant.parse("2026-01-02T03:04:05.999Z").toApiDateTime())
    }

    @Test // AC-5 at the API boundary
    fun responseWithoutEmbeddedIsSuccessNotError() = runTest {
        val result = api(mutableListOf(), body = """{"page":{"totalElements":0}}""").fetchEvents(EventCategory.Music, now)
        val success = assertIs<Result.Success<*>>(result)
        assertNull((success.data as me.nomanfaisal.eventhub.feature.events.data.dto.EventsResponseDto).embedded)
    }

    @Test
    fun serverErrorIsMapped() = runTest {
        val result = api(mutableListOf(), body = "{}", status = HttpStatusCode.ServiceUnavailable).fetchEvents(EventCategory.All, now)
        assertEquals(Result.Error(DataError.Network.Server), result)
    }
}
