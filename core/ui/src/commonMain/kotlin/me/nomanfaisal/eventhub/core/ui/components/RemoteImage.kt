package me.nomanfaisal.eventhub.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import me.nomanfaisal.eventhub.core.ui.theme.EventHubTheme

/**
 * Image from a URL. A null URL, or a load that fails, leaves a plain colour block;
 * there is never a stock image.
 */
@Composable
fun RemoteImage(
    url: String?,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    contentScale: ContentScale = ContentScale.Crop,
) {
    Box(modifier = modifier.background(EventHubTheme.extras.imagePlaceholder)) {
        if (url != null) {
            AsyncImage(
                model = url,
                contentDescription = contentDescription,
                contentScale = contentScale,
                modifier = Modifier.matchParentSize(),
            )
        }
    }
}
