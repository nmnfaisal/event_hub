package me.nomanfaisal.eventhub.core.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.request.get
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import me.nomanfaisal.eventhub.core.model.DataError
import me.nomanfaisal.eventhub.core.model.Result
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@Serializable
private data class Payload(val name: String? = null)

class NetworkTest {
    private val json = headersOf(HttpHeaders.ContentType, "application/json")

    private fun client(
        captured: MutableList<HttpRequestData> = mutableListOf(),
        respond: io.ktor.client.engine.mock.MockRequestHandleScope.(HttpRequestData) -> io.ktor.client.request.HttpResponseData,
    ): HttpClient =
        createHttpClient(MockEngine { request ->
            captured += request
            respond(request)
        }, apiKey = { "test-key" })

    private suspend fun HttpClient.call(path: String = "events.json") =
        safeCall<Payload> { get(path) }

    @Test
    fun requestsCarryBaseUrlAndApiKey() = runTest {
        val seen = mutableListOf<HttpRequestData>()
        client(seen) { respond("""{"name":"x"}""", HttpStatusCode.OK, json) }.call()

        val url = seen.single().url
        assertEquals("app.ticketmaster.com", url.host)
        assertEquals("/discovery/v2/events.json", url.encodedPath)
        assertEquals("test-key", url.parameters["apikey"])
    }

    @Test
    fun successBodyIsParsed() = runTest {
        val result = client { respond("""{"name":"x"}""", HttpStatusCode.OK, json) }.call()
        assertEquals(Result.Success(Payload("x")), result)
    }

    private suspend fun statusMapsTo(status: HttpStatusCode, expected: DataError.Network) {
        val result = client { respond("{}", status, json) }.call()
        assertEquals(Result.Error(expected), result, "HTTP ${status.value}")
    }

    @Test fun http401IsUnauthorized() = runTest { statusMapsTo(HttpStatusCode.Unauthorized, DataError.Network.Unauthorized) }
    @Test fun http403IsUnauthorized() = runTest { statusMapsTo(HttpStatusCode.Forbidden, DataError.Network.Unauthorized) }
    @Test fun http429IsTooManyRequests() = runTest { statusMapsTo(HttpStatusCode.TooManyRequests, DataError.Network.TooManyRequests) }
    @Test fun http500IsServer() = runTest { statusMapsTo(HttpStatusCode.InternalServerError, DataError.Network.Server) }
    @Test fun http503IsServer() = runTest { statusMapsTo(HttpStatusCode.ServiceUnavailable, DataError.Network.Server) }
    @Test fun http404IsUnknown() = runTest { statusMapsTo(HttpStatusCode.NotFound, DataError.Network.Unknown) }

    @Test
    fun malformedBodyIsSerializationError() = runTest {
        val result = client { respond("not json", HttpStatusCode.OK, json) }.call()
        assertEquals(Result.Error(DataError.Network.Serialization), result)
    }

    @Test
    fun connectionFailureIsNoInternet() = runTest {
        val result = client { throw IOException("unreachable") }.call()
        assertEquals(Result.Error(DataError.Network.NoInternet), result)
    }

    @Test
    fun timeoutIsTimeout() = runTest {
        val result = client { throw HttpRequestTimeoutException("http://x", 1) }.call()
        assertEquals(Result.Error(DataError.Network.Timeout), result)
    }

    @Test
    fun unexpectedExceptionIsUnknown() = runTest {
        val result = client { throw IllegalStateException("boom") }.call()
        assertEquals(Result.Error(DataError.Network.Unknown), result)
    }

    @Test
    fun cancellationIsRethrown() = runTest {
        val client = client { throw CancellationException("cancelled") }
        assertFailsWith<CancellationException> { client.call() }
    }
}
