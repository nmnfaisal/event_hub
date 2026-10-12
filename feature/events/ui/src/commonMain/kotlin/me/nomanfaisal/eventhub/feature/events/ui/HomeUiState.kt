package me.nomanfaisal.eventhub.feature.events.ui

import me.nomanfaisal.eventhub.core.model.EventId
import me.nomanfaisal.eventhub.feature.events.domain.EventCategory

/** Everything Home renders, derived from the repository; never mutated in place. */
data class HomeUiState(
    val selectedCategory: EventCategory,
    val body: HomeBody,
) {
    val chips: List<String> get() = HomeStrings.chips
    val selectedIndex: Int get() = selectedCategory.ordinal
}

sealed interface HomeBody {
    /** The selected category has never been loaded. */
    data object Loading : HomeBody

    data class Content(
        val trending: List<FeaturedItemUi>,
        val comingUp: List<EventRowUi>,
        val banner: BannerUi?,
        val isRefreshing: Boolean,
    ) : HomeBody

    /** Loaded successfully, nothing to show. */
    data class Empty(val showClearFilters: Boolean) : HomeBody

    /** Never loaded and the first load failed. */
    data class Error(val kind: ErrorKind) : HomeBody
}

enum class ErrorKind { Connection, Generic }

enum class BannerKind { Offline, RefreshFailed }

data class BannerUi(val kind: BannerKind, val time: String)

data class FeaturedItemUi(
    val id: EventId,
    val title: String,
    val dateChip: String,
    val subtitle: String?,
    val imageUrl: String?,
)

data class EventRowUi(
    val id: EventId,
    val title: String,
    val dateText: String,
    val venueText: String?,
    val imageUrl: String?,
    val badge: String?,
)

sealed interface HomeAction {
    data class SelectCategory(val category: EventCategory) : HomeAction

    /** Pull to refresh, or Retry on the offline banner: keeps content on screen. */
    data object Refresh : HomeAction

    /** Retry on the error screen: shows Loading again. */
    data object Retry : HomeAction

    data object ClearFilters : HomeAction
}

/** User-facing copy. Matches the designs in .claude/design/. */
object HomeStrings {
    const val APP_TITLE = "EventHub"
    const val LOCATION = "Berlin, DE"
    const val HEADLINE = "What's on"
    const val SEARCH_PLACEHOLDER = "Search artists, venues, events"
    const val TRENDING = "Trending near you"
    const val SEE_ALL = "See all"
    const val COMING_UP = "Coming up"
    const val RETRY = "Retry"
    const val LOADING_DESCRIPTION = "Loading events"
    const val EMPTY_TITLE = "Nothing on for these filters"
    const val EMPTY_MESSAGE = "Try another category."
    const val CLEAR_FILTERS = "Clear filters"
    const val ERROR_TITLE = "Couldn't load events"
    const val ERROR_CONNECTION = "Check your connection and try again."
    const val ERROR_GENERIC = "Something went wrong. Try again."

    val chips: List<String> = listOf("All", "Music", "Sports", "Arts & Theatre", "Film", "Miscellaneous")

    fun bannerText(kind: BannerKind, time: String): String = when (kind) {
        BannerKind.Offline -> "Offline. Showing events from $time."
        BannerKind.RefreshFailed -> "Couldn't refresh. Showing events from $time."
    }

    fun errorMessage(kind: ErrorKind): String = when (kind) {
        ErrorKind.Connection -> ERROR_CONNECTION
        ErrorKind.Generic -> ERROR_GENERIC
    }
}
