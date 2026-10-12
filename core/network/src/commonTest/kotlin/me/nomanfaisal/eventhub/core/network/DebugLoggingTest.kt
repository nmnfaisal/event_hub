package me.nomanfaisal.eventhub.core.network

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DebugLoggingTest {
    private val json = headersOf(HttpHeaders.ContentType, "application/json")
    private val secret = "s3cr3tK3y123"

    private fun client(log: ((String) -> Unit)?) = createHttpClient(
        MockEngine { respond("""{"page":{"totalElements":1},"items":[{"id":"a"}]}""", HttpStatusCode.OK, json) },
        apiKey = { secret },
        debugLog = log,
    )

    @Test
    fun logsCurlAndPrettyResponseWithKeyRedacted() = runTest {
        val lines = mutableListOf<String>()
        client(lines::add).get("events.json") { url.parameters.append("city", "Berlin") }

        val all = lines.joinToString("\n")
        println(all) // sample output
        assertTrue(lines[0].startsWith("curl -X GET 'https://app.ticketmaster.com/discovery/v2/events.json?"))
        assertTrue("apikey=[REDACTED]" in lines[0])
        assertTrue("city=Berlin" in lines[0])
        assertTrue(lines[1].startsWith("<-- 200 "))
        assertTrue("\n    \"page\"" in lines[1], "body should be pretty-printed")
        assertFalse(secret in all, "api key must never be logged")
    }

    @Test
    fun noLoggerMeansNoOutputAndStillWorks() = runTest {
        val response = client(null).get("events.json")
        assertEquals(HttpStatusCode.OK, response.status)
    }
}
