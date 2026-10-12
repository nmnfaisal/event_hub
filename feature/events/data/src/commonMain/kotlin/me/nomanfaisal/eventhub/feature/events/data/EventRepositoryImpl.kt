package me.nomanfaisal.eventhub.feature.events.data

import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import me.nomanfaisal.eventhub.core.model.DataError
import me.nomanfaisal.eventhub.core.model.Result
import me.nomanfaisal.eventhub.feature.events.data.db.CategoryFetchEntity
import me.nomanfaisal.eventhub.feature.events.data.db.EventsDao
import me.nomanfaisal.eventhub.feature.events.data.db.toDomain
import me.nomanfaisal.eventhub.feature.events.data.db.toEntity
import me.nomanfaisal.eventhub.feature.events.domain.EventCategory
import me.nomanfaisal.eventhub.feature.events.domain.EventFeed
import me.nomanfaisal.eventhub.feature.events.domain.EventRepository

private val STALE_AFTER = 15.minutes

/** Room is the single source of truth; the network only writes to it (ADR-003). */
internal class EventRepositoryImpl(
    private val api: TicketmasterEventsApi,
    private val dao: EventsDao,
    private val clock: Clock = Clock.System,
) : EventRepository {

    override fun observeEvents(
        category: EventCategory,
        forceRefresh: Boolean,
    ): Flow<Result<EventFeed, DataError>> = channelFlow {
        val key = category.name
        val now = clock.now()
        val cached = dao.fetchRow(key)
        val needsFetch = forceRefresh || cached == null || cached.isStale(now)

        val fetchState = MutableStateFlow<FetchState>(if (needsFetch) FetchState.Pending else FetchState.Idle)

        if (needsFetch) {
            launch {
                val outcome = fetchAndStore(category, now)
                if (outcome is Result.Error) {
                    // Nothing cached to show: the failure is the answer.
                    if (cached == null) send(outcome)
                    fetchState.value = FetchState.Failed(outcome.error)
                } else {
                    fetchState.value = FetchState.Stored
                }
            }
        }

        // Emits only once a fetch row exists, so a never-loaded category stays silent.
        combine(dao.observeSnapshot(key).filterNotNull(), fetchState) { snapshot, state ->
            // Room delivers the new snapshot a moment after the write returns, so "refreshing"
            // lasts until the snapshot shows this fetch, not just until the write finished.
            val reflectsFetch = snapshot.fetch.fetchedAtEpochMs >= now.toEpochMilliseconds()
            Result.Success(
                EventFeed(
                    events = snapshot.visibleEvents(now.toEpochMilliseconds()).map { it.toDomain() },
                    cachedAt = Instant.fromEpochMilliseconds(snapshot.fetch.fetchedAtEpochMs),
                    refreshError = (state as? FetchState.Failed)?.error,
                    isRefreshing = state == FetchState.Pending || (state == FetchState.Stored && !reflectsFetch),
                ),
            )
        }.collect { send(it) }
    }

    private sealed interface FetchState {
        data object Idle : FetchState
        data object Pending : FetchState
        data object Stored : FetchState
        data class Failed(val error: DataError) : FetchState
    }

    override suspend fun isLoaded(category: EventCategory): Boolean = dao.fetchRow(category.name) != null

    private suspend fun fetchAndStore(category: EventCategory, now: Instant): Result<Unit, DataError> =
        when (val response = api.fetchEvents(category, now)) {
            is Result.Error -> response
            is Result.Success -> try {
                val entities = response.data.toEvents().map { it.toEntity(category.name) }
                dao.replaceCategory(category.name, entities, now.toEpochMilliseconds())
                Result.Success(Unit)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Result.Error(DataError.Local.Unknown)
            }
        }

    private fun CategoryFetchEntity.isStale(now: Instant): Boolean =
        now - Instant.fromEpochMilliseconds(fetchedAtEpochMs) >= STALE_AFTER
}
