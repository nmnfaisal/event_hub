# Design

## Context

See proposal.md for the reasons behind this change and specs/events-home/spec.md for the required behaviour.

Current state:
- **Feature modules:** every `feature:events:*` module contains only a `Placeholder.kt`.
- **`core:model`:** has `EventId` and `TicketId` only. `Result`, `DataError` and `Money` don't exist yet.
- **`core:network`:** generates `TICKETMASTER_API_KEY` from `local.properties` and exposes `requireApiKey()`. There is no HttpClient yet.
- **`core:database`:** an empty file. Room is pinned in the version catalog but not applied.
- **`:shared`:** `App()` renders a "EventHub" text and `appModule` is empty.
- **Android startup:** `EventHubApplication` calls `startKoin` itself instead of the shared `initKoin()`.
- **Toolchain (ADR-002):** Kotlin 2.2.20, CMP 1.10.3, KSP 2.2.20-2.0.3, Ktor 3.1.0, Room 2.7.1.

Module boundaries follow ADR-001. Data source and caching follow ADR-003, which is written first.

## Goals / Non-Goals

**Goals:**
- All ten CLAUDE.md rules visible in one feature, end to end.
- One source of truth: the UI only ever reads Room. The network only writes to it.
- Every shared test runs on both Android (Robolectric host test) and iOS (simulator).

**Non-Goals:**
- Paging or infinite scroll. One page of 50 is enough for Home.
- A navigation library. Tabs are a simple selected-index scaffold in `:shared`.
- Background sync or WorkManager.

## Decisions

### D1. Repository contract: `Flow<Result<EventFeed, DataError>>`

```kotlin
// feature:events:domain
interface EventRepository {
    fun observeEvents(category: EventCategory, forceRefresh: Boolean): Flow<Result<EventFeed, DataError>>
}
data class EventFeed(val events: List<Event>, val cachedAt: Instant, val refreshError: DataError?)
```

The flow behaves as follows:
- **Never loaded:** there's no fetch row for the category. The flow emits nothing until the first fetch completes. On failure it emits `Result.Error(e)`.
- **Loaded:** a fetch row exists. The flow emits `Result.Success(EventFeed(...))` from Room, even when `events` is empty (Rule 10).
- **Refresh fails with a cache:** it emits `Success` with `refreshError = e`, so the UI can show "content with banner" without losing data.

Alternatives considered:
- **`Flow<List<Event>>` plus a separate error flow.** Rejected: it breaks Rule 3, and the compiler can't force callers to handle both cases.
- **A `Loading` variant inside `Result`.** Rejected: loading is a UI concern, and the absence of an emission already means never loaded.

`EventId` is the existing value class in `core:model` (Rule 4). The attraction de-duplication key stays a plain `String`, because it's internal to the use case and never crosses a module boundary as an identifier.

### D2. Home rules live in a use case

`GetEventsUseCase(repo)` returns `Flow<Result<HomeFeed, DataError>>`. It applies the spec's Trending and Coming up rules, which are:
- drop cancelled events
- take the top 5 by relevance rank that have a hero image, deduplicated by attraction
- put the rest in Coming up, sorted by `startsAt`

This is real business logic, so a domain-layer use case is justified. It's pure Kotlin and tested without Android.

`GetEventsUseCase` is registered in `:shared`, which keeps `feature:events:domain` free of Koin and dependent only on `core:model`.

### D3. ViewModel state is derived, never mutated (Rule 1)

```kotlin
private val selectedCategory = MutableStateFlow(EventCategory.All)   // input
private val refreshRequests = MutableSharedFlow<Unit>(extraBufferCapacity = 1) // input

val state: StateFlow<HomeUiState> = selectedCategory
    .flatMapLatest { category ->
        refreshRequests.onStart { emit(Unit) }.withIndex()
            .flatMapLatest { (i, _) ->
                getEvents(category, forceRefresh = i > 0)
                    .map { result -> result.toUiState(category) }
                    .onStart { emit(previousOrLoading(category, refreshing = i > 0)) }
            }
    }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState.initial())
```

- The two `Mutable*Flow`s are inputs written by `onAction`, not state. There's no `_state.update {}` and no try/catch: errors arrive as `Result.Error`.
- `previousOrLoading` lets a refresh keep showing the current content with `isRefreshing = true` instead of dropping back to the skeleton. It's implemented with `scan` over the emissions, not mutable fields.
- **`HomeUiState`:** `chips` and `selectedIndex`, plus `body`, which is one of:
  - `Loading`
  - `Content(trending, comingUp, banner: BannerUi?, isRefreshing)`
  - `Empty(showClearFilters)`
  - `Error(messageRes)`
- Content with a banner is `Content` with a non-null `banner`.

**Alternative considered:** an imperative MVI reducer with `MutableStateFlow.update`. Rejected because it breaks Rule 1.

### D4. Primitives-only design system (Rule 5)

`core:ui` gains:
- `EventHubTheme` with colour and type tokens taken from the designs
- `AppHeader`, `LocationPill`, `SearchBarButton`, `CategoryChipRow`
- `FeaturedCard(title, dateChip, subtitle, imageUrl: String?)`
- `EventRow(title, dateText, venueText: String?, imageUrl: String?, badge: String?)`
- `SectionHeader(title, actionLabel?, onAction?)`, `SkeletonBox`, `EmptyView`, `ErrorView`, `OfflineBanner`
- `BottomNavBar(items, selectedIndex, onSelect)` and `RemoteImage`

All of them take only `String`, `Boolean`, `Int` and lambdas.

`feature:events:ui` maps `Event` to `FeaturedItemUi` and `EventRowUi`. That mapping does the date formatting in the event's time zone, picks the venue text, and sets the badge text. `core:ui` never depends on `core:model` (ADR-001 Rule 7).

### D5. Room cache layout (per-feature database, ADR-001 Rule 5)

- **`EventEntity`:**
  - Primary key: `(category, eventId)`. The same event can appear under All and under Music.
  - Columns: `relevanceRank`, `startsAtEpochMs`, display fields, nullable image URLs, `priceMinMinor?`, `priceCurrency?`.
- **`CategoryFetchEntity(category PK, fetchedAtEpochMs)`:** whether this row exists is what separates "never loaded" from "loaded and empty".
- **`EventsDao`:**
  - `observe(category, nowMs)`: `WHERE category = ? AND startsAtEpochMs >= ? ORDER BY relevanceRank`. Past events are filtered on read.
  - `observeFetch(category)`.
  - `@Transaction replaceCategory(category, events, fetchedAt)`.
- **`EventsDatabase`:** `@ConstructedBy(EventsDatabaseConstructor)`, `BundledSQLiteDriver`, and `Dispatchers.IO` via `core:database` helpers.

**Alternative considered:** a single table without a category key, filtering by classification on read. Rejected: "All" returns a different relevance ranking than any single category, and ranks must be kept per request.

### D6. Network and error mapping

- **`core:network`** provides:
  - `createHttpClient(engine)`: base URL, a `Json { ignoreUnknownKeys = true }` with no special coercion, an API-key plugin, and timeouts.
  - `safeCall {}`, returning `Result<T, DataError.Network>`, as in the spec's error table. It rethrows `CancellationException`.
- **`TicketmasterEventsApi`** lives in `feature:events:data` and is `internal` (Rule 9).
- **DTOs:** every field is nullable and defaults to `null`; lists default to `null`, not `emptyList()` (Rule 2). `EventsResponseDto.embedded` is nullable so that the no-results response works.
- **The mapper:**
  - returns `List<Event>`, dropping invalid entries
  - converts dates with kotlinx-datetime only in `:data`; domain uses `kotlin.time.Instant`
  - stores `timeZoneId` as a `String`

### D7. Offline-first flow inside the repository

```
observeEvents(category, force) = channelFlow {
  val fetch = dao.fetchRow(category)
  val stale = fetch == null || now - fetch.fetchedAt >= 15.minutes
  val refreshError = MutableStateFlow<DataError?>(null)   // local to this collection
  if (stale || force) launch { fetch(category).onError { refreshError.value = it } }
  combine(dao.observeFetch(category).filterNotNull(), dao.observe(category, now), refreshError) { … }
      .collect { send(Success(it)) }
  // if fetch == null and the fetch failed → send(Error(e)) instead
}
```

The clock is injected (`kotlin.time.Clock`) so tests can control staleness and the past-event cutoff.

### D8. Navigation shell

- `:shared` hosts `Scaffold(bottomBar = BottomNavBar)`, with `rememberSaveable` holding the selected tab.
- Home's `koinViewModel` is scoped to the scaffold, so switching tabs keeps its state (AC-42).
- **Callbacks:** `onSearchClick` and `onSeeAllClick` select the Search tab. `onEventClick(EventId)` is a no-op. `onLocationClick` is a no-op.
- A navigation library is deferred until Event detail needs a back stack.

### D9. Test infrastructure for Android and iOS

- **Android host tests:** turned on in the `eventhub.kmp.library` convention plugin (`withHostTest { isIncludeAndroidResources = true }`), using Robolectric.
- **Fixtures:** a small Gradle task generates a `commonTest` constant holding the absolute path to `docs/api-samples/`, and tests read the files with kotlinx-io. The 400 KB file is too large for a Kotlin string constant, and the iOS simulator can read host paths.
- **In-memory Room:** an `expect fun inMemoryEventsDb()` test helper. Android uses Robolectric's `ApplicationProvider`; iOS uses `Room.inMemoryDatabaseBuilder`.
- **Compose tests:** `runComposeUiTest` in `commonTest`.

### ADRs

- **ADR-003 (data source and caching):** must exist before code. It records D1, D5 and D7, the fixed city, the relevance-ranked Trending rule, and the rate-limit budget.
- **ADR-002:** gets a dependency-table update rather than a new ADR.
- **Navigation:** if a navigation library is introduced later (D8), it needs its own ADR.

## Risks / Trade-offs

- **[Risk] Room 2.7.1 may not work with KSP 2.2.20-2.0.3.** → Mitigation: the first build task checks it. If it fails, bump to the nearest compatible Room and record the change in ADR-002.
- **[Risk] `classificationName` for Film and Miscellaneous has never been sampled.** → Mitigation: an API_NOTES question, plus a manual check when the app first runs. If a value is wrong, that chip shows Empty rather than crashing.
- **[Trade-off] Relevance-ranked "Trending" with a 50-event page** means Coming up holds relevant events rather than literally the next 50 by date. This is accepted for one request per category; ADR-003 records it.
- **[Risk] No captured empty or error responses.** → Mitigation: capturing them is the first data-layer task (AC-5 depends on it).
- **[Trade-off] Robolectric host tests aren't real devices.** → Mitigation: manual run on an Android emulator and the iOS simulator before archive.
- **[Risk] Ticketmaster `_SOURCE` hero images are multi-megabyte originals.** → Mitigation: the hero rule picks the smallest 16:9 image at least 1024px wide, which in the samples is always a sized variant.

## Open Questions

- Rate-limit response body and caching headers (`Cache-Control`, `ETag`) are unknown. They'll be documented in API_NOTES after a live call. Neither changes the behaviour or the tasks.
