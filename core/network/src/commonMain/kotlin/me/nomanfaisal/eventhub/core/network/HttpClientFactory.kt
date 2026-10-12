package me.nomanfaisal.eventhub.core.network

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.http.URLProtocol
import io.ktor.http.takeFrom
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

private const val BASE_URL = "https://app.ticketmaster.com/discovery/v2/"
private const val API_KEY_PARAM = "apikey"

/**
 * Builds the shared client: base URL, `apikey` query parameter on every request, lenient
 * JSON (unknown keys ignored, nothing coerced) and timeouts.
 *
 * Non-2xx responses are not thrown; [safeCall] maps them. When [engine] is null the engine
 * on the platform classpath is used (OkHttp on Android, Darwin on iOS).
 */
fun createHttpClient(
    engine: HttpClientEngine? = null,
    apiKey: () -> String = ::requireApiKey,
    debugLog: ((String) -> Unit)? = null,
): HttpClient {
    val configure: HttpClientConfig<*>.() -> Unit = {
        expectSuccess = false
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
        install(HttpTimeout) {
            connectTimeoutMillis = 10_000
            requestTimeoutMillis = 20_000
            socketTimeoutMillis = 20_000
        }
        defaultRequest {
            url {
                takeFrom(BASE_URL)
                protocol = URLProtocol.HTTPS
                parameters.append(API_KEY_PARAM, apiKey())
            }
        }
        // Installed last so it sees the final URL. Null in release builds: no logging at all.
        debugLog?.let { installDebugLogging(it) }
    }
    return if (engine == null) HttpClient(configure) else HttpClient(engine, configure)
}
