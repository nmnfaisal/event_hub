package me.nomanfaisal.eventhub.core.model

sealed interface DataError : Error {
    enum class Network : DataError {
        NoInternet,
        Timeout,
        Unauthorized,
        TooManyRequests,
        Server,
        Serialization,
        Unknown,
    }

    enum class Local : DataError {
        Unknown,
    }
}
