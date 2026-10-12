package me.nomanfaisal.eventhub.feature.events.ui

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import me.nomanfaisal.eventhub.core.model.DataError
import me.nomanfaisal.eventhub.core.model.Result
import me.nomanfaisal.eventhub.feature.events.domain.EventCategory
import me.nomanfaisal.eventhub.feature.events.domain.EventFeed
import me.nomanfaisal.eventhub.feature.events.domain.EventRepository

/**
 * Per-category scripted repository. A category with no entry behaves as "never loaded"
 * (emits nothing). Records every call so tests can assert forceRefresh.
 */
class FakeEventRepository : EventRepository {
    val calls = mutableListOf<Pair<EventCategory, Boolean>>()
    private val results = mutableMapOf<EventCategory, MutableStateFlow<Result<EventFeed, DataError>?>>()

    fun set(category: EventCategory, result: Result<EventFeed, DataError>) {
        results.getOrPut(category) { MutableStateFlow(null) }.value = result
    }

    override suspend fun isLoaded(category: EventCategory): Boolean = results[category]?.value is Result.Success

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeEvents(category: EventCategory, forceRefresh: Boolean): Flow<Result<EventFeed, DataError>> {
        calls += category to forceRefresh
        val source = results.getOrPut(category) { MutableStateFlow(null) }
        return source.flatMapLatest { value -> if (value == null) emptyFlow() else kotlinx.coroutines.flow.flowOf(value) }
    }
}
