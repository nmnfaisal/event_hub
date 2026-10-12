package me.nomanfaisal.eventhub.core.network

import io.ktor.client.HttpClient
import org.koin.core.module.Module
import org.koin.dsl.module

/** [isDebug] turns on curl-style request/response logging; release builds pass false. */
fun networkModule(isDebug: Boolean): Module = module {
    single<HttpClient> { createHttpClient(debugLog = if (isDebug) ::println else null) }
}
