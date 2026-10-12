package me.nomanfaisal.eventhub.core.network

import me.nomanfaisal.eventhub.core.model.DataError

internal actual fun Throwable.platformNetworkError(): DataError.Network? = null
