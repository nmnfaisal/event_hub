package me.nomanfaisal.eventhub.feature.events.domain

import kotlin.time.Instant
import me.nomanfaisal.eventhub.core.model.DataError

/**
 * What the repository knows about a category.
 *
 * `events` may be empty: that means "loaded, nothing there". A category that was
 * never loaded produces no feed at all.
 */
data class EventFeed(
    val events: List<Event>,
    val cachedAt: Instant,
    /** Non-null when the latest refresh failed and `events` come from the cache. */
    val refreshError: DataError?,
    /** True while a network fetch for this category is in flight. */
    val isRefreshing: Boolean = false,
)

data class HomeFeed(
    val trending: List<Event>,
    val comingUp: List<Event>,
    val cachedAt: Instant,
    val refreshError: DataError?,
    val isRefreshing: Boolean = false,
) {
    val isEmpty: Boolean get() = trending.isEmpty() && comingUp.isEmpty()
}
