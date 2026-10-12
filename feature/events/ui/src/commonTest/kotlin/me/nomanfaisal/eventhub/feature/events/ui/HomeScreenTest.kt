package me.nomanfaisal.eventhub.feature.events.ui

import androidx.compose.foundation.layout.requiredSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import me.nomanfaisal.eventhub.core.model.EventId
import me.nomanfaisal.eventhub.core.ui.theme.EventHubTheme
import me.nomanfaisal.eventhub.feature.events.domain.EventCategory
import kotlin.test.Test
import kotlin.test.assertEquals

/** One test per Home state (AC-31..38) plus the navigation callbacks (AC-39..41, 43). */
@OptIn(ExperimentalTestApi::class)
class HomeScreenTest : UiTest() {

    private class Calls {
        val actions = mutableListOf<HomeAction>()
        var search = 0
        var seeAll = 0
        var location = 0
        val events = mutableListOf<EventId>()
    }

    private fun androidx.compose.ui.test.ComposeUiTest.show(
        body: HomeBody,
        category: EventCategory = EventCategory.All,
        calls: Calls = Calls(),
    ): Calls {
        setContent {
            EventHubTheme {
                HomeScreen(
                    state = HomeUiState(category, body),
                    onAction = { calls.actions += it },
                    onSearchClick = { calls.search++ },
                    onSeeAllClick = { calls.seeAll++ },
                    onLocationClick = { calls.location++ },
                    onEventClick = { calls.events += it },
                    modifier = Modifier.requiredSize(412.dp, 2400.dp),
                )
            }
        }
        return calls
    }

    @Test
    fun loadingShowsSkeletonUnderTheFixedHeader() = runTallUiTest {
        show(HomeBody.Loading)

        onNodeWithText("What's on").assertIsDisplayed()
        onNodeWithText("EventHub").assertIsDisplayed()
        onNodeWithText("Berlin, DE").assertIsDisplayed()
        onNodeWithText("Search artists, venues, events").assertIsDisplayed()
        onNodeWithContentDescription("Loading events").assertIsDisplayed()
        onNodeWithText("Nothing on for these filters").assertDoesNotExist()
        onNodeWithText("Couldn't load events").assertDoesNotExist()
    }

    @Test
    fun contentShowsTrendingAndComingUpWithoutBanner() = runTallUiTest {
        show(PreviewData.content)

        onNodeWithText("Trending near you").assertIsDisplayed()
        onNodeWithText("See all").assertIsDisplayed()
        onNodeWithText("Velvet Static: Autumn Tour").assertIsDisplayed()
        onNodeWithText("Halle Ost · from €39").assertIsDisplayed()
        onNodeWithText("Coming up").assertIsDisplayed()
        onNodeWithText("Film Scores Live").assertIsDisplayed()
        onNodeWithText("Tue 13 Oct · 19:30").assertIsDisplayed()
        onNodeWithText("Postponed").assertIsDisplayed()
        onNodeWithText("Retry").assertDoesNotExist()
        onNodeWithContentDescription("Loading events").assertDoesNotExist()
    }

    @Test
    fun noTrendingHidesWholeTrendingSection() = runTallUiTest {
        show(PreviewData.content.copy(trending = emptyList()))

        onNodeWithText("Trending near you").assertDoesNotExist()
        onNodeWithText("See all").assertDoesNotExist()
        onNodeWithText("Coming up").assertIsDisplayed()
    }

    @Test
    fun offlineBannerShowsFetchTimeAndRetryRefreshes() = runTallUiTest {
        val calls = show(PreviewData.content.copy(banner = BannerUi(BannerKind.Offline, "14:32")))

        onNodeWithText("Offline. Showing events from 14:32.").assertIsDisplayed()
        onNodeWithText("Film Scores Live").assertIsDisplayed()
        onNodeWithText("Retry").performClick()
        assertEquals(listOf<HomeAction>(HomeAction.Refresh), calls.actions)
    }

    @Test
    fun refreshFailedBannerUsesItsOwnWording() = runTallUiTest {
        show(PreviewData.content.copy(banner = BannerUi(BannerKind.RefreshFailed, "14:32")))

        onNodeWithText("Couldn't refresh. Showing events from 14:32.").assertIsDisplayed()
    }

    @Test
    fun emptyOffAllOffersClearFilters() = runTallUiTest {
        val calls = show(HomeBody.Empty(showClearFilters = true), EventCategory.Film)

        onNodeWithText("Nothing on for these filters").assertIsDisplayed()
        onNodeWithText("Try another category.").assertIsDisplayed()
        onNodeWithText("Clear filters").performClick()
        assertEquals(listOf<HomeAction>(HomeAction.ClearFilters), calls.actions)
    }

    @Test
    fun emptyOnAllHasNoClearFiltersButton() = runTallUiTest {
        show(HomeBody.Empty(showClearFilters = false))

        onNodeWithText("Nothing on for these filters").assertIsDisplayed()
        onNodeWithText("Clear filters").assertDoesNotExist()
    }

    @Test
    fun errorWithConnectionCauseShowsConnectionCopyAndRetry() = runTallUiTest {
        val calls = show(HomeBody.Error(ErrorKind.Connection))

        onNodeWithText("Couldn't load events").assertIsDisplayed()
        onNodeWithText("Check your connection and try again.").assertIsDisplayed()
        onNodeWithText("Retry").performClick()
        assertEquals(listOf<HomeAction>(HomeAction.Retry), calls.actions)
    }

    @Test
    fun errorWithOtherCauseShowsGenericCopy() = runTallUiTest {
        show(HomeBody.Error(ErrorKind.Generic))

        onNodeWithText("Something went wrong. Try again.").assertIsDisplayed()
    }

    @Test
    fun searchBarSeeAllAndLocationReportClicksOnly() = runTallUiTest {
        val calls = show(PreviewData.content)

        onNodeWithText("Search artists, venues, events").performClick()
        onNodeWithText("See all").performClick()
        onNodeWithText("Berlin, DE").performClick()

        assertEquals(1, calls.search)
        assertEquals(1, calls.seeAll)
        assertEquals(1, calls.location)
        assertEquals(emptyList(), calls.actions)
    }

    @Test
    fun tappingAChipSelectsThatCategory() = runTallUiTest {
        val calls = show(PreviewData.content)

        onNodeWithText("Sports").performClick()

        assertEquals(listOf<HomeAction>(HomeAction.SelectCategory(EventCategory.Sports)), calls.actions)
    }

    @Test
    fun tappingAnEventReportsItsId() = runTallUiTest {
        val calls = show(PreviewData.content)

        onNodeWithText("Film Scores Live").performClick()
        onNodeWithText("Velvet Static: Autumn Tour").performClick()

        assertEquals(listOf(EventId("3"), EventId("1")), calls.events)
    }
}
