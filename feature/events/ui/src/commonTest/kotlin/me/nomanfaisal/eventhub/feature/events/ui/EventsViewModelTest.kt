package me.nomanfaisal.eventhub.feature.events.ui

import app.cash.turbine.test
import kotlin.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.TimeZone
import me.nomanfaisal.eventhub.core.model.DataError
import me.nomanfaisal.eventhub.core.model.EventId
import me.nomanfaisal.eventhub.core.model.Result
import me.nomanfaisal.eventhub.feature.events.domain.Event
import me.nomanfaisal.eventhub.feature.events.domain.EventCategory
import me.nomanfaisal.eventhub.feature.events.domain.EventFeed
import me.nomanfaisal.eventhub.feature.events.domain.EventStatus
import me.nomanfaisal.eventhub.feature.events.domain.GetEventsUseCase
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class EventsViewModelTest {
    private val cachedAt = Instant.parse("2026-10-12T12:32:00Z")
    private val repo = FakeEventRepository()

    @BeforeTest fun setUp() = Dispatchers.setMain(StandardTestDispatcher())
    @AfterTest fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() = EventsViewModel(GetEventsUseCase(repo), TimeZone.of("Europe/Berlin"))

    private fun event(id: String, rank: Int = 0) = Event(
        id = EventId(id), name = id, startsAt = Instant.parse("2026-10-20T18:00:00Z"), timeZoneId = "Europe/Berlin",
        hasSpecificTime = true, venueName = "Venue", heroImageUrl = "hero-$id", thumbnailUrl = null,
        status = EventStatus.OnSale, attractionKey = id, relevanceRank = rank, priceFrom = null,
    )

    private fun feed(vararg events: Event, error: DataError? = null, refreshing: Boolean = false) =
        Result.Success(EventFeed(events.toList(), cachedAt, error, refreshing))

    @Test
    fun neverLoadedCategoryShowsLoadingNotEmpty() = runTest {
        val vm = viewModel()
        vm.state.test {
            assertEquals(HomeBody.Loading, awaitItem().body)
            advanceUntilIdle()
            expectNoEvents()
        }
    }

    @Test
    fun loadedContentReplacesLoading() = runTest {
        repo.set(EventCategory.All, feed(event("a")))
        val vm = viewModel()
        vm.state.test {
            assertEquals(HomeBody.Loading, awaitItem().body)
            val content = assertIs<HomeBody.Content>(awaitItem().body)
            assertEquals(listOf("a"), content.trending.map { it.id.value })
            assertEquals(null, content.banner)
        }
    }

    @Test
    fun switchingToAnUnloadedCategoryShowsLoadingNotPreviousContent() = runTest {
        repo.set(EventCategory.Music, feed(event("m")))
        val vm = viewModel()
        vm.state.test {
            awaitItem() // initial Loading
            vm.onAction(HomeAction.SelectCategory(EventCategory.Music))
            advanceUntilIdle()
            assertIs<HomeBody.Content>(expectMostRecentItem().body)

            vm.onAction(HomeAction.SelectCategory(EventCategory.Film)) // never loaded
            advanceUntilIdle()
            val last = expectMostRecentItem()
            assertEquals(EventCategory.Film, last.selectedCategory)
            assertEquals(HomeBody.Loading, last.body)
        }
    }

    @Test
    fun switchingToACachedCategoryNeverShowsTheSkeleton() = runTest {
        repo.set(EventCategory.Music, feed(event("m")))
        repo.set(EventCategory.Sports, feed(event("s")))
        val vm = viewModel()
        vm.state.test {
            awaitItem() // initial Loading, before anything is collected
            vm.onAction(HomeAction.SelectCategory(EventCategory.Music))
            advanceUntilIdle()
            val afterMusic = cancelAndConsumeRemainingEvents().filterIsInstance<app.cash.turbine.Event.Item<HomeUiState>>()
            assertTrue(afterMusic.none { it.value.body == HomeBody.Loading }, "cached Music must not flash Loading")
        }
        vm.state.test {
            awaitItem() // current state (Music content)
            vm.onAction(HomeAction.SelectCategory(EventCategory.Sports))
            advanceUntilIdle()
            val items = cancelAndConsumeRemainingEvents().filterIsInstance<app.cash.turbine.Event.Item<HomeUiState>>()
            assertTrue(items.none { it.value.body == HomeBody.Loading }, "cached Sports must not flash Loading")
            assertEquals(EventCategory.Sports, items.last().value.selectedCategory)
        }
    }

    @Test
    fun selectingAChipSelectsItAndDefaultIsAll() = runTest {
        val vm = viewModel()
        vm.state.test {
            assertEquals(0, awaitItem().selectedIndex)
            vm.onAction(HomeAction.SelectCategory(EventCategory.Sports))
            advanceUntilIdle()
            assertEquals(EventCategory.Sports.ordinal, expectMostRecentItem().selectedIndex)
        }
    }

    @Test
    fun emptyAfterSuccessfulLoadIsEmptyState() = runTest {
        repo.set(EventCategory.Film, feed())
        val vm = viewModel()
        vm.state.test {
            awaitItem()
            vm.onAction(HomeAction.SelectCategory(EventCategory.Film))
            advanceUntilIdle()
            assertEquals(HomeBody.Empty(showClearFilters = true), expectMostRecentItem().body)
        }
    }

    @Test
    fun clearFiltersReturnsToAll() = runTest {
        val vm = viewModel()
        vm.state.test {
            awaitItem()
            vm.onAction(HomeAction.SelectCategory(EventCategory.Film))
            advanceUntilIdle()
            vm.onAction(HomeAction.ClearFilters)
            advanceUntilIdle()
            assertEquals(EventCategory.All, expectMostRecentItem().selectedCategory)
        }
    }

    @Test
    fun errorWithNothingCachedShowsErrorAndRetryShowsLoadingAgain() = runTest {
        repo.set(EventCategory.All, Result.Error(DataError.Network.NoInternet))
        val vm = viewModel()
        vm.state.test {
            awaitItem() // Loading
            assertEquals(HomeBody.Error(ErrorKind.Connection), awaitItem().body)

            repo.set(EventCategory.All, feed(event("a")))
            vm.onAction(HomeAction.Retry)
            advanceUntilIdle()
            assertIs<HomeBody.Content>(expectMostRecentItem().body)
            assertTrue(repo.calls.size >= 2, "retry should re-observe the repository")
        }
    }

    @Test
    fun refreshKeepsContentAndForcesRefresh() = runTest {
        repo.set(EventCategory.All, feed(event("a")))
        val vm = viewModel()
        vm.state.test {
            awaitItem()
            assertIs<HomeBody.Content>(awaitItem().body)

            repo.set(EventCategory.All, feed(event("a"), refreshing = true))
            vm.onAction(HomeAction.Refresh)
            advanceUntilIdle()
            val refreshing = assertIs<HomeBody.Content>(expectMostRecentItem().body)
            assertTrue(refreshing.isRefreshing)
            assertEquals(listOf(EventCategory.All to false, EventCategory.All to true), repo.calls.take(2))
        }
    }

    @Test
    fun cachedContentWithFailedRefreshShowsBanner() = runTest {
        repo.set(EventCategory.All, feed(event("a"), error = DataError.Network.NoInternet))
        val vm = viewModel()
        vm.state.test {
            awaitItem()
            val content = assertIs<HomeBody.Content>(awaitItem().body)
            assertEquals(BannerUi(BannerKind.Offline, "14:32"), content.banner)
            assertEquals(false, content.isRefreshing)
        }
    }
}
