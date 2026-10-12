package me.nomanfaisal.eventhub.core.network

import io.ktor.client.engine.darwin.DarwinHttpRequestException
import me.nomanfaisal.eventhub.core.model.DataError

private const val NS_URL_ERROR_TIMED_OUT = -1001L

internal actual fun Throwable.platformNetworkError(): DataError.Network? {
    if (this !is DarwinHttpRequestException) return null
    return if (origin.code == NS_URL_ERROR_TIMED_OUT) DataError.Network.Timeout else DataError.Network.NoInternet
}
