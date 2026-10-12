package me.nomanfaisal.eventhub.core.network

import io.ktor.client.call.body
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.statement.HttpResponse
import io.ktor.serialization.ContentConvertException
import kotlinx.coroutines.CancellationException
import kotlinx.io.IOException
import kotlinx.serialization.SerializationException
import me.nomanfaisal.eventhub.core.model.DataError
import me.nomanfaisal.eventhub.core.model.Result

/**
 * Runs [execute] and maps the outcome to a [Result]. HTTP failures and exceptions become
 * [DataError.Network]; cancellation is always rethrown.
 */
suspend inline fun <reified T> safeCall(execute: suspend () -> HttpResponse): Result<T, DataError.Network> {
    val response = try {
        execute()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        return Result.Error(e.toNetworkError())
    }
    return try {
        when (val failure = response.status.value.toHttpError()) {
            null -> Result.Success(response.body<T>())
            else -> Result.Error(failure)
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        Result.Error(e.toNetworkError())
    }
}

@PublishedApi
internal fun Int.toHttpError(): DataError.Network? = when (this) {
    in 200..299 -> null
    401, 403 -> DataError.Network.Unauthorized
    429 -> DataError.Network.TooManyRequests
    in 500..599 -> DataError.Network.Server
    else -> DataError.Network.Unknown
}

@PublishedApi
internal fun Throwable.toNetworkError(): DataError.Network = when (this) {
    is HttpRequestTimeoutException, is ConnectTimeoutException, is SocketTimeoutException ->
        DataError.Network.Timeout
    is SerializationException, is ContentConvertException -> DataError.Network.Serialization
    else -> platformNetworkError() ?: if (this is IOException) DataError.Network.NoInternet else DataError.Network.Unknown
}

/** Platform-specific failures that are not `IOException`s (Darwin reports `NSError`s). */
internal expect fun Throwable.platformNetworkError(): DataError.Network?
