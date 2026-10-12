package me.nomanfaisal.eventhub.core.ui.components

import androidx.compose.runtime.Composable
import coil3.compose.setSingletonImageLoaderFactory
import coil3.ImageLoader
import coil3.network.ktor3.KtorNetworkFetcherFactory

/** Lets Coil load http(s) image URLs on every platform. Call once near the root of the app. */
@Composable
fun SetUpImageLoading() {
    setSingletonImageLoaderFactory { context ->
        ImageLoader.Builder(context)
            .components { add(KtorNetworkFetcherFactory()) }
            .build()
    }
}
