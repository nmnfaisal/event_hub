package me.nomanfaisal.eventhub.feature.events.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.withIndex
import kotlinx.datetime.TimeZone
import me.nomanfaisal.eventhub.core.model.DataError
import me.nomanfaisal.eventhub.core.model.Result
import me.nomanfaisal.eventhub.feature.events.domain.EventCategory
import me.nomanfaisal.eventhub.feature.events.domain.GetEventsUseCase
import me.nomanfaisal.eventhub.feature.events.domain.HomeFeed

/**
 * Home state is derived: `selectedCategory`, `retryRequests` and `refreshRequests` are
 * inputs, and [state] is a pure function of them and of what the repository emits.
 * There is no mutable UI state and no try/catch; failures arrive as `Result.Error`.
 */
class EventsViewModel(
    private val getEvents: GetEventsUseCase,
    deviceZone: TimeZone,
) : ViewModel() {

    constructor(getEvents: GetEventsUseCase) : this(getEvents, TimeZone.currentSystemDefault())

    private val mapper = EventUiMapper(deviceZone)

    private val selectedCategory = MutableStateFlow(EventCategory.All)
    private val retryRequests = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    private val refreshRequests = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<HomeUiState> =
        combine(selectedCategory, retryRequests.onStart { emit(Unit) }) { category, _ -> category }
            .flatMapLatest { category ->
                refreshRequests
                    .onStart { emit(Unit) }
                    .withIndex()
                    .flatMapLatest { (index, _) ->
                        val userRefresh = index > 0
                        getEvents(category, forceRefresh = userRefresh).map { result ->
                            HomeUiState(category, result.toBody(category, userRefresh))
                        }
                    }
                    .onStart { emit(HomeUiState(category, HomeBody.Loading)) }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
                initialValue = HomeUiState(EventCategory.All, HomeBody.Loading),
            )

    fun onAction(action: HomeAction) {
        when (action) {
            is HomeAction.SelectCategory -> selectedCategory.value = action.category
            HomeAction.ClearFilters -> selectedCategory.value = EventCategory.All
            HomeAction.Refresh -> refreshRequests.tryEmit(Unit)
            HomeAction.Retry -> retryRequests.tryEmit(Unit)
        }
    }

    private fun Result<HomeFeed, DataError>.toBody(
        category: EventCategory,
        userRefresh: Boolean,
    ): HomeBody = when (this) {
        is Result.Success -> mapper.toBody(data, userRefresh, isAllCategory = category == EventCategory.All)
        is Result.Error -> HomeBody.Error(mapper.errorKind(error))
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
