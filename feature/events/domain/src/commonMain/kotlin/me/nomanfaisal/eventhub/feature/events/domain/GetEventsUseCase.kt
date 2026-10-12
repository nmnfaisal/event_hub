package me.nomanfaisal.eventhub.feature.events.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import me.nomanfaisal.eventhub.core.model.DataError
import me.nomanfaisal.eventhub.core.model.Result

/**
 * Splits a category's events into the Home screen's two sections.
 *
 * Trending: first [TRENDING_LIMIT] events in relevance order that are not cancelled and
 * have a hero image, at most one per act. Coming up: every other non-cancelled event by
 * start time.
 */
class GetEventsUseCase(private val repository: EventRepository) {

    operator fun invoke(
        category: EventCategory,
        forceRefresh: Boolean = false,
    ): Flow<Result<HomeFeed, DataError>> =
        repository.observeEvents(category, forceRefresh).map { result ->
            when (result) {
                is Result.Success -> Result.Success(result.data.toHomeFeed())
                is Result.Error -> result
            }
        }

    private fun EventFeed.toHomeFeed(): HomeFeed {
        val visible = events.filter { it.status != EventStatus.Cancelled }
        val trending = visible
            .sortedBy { it.relevanceRank }
            .filter { it.heroImageUrl != null }
            .distinctBy { it.attractionKey }
            .take(TRENDING_LIMIT)
        val trendingIds = trending.map { it.id }.toSet()
        val comingUp = visible
            .filter { it.id !in trendingIds }
            .sortedBy { it.startsAt }
        return HomeFeed(trending, comingUp, cachedAt, refreshError, isRefreshing)
    }

    private companion object {
        const val TRENDING_LIMIT = 5
    }
}
