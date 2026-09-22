# EventHub KMP — Architecture Context

When the modularization skill and this file disagree on module structure,
this file takes precedence for this project.

## Why This Project Exists

I recently failed a take-home assessment for a Berlin fintech company. They gave
detailed feedback on 11 architectural shortcomings. This project exists to practice
and internalize every one of those patterns. Every architectural decision should
reflect the lessons below.

## Module Structure (Lackner's per-feature vertical slicing)

Instead of shared root-level :domain and :data modules, each feature has its OWN
domain, data, and presentation modules:

:core:domain — shared Result type, error types, value classes (pure Kotlin, no Android)
:core:designsystem — theme, reusable composables (primitives only, never domain types)
:core:data — HttpClient, DataStore instances (ZERO domain knowledge)

:feature:events:domain — Event model, EventRepository interface (pure Kotlin)
:feature:events:presentation — ViewModel, Screen, MVI state
:feature:events:data — API service, DTOs, mapper, repo impl (all internal)


Not every feature needs every layer. Only create justified modules.

## 10 Rules (enforce strictly, flag violations before proceeding)

### 1. Reactive Flow, not imperative state
Use flatMapLatest + combine + stateIn in ViewModels.
NO MutableStateFlow with _state.update{}. NO try/catch blocks in ViewModel.
State is DERIVED from combining flows, never imperatively mutated.

### 2. Nullable DTOs, no dummy values
All DTO fields NULLABLE with null defaults. Never default empty strings.
Mapper filters out invalid entries. No dummy values ever.

### 3. Result type for repository returns
Repository returns Flow<Result<D, E>>, not Flow<List<D>>.
Success(emptyList()) != Error(NO_INTERNET). Compiler must enforce both are handled.

### 4. Value classes for all IDs
@JvmInline value class for opaque identifiers: EventId, UserId, TicketId.
Don't over-apply to display strings like names.

### 5. Design system composables take primitives
Composables take String, Boolean, () -> Unit. NEVER domain types.
Presentation layer maps domain models to composable parameters.

### 6. Contract tests for every API
Every API response structure gets a deserialization test.
Critical: empty response = Success(emptyList()), NOT Error.

### 7. Build order: domain -> presentation -> data
Domain first, then presentation (verify with previews and fake data),
then data last (verify by running app). UI before data because UI gives
you something visual to verify immediately.

### 8. Document API design questions
For every API endpoint, document questions in API_NOTES.md about pagination,
error contract, rate limits, caching headers, field stability.

### 9. Core has ZERO domain knowledge
If deleting all feature code doesn't break a core module, it's correct.
Feature-specific API services, DTOs, and mappers live in feature:data,
NOT in core:data or core:network.

### 10. Null vs empty distinction
null = never loaded (show shimmer), emptyList() = API returned nothing
(show "no items"), [data] = normal display. Never use empty list as default
that hides "never loaded" state.

## Tech Stack

- Kotlin Multiplatform with Compose Multiplatform
- Koin for DI (not Hilt — KMP)
- Ktor for networking (not Retrofit — KMP)
- kotlinx.serialization for JSON
- DataStore for local persistence
- Coroutines + Flow for async/reactive
- MVI for state management

## Skills Reference

These skills are installed at `.claude/skills/` and auto-trigger by context.
Do NOT duplicate their content — they provide the detailed conventions.

| Skill | Covers |
|---|---|
| android-dev | Baseline for all Android/KMP work, MVVM/MVI, architecture |
| modularization | Module placement, visibility, `implementation` vs `api`, dependency direction |
| compose | Compose/CMP UI: state, animations, navigation, performance |
| android-data-layer | Repository pattern, error propagation, Room KMP, sealed DataError |
| koin | DI modules, KSP annotations, ViewModel injection, `verify()` |
| kotlin-coroutines | Dispatchers, structured concurrency, scope ownership, cancellation |
| kotlin-flows | Flow/StateFlow/SharedFlow, operators, lifecycle-safe collection |
| kmp-boundaries | expect/actual, platform services, source-set hierarchy |
| kmp-ktor | HttpClient config, engine selection, auth refresh, MockEngine |
| paging | PagingSource, RemoteMediator, LazyPagingItems, offline-first |
| datastore | Preferences vs Typed vs Room, corruption recovery, KMP setup |
| android-testing | Test-first, Compose test dispatching, semantics selectors |
| android-debugging | Logcat, ADB, ANR traces, R8 decoding, memory leaks |
| android-ux | Material Design 3, touch targets, accessibility, M3 audit |
| android-gradle-logic | Convention plugins, composite builds, version catalogs |
| gradle-build-performance | Build scans, configuration cache, kapt to KSP |
| coil-compose | AsyncImage, ImageRequest, placeholder/error, KMP setup |
| android-source-search | AOSP/AndroidX source lookup via Gitiles/GitHub |
| pdf-annotations | PDF annotation editing via PdfRenderer (API 31+/36.1+) |
| rxjava-migration | RxJava to coroutines/flows migration |