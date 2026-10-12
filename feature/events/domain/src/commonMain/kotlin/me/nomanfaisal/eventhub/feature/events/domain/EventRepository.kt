package me.nomanfaisal.eventhub.feature.events.domain

import kotlinx.coroutines.flow.Flow
import me.nomanfaisal.eventhub.core.model.DataError
import me.nomanfaisal.eventhub.core.model.Result

interface EventRepository {
    /**
     * Cached events for [category], refreshed when stale or when [forceRefresh] is set.
     *
     * Emits nothing while the category has never been loaded and the first fetch is in
     * flight. Emits `Result.Error` only when nothing is cached and the fetch failed; a
     * failed refresh with a cache is `Success` with `EventFeed.refreshError` set.
     */
    fun observeEvents(
        category: EventCategory,
        forceRefresh: Boolean,
    ): Flow<Result<EventFeed, DataError>>

    /**
     * True once [category] has been loaded successfully at least once. Lets the UI show the
     * Loading skeleton only for a category that has never been loaded (Rule 10).
     */
    suspend fun isLoaded(category: EventCategory): Boolean
}
