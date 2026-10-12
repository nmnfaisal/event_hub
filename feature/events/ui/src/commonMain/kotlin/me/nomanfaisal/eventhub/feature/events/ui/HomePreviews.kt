package me.nomanfaisal.eventhub.feature.events.ui

import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import me.nomanfaisal.eventhub.core.model.EventId
import me.nomanfaisal.eventhub.core.ui.theme.EventHubTheme
import me.nomanfaisal.eventhub.feature.events.domain.EventCategory
import org.jetbrains.compose.ui.tooling.preview.Preview

internal object PreviewData {
    val trending = listOf(
        FeaturedItemUi(EventId("1"), "Velvet Static: Autumn Tour", "Fri 16 Oct", "Halle Ost · from €39", null),
        FeaturedItemUi(EventId("2"), "Spree Jazz Sessions", "Sat 17 Oct", "Kellerbühne", null),
    )
    val comingUp = listOf(
        EventRowUi(EventId("3"), "Film Scores Live", "Tue 13 Oct · 19:30", "Konzertsaal Mitte", null, null),
        EventRowUi(EventId("4"), "Comedy Kitchen Open Mic", "Wed 14 Oct · 20:00", "Lachkeller", null, "Postponed"),
    )
    val content = HomeBody.Content(trending, comingUp, banner = null, isRefreshing = false)
}

@Composable
private fun Preview(body: HomeBody, category: EventCategory = EventCategory.All) {
    EventHubTheme {
        HomeScreen(
            state = HomeUiState(category, body),
            onAction = {}, onSearchClick = {}, onSeeAllClick = {}, onLocationClick = {}, onEventClick = {},
            modifier = Modifier.background(MaterialTheme.colorScheme.background),
        )
    }
}

@Preview @Composable private fun HomeLoadingPreview() = Preview(HomeBody.Loading)

@Preview @Composable private fun HomeContentPreview() = Preview(PreviewData.content)

@Preview
@Composable
private fun HomeOfflinePreview() =
    Preview(PreviewData.content.copy(banner = BannerUi(BannerKind.Offline, "14:32")))

@Preview @Composable private fun HomeEmptyPreview() = Preview(HomeBody.Empty(showClearFilters = true), EventCategory.Film)

@Preview @Composable private fun HomeErrorPreview() = Preview(HomeBody.Error(ErrorKind.Connection))
