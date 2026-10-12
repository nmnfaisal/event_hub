package me.nomanfaisal.eventhub.core.model

interface Error

/**
 * Outcome of an operation that can fail. Unlike a nullable or empty list, the two
 * outcomes are distinct types, so `Success(emptyList())` can never be confused with
 * `Error(NoInternet)` and `when` must handle both.
 */
sealed interface Result<out D, out E : Error> {
    data class Success<out D>(val data: D) : Result<D, Nothing>
    data class Error<out E : me.nomanfaisal.eventhub.core.model.Error>(val error: E) : Result<Nothing, E>
}

typealias EmptyResult<E> = Result<Unit, E>

inline fun <D, E : Error, R> Result<D, E>.map(transform: (D) -> R): Result<R, E> =
    when (this) {
        is Result.Success -> Result.Success(transform(data))
        is Result.Error -> this
    }

inline fun <D, E : Error> Result<D, E>.onSuccess(action: (D) -> Unit): Result<D, E> {
    if (this is Result.Success) action(data)
    return this
}

inline fun <D, E : Error> Result<D, E>.onError(action: (E) -> Unit): Result<D, E> {
    if (this is Result.Error) action(error)
    return this
}
