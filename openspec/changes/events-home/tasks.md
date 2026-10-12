# Tasks

## 1. Decisions and samples

- [x] 1.1 Write `docs/adr/ADR-003-data-source.md` (Ticketmaster as source, Berlin/DE fixed, per-category Room cache, 15-min staleness, past filtered on read, relevance-ranked Trending, rate-limit budget); verify it links ADR-001 and is referenced from proposal.md
- [ ] 1.2 Capture `docs/api-samples/events-empty.json` (e.g. `keyword=zzqxvq&city=Berlin&countryCode=DE`) and `docs/api-samples/error-invalid-key.json` (`apikey=bad`), apikey stripped; verify `grep -ci apikey docs/api-samples/*` returns 0 for both and the empty one has no `_embedded`
- [x] 1.3 Update ADR-002 dependency table with every library added in group 2; verify each new catalog entry appears in the table

## 2. Build setup

- [x] 2.1 Add catalog entries: kotlinx-datetime, kotlinx-coroutines-core, JetBrains lifecycle-viewmodel-compose, koin-compose-viewmodel, ktor-client-mock, kotlinx-coroutines-test, turbine, kotlinx-io, robolectric; verify `./gradlew help` succeeds
- [x] 2.2 Enable Android host tests with Android resources in `eventhub.kmp.library`; verify `./gradlew :core:model:tasks --all` lists `testAndroidHostTest` and `iosSimulatorArm64Test`
- [x] 2.3 Add the commonTest fixture-path generator for `docs/api-samples/`; verify a throwaway test reading `events-berlin-music.json` passes on both targets, then delete it
- [x] 2.4 Apply Room + KSP (`kspAndroid`, `kspIosArm64`, `kspIosSimulatorArm64`) with schema directory in `feature:events:data`, Room runtime + sqlite-bundled in `core:database`; verify `./gradlew :feature:events:data:compileKotlinIosSimulatorArm64` succeeds (bump Room per design risk if not)

## 3. core:model

- [x] 3.1 Add `Result<D, E : Error>`, `EmptyResult`, `map`/`onSuccess`/`onError`; verify unit tests for map and both branches pass on both targets
- [x] 3.2 Add `DataError` (`Network.NoInternet, Timeout, Unauthorized, TooManyRequests, Server, Serialization, Unknown`; `Local.Unknown`) and `Money(amountMinor: Long, currencyCode: String)`; verify `./gradlew :core:model:allTests`

## 4. feature:events:domain

- [x] 4.1 Add `Event`, `EventStatus`, `EventCategory`, `EventFeed`, `HomeFeed` per spec "Reference: domain model", depending only on `core:model`; verify `feature/events/domain/build.gradle.kts` lists only `:core:model`
- [x] 4.2 Add `EventRepository.observeEvents(category, forceRefresh): Flow<Result<EventFeed, DataError>>`; verify it compiles and `Placeholder.kt` is removed
- [x] 4.3 Implement `GetEventsUseCase` (drop cancelled, Trending top-5 by rank with hero image de-duped by attraction, Coming up by start); verify use-case tests for AC-24, AC-25, AC-26, AC-27 pass on both targets

## 5. core:ui (primitives only)

- [ ] 5.1 Add theme tokens (warm off-white background, red accent, dark selected chip, display type) to `EventHubTheme`; verify a preview renders the palette
- [x] 5.2 Add `AppHeader`, `LocationPill`, `SearchBarButton`, `CategoryChipRow`, `SectionHeader`; verify each signature uses only primitives/lambdas and has a `@Preview`
- [ ] 5.3 Add `FeaturedCard`, `EventRow`, `RemoteImage` (Coil, colour-block placeholder/error); verify previews with null and non-null image URLs
- [x] 5.4 Add `SkeletonBox`, `EmptyView`, `ErrorView`, `OfflineBanner`, `BottomNavBar`; verify previews and that `core/ui/build.gradle.kts` has no `:core:model` dependency

## 6. feature:events:ui

- [x] 6.1 Add UI models and `Event` → `FeaturedItemUi`/`EventRowUi` mapping (event-time-zone formatting, venue text, badge, "from €X" only with price); verify mapping tests for AC-13, AC-28, AC-29
- [x] 6.2 Implement `EventsViewModel` with derived state (flatMapLatest/stateIn, no `update`, no try/catch) and actions (SelectCategory, Refresh, Retry, ClearFilters); verify ViewModel tests with `FakeEventRepository` + Turbine for AC-21, AC-22, AC-30 and refresh-keeps-content (AC-18 UI side)
- [ ] 6.3 Build `HomeScreen(state, onAction, onSearchClick, onSeeAllClick, onLocationClick, onEventClick)` and `HomeSkeleton`, with `PullToRefreshBox`, chips, Trending `LazyRow` (peeking cards, snap), Coming up list; verify `@Preview` for all five states with fake data and compare against `.claude/design/`
- [x] 6.4 Add `HomeRoute` (koinViewModel) and `eventsUiModule`; verify Compose UI tests for AC-31 to AC-41 and AC-43 pass via `testAndroidHostTest` and `iosSimulatorArm64Test`

## 7. core:network and core:database

- [x] 7.1 Add `createHttpClient(engine)` (base URL, apikey plugin using `requireApiKey()`, `ignoreUnknownKeys`, timeouts) and per-platform engine in `networkModule`; verify a MockEngine test asserts `apikey` and base URL on outgoing requests
- [x] 7.2 Add `safeCall` mapping per the spec error table, rethrowing cancellation; verify MockEngine tests for 401, 403, 429, 500, malformed body, connection failure, timeout
- [x] 7.3 Add Room builder helpers (`BundledSQLiteDriver`, `Dispatchers.IO`) and platform hook (Android context, iOS documents dir) in `databaseModule`; verify `core:database` has no entity or feature imports

## 8. feature:events:data

- [x] 8.1 Add DTOs with every field nullable and defaulting to `null` (no empty-string or empty-list defaults), including nullable `_embedded`; verify contract tests deserialize `events-us-unfiltered.json`, `events-berlin-music.json`, `events-empty.json` without error
- [x] 8.2 Implement the mapper that filters invalid entries (missing id/name, undatable) and applies field fallbacks; verify mapper tests for AC-3 to AC-12 against real fixtures and minimal edited variants
- [x] 8.3 Implement `TicketmasterEventsApi` (internal); verify MockEngine tests for AC-1 and AC-2 request parameters
- [x] 8.4 Add `EventEntity`, `CategoryFetchEntity`, `EventsDao`, `EventsDatabase` (`@ConstructedBy`); verify in-memory Room DAO tests for past filtering and per-category replace on both targets
- [x] 8.5 Implement `EventRepositoryImpl` (internal, injected `Clock`) per design D7, and `eventsDataModule` binding `EventRepository`; verify repository tests for AC-5, AC-14, AC-16 to AC-20, AC-23 and never-loaded failure → `Error`
- [x] 8.6 Write `feature/events/data/API_NOTES.md` (pagination limit, error contract `fault` vs `errors[]`, rate limits + headers, caching headers, unstable fields: `priceRanges`, venue `name`, `cancelled`/`canceled`, ratio-less images, unverified Film/Miscellaneous values); verify every open question has an owner/next step

## 9. shared and app wiring

- [x] 9.1 Change `initKoin(appDeclaration)` and make `EventHubApplication` use it with `androidContext`; register network, database, events data/ui modules and `GetEventsUseCase`; verify a Koin `verify()` test passes
- [ ] 9.2 Replace `App()` with the bottom-nav scaffold (Home, Search/Saved/Tickets placeholders) and wire Home callbacks; verify AC-39, AC-40, AC-42 manually on Android
- [ ] 9.3 Run on Android emulator and iOS simulator with a real key; verify chips, Trending, Coming up, pull to refresh, airplane-mode banner (AC-33) and cold start offline (AC-15)

## 10. Integration checks

- [x] 10.1 Run `./gradlew :core:model:allTests :feature:events:domain:allTests :feature:events:ui:allTests :feature:events:data:allTests :core:network:allTests`; verify all pass on Android host and iOS simulator
- [ ] 10.2 Run the code-reviewer agent against the change; verify no CLAUDE.md rule violations remain open

## Workflow follow-up

- Archive the change with `/opsx:archive` after review.
- Remove `feature:events:*` `Placeholder.kt` files if any remain.
