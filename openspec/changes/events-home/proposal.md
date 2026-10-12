# Proposal

## Why

EventHub opens on an empty "EventHub" placeholder. People who open the app want
to see what's on in their city tonight and this week without typing anything,
and they open it in places with bad signal (U-Bahn, venues), so the screen has
to stay useful offline. The Home screen is the first feature because every
other screen (Search, Event detail, Saved) is reached from it.

Architectural context is not restated here: module boundaries are in ADR-001,
the data source and offline-first caching strategy in ADR-003 (written as the
first task of this change).

## What Changes

- New Home screen ("What's on") for Berlin, DE: header with location pill,
  tappable search bar, category chips, "Trending near you" carousel,
  "Coming up" list, pull to refresh.
- Events fetched from the Ticketmaster Discovery API (`events.json`) and
  cached on device per category, so Home works offline and on cold start
  without network.
- Five screen states from the designs: loading skeleton, content,
  content with offline banner, empty, error.
- Bottom navigation (Home / Search / Saved / Tickets) with placeholder
  screens for the three tabs not built yet.
- App theme matched to the designs (light theme only).
- New shared building blocks: `Result` and `DataError` (and `Money`) in
  `core:model`; HTTP client and Room driver setup in `core:network` and
  `core:database`; primitive composables in `core:ui`.

## Capabilities

### New Capabilities
- `events-home`: The Home screen's observable behaviour — what events it shows
  for a city and category, how they are split into Trending and Coming up, how
  data is cached and refreshed, and what the user sees when loading, empty,
  offline or failing.

### Modified Capabilities
- None (no existing specs).

## Impact

**Modules touched** (all exist in `settings.gradle.kts`; no new modules):

| Module | Why |
|---|---|
| `core:model` | Add `Result`, `DataError`, `Money` (ADR-001 already places them here) |
| `core:network` | HTTP client, API-key plugin, error mapping — no event knowledge |
| `core:database` | Room driver/builder helpers — no entities |
| `core:ui` | Theme tokens and primitive composables (chips, cards, rows, banners, nav bar, skeleton) |
| `feature:events:domain` | Event model, repository interface, Home feed rules |
| `feature:events:ui` | Home ViewModel, screen, previews |
| `feature:events:data` | Ticketmaster API, DTOs, mapper, Room cache, repository |
| `shared` | Koin assembly, bottom-nav scaffold, Koin init fix for Android |
| `androidApp` | Call shared `initKoin` instead of its own `startKoin` |

`feature:saved:*` and `feature:tickets:*` are untouched; their tabs show a
placeholder hosted by `:shared`.

**Dependencies added** (recorded in ADR-002): kotlinx-datetime, kotlinx-coroutines,
lifecycle-viewmodel-compose (JetBrains), koin-compose-viewmodel, Room +
sqlite-bundled and Coil 3 (both already pinned, now activated); test-only:
ktor-client-mock, kotlinx-coroutines-test, Turbine, kotlinx-io, Compose UI test,
Robolectric.

**External API**: Ticketmaster Discovery v2, about one request per category per 15 minutes
(quota 5 req/s, 5000/day).

**Out of scope**
- Search screen behaviour (search bar and "See all" only navigate to a placeholder Search tab)
- Event detail (tapping an event is a no-op callback)
- Saved and Tickets screens (placeholder tabs)
- Location picker (city fixed to Berlin, DE; pill is not interactive)
- Date filter and price filter
- Pagination beyond the first 50 results per category
- Dark theme
