# ADR-003: Data Source and Offline-First Caching

**Status:** Accepted
**Date:** 2026-10-12
**Context:** The Home screen (change `events-home`) needs a list of upcoming events
that stays usable offline. Module boundaries are fixed by ADR-001; this ADR decides
where the data comes from and how it is cached.

---

## Decision

### 1. Ticketmaster Discovery API v2 is the only data source

`GET /discovery/v2/events.json`, authenticated with an `apikey` query parameter that is
read from `local.properties` (`TICKETMASTER_API_KEY`) at build time and generated into
`core:network`. The key ships inside the app binary; this is accepted for a practice
project and is not a secret-management design.

The API service, DTOs and mapper live in `feature:events:data` (ADR-001 Rule 3).
`core:network` knows only about HTTP: client, API-key plugin, error mapping.

### 2. The city is fixed to Berlin, DE in v1

Every request sends `city=Berlin&countryCode=DE`. There is no location permission and no
picker. The UI shows a non-interactive "Berlin, DE" pill. A picker is a later change and
would need its own spec.

### 3. Room is the single source of truth (cache-then-network)

The UI reads only from Room. The network writes only to Room. The repository exposes
`Flow<Result<EventFeed, DataError>>` (CLAUDE.md Rule 3).

- **Per-category cache.** Rows are keyed by `(category, eventId)`; a second table holds
  one `fetchedAt` per category. "All" is its own category because its relevance ranking
  differs from any single category, and ranks are stored as returned.
- **Never loaded vs loaded-and-empty.** A category with no `fetchedAt` row has never been
  loaded; the flow emits nothing until the first fetch ends (CLAUDE.md Rule 10). A
  category with a `fetchedAt` row and no events is genuinely empty.
- **Staleness: 15 minutes.** When the row is missing or older than 15 minutes the
  repository fetches in the background while cached data is shown. An explicit user
  refresh always fetches.
- **Past events are filtered on read.** The DAO query is `startsAt >= now`, so an event
  that started after it was cached disappears without a refetch. The request also sends
  `startDateTime=now` so the API does not return past events either.
- **Refresh failure with a cache** surfaces as `Success(feed with refreshError)`, not as
  `Error`; the UI shows the cached data with a banner. `Error` is emitted only when
  nothing is cached.
- Each feature owns its own `@Database` (ADR-001 Rule 5); `core:database` only supplies
  driver and builder helpers.

### 4. "Trending" is derived from relevance order

The API has no featured/promoted field. One request per category is made with
`sort=relevance,desc&size=50`; the use case takes the first five non-cancelled events that
have a usable 16:9 image, at most one per act (by primary attraction id, falling back to
name), and puts the rest in "Coming up" sorted by date. Trade-off: "Coming up" is the
rest of the 50 most relevant upcoming events, not the 50 soonest by date.

### 5. Error mapping happens once, at the repository boundary

HTTP status and exceptions map to `DataError.Network.*` in `core:network`'s `safeCall`;
the UI maps `DataError` to copy. No exception crosses the repository (ViewModels contain
no try/catch).

### 6. Rate-limit budget

Ticketmaster allows 5 requests/second and 5000/day. With one request per category per
15 minutes and six categories the worst case is far below both limits. No client-side
throttling in v1. HTTP 429 maps to `DataError.Network.TooManyRequests`.

---

## Alternatives considered

- **Network-first with in-memory cache.** Rejected: no offline start, and every process
  restart re-spends quota.
- **One shared events table with a category column filter.** Rejected: loses per-request
  relevance order and cannot tell "never loaded" per category.
- **Paging with RemoteMediator.** Rejected for v1: Home shows one page; Ticketmaster
  limits deep paging (`size * page < 1000`) and Search, not Home, is where paging belongs.
- **Separate request for Trending.** Rejected: doubles quota use for a marginal ranking gain.

## Consequences

- The app works offline after the first successful load per category.
- Cached events can be up to 15 minutes old while online and arbitrarily old offline;
  the UI states the fetch time whenever a refresh has failed.
- Search/Event detail (later changes) must decide their own caching; this ADR covers only
  the Home list.
- Unknowns to verify against live responses: caching headers, 429 body shape, the exact
  `classificationName` values for Film and Miscellaneous. They are tracked in
  `feature/events/data/API_NOTES.md`.
