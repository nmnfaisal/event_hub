# Spec Delta

## Purpose

The Home screen ("What's on") shows upcoming events in the user's city, grouped into
Trending and Coming up, filterable by category, and keeps working offline from a
per-category on-device cache.

## User Stories

- US-1: As a visitor in Berlin, I open the app and immediately see what's on, without typing.
- US-2: I filter by a category (Music, Sports, Arts & Theatre, Film, Miscellaneous) to see only that kind of event.
- US-3: I see a few standout "Trending near you" events, each from a different act.
- US-4: I see what's "Coming up" soonest, with date, time and venue.
- US-5: I pull down to get the latest events.
- US-6: With no signal, I still see the events I last loaded, and I'm told they may be out of date.
- US-7: I tap the search bar to go and search (the Search screen itself is a separate change).

## Reference: API request

One request per category, against Ticketmaster Discovery v2:

```
GET https://app.ticketmaster.com/discovery/v2/events.json
    ?apikey=<key>
    &city=Berlin
    &countryCode=DE
    &classificationName=<category>        # omitted for "All"
    &sort=relevance,desc
    &size=50
    &page=0
    &startDateTime=<now, UTC, yyyy-MM-ddTHH:mm:ssZ, no milliseconds>
```

| Chip label | `classificationName` |
|---|---|
| All | (omitted) |
| Music | `Music` |
| Sports | `Sports` |
| Arts & Theatre | `Arts & Theatre` |
| Film | `Film` |
| Miscellaneous | `Miscellaneous` |

## Reference: response fields used

Presence measured over the 25 events in `docs/api-samples/` (20 in
`events-us-unfiltered.json`, an unfiltered US response, and 5 in
`events-berlin-music.json`). No empty or error response has been captured yet.

| Field | Present | Used for | When missing |
|---|---|---|---|
| `_embedded` (top level) | 2/2 files | event list | Treated as an empty list (Ticketmaster omits `_embedded` when nothing matches). Not an error. |
| `_embedded.events[].id` | 25/25 | identity, cache key | Event is dropped |
| `.name` | 25/25 | title | Event is dropped (blank counts as missing) |
| `.dates.start.dateTime` (UTC) | 25/25 | start instant, ordering, past filter | Derived from `localDate` + `localTime` in `dates.timezone`; if `localTime` is missing too, the start of `localDate` is used and the time is not shown |
| `.dates.start.localDate` | 25/25 | fallback for start | If `dateTime` and `localDate` are both missing, the event is dropped (cannot be placed in time) |
| `.dates.start.localTime` | 25/25 | fallback for start | See above |
| `.dates.start.noSpecificTime` / `timeTBA` | 25/25 (all false) | hide time | Treated as false |
| `.dates.timezone` | 25/25 | date/time display, fallback conversion | Display falls back to device time zone; fallback conversion drops the event |
| `.dates.status.code` | 25/25 | hide cancelled, badge | Treated as Unknown (shown, no badge). Observed values: `onsale`, `offsale`, `cancelled`, `postponed`. The API docs spell it `canceled`; both spellings mean Cancelled |
| `.images[]` | 25/25 | hero and thumbnail | No image: neutral colour block. Images without `ratio` (2 events, `_SOURCE` originals) are ignored |
| `.images[]` 16:9, non-fallback, width ≥ 1024 | 25/25 | Trending hero (smallest qualifying) | Event is not eligible for Trending (still in Coming up) |
| `.images[]` 4:3 or 3:2, width ≥ 300 | 25/25 | Coming-up thumbnail (smallest qualifying, cropped square) | Falls back to the hero image; then colour block |
| `._embedded.venues[0].name` | 22/25 (missing in 3 of 5 Berlin events) | venue line | `._embedded.venues[0].address.line1`; if also missing, the venue line is hidden |
| `._embedded.venues[0].city.name` | 25/25 | not shown on Home | Ignored |
| `.classifications[]` with `primary = true` | 25/25 (not first in 1 event) | category of the event | First classification; if none, category unknown (event still shown under All) |
| `._embedded.attractions[0].id` | 25/25 | Trending de-duplication | Event name is used as the de-duplication key |
| `.priceRanges[]` (min, currency) | **0/25** | "from €X" on Trending cards | No price shown. Never a placeholder price |

## Reference: domain model

| Concept | Fields |
|---|---|
| Event | id (EventId), name, startsAt (instant), timeZoneId?, hasSpecificTime, venueName?, heroImageUrl?, thumbnailUrl?, status, attractionKey, relevanceRank, priceFrom (Money)? |
| EventStatus | OnSale, OffSale, Cancelled, Postponed, Rescheduled, Unknown |
| EventCategory | All, Music, Sports, ArtsAndTheatre, Film, Miscellaneous |
| EventFeed | events, cachedAt, refreshError (DataError)? |
| HomeFeed | trending (≤ 5 events), comingUp, cachedAt, refreshError? |

`?` means the value may be absent. No field ever holds a dummy value.

## Reference: error mapping

| Cause | DataError | Error screen message | Banner (cache shown) |
|---|---|---|---|
| No connectivity, host unreachable | Network.NoInternet | "Check your connection and try again." | "Offline. Showing events from HH:mm." |
| Request timed out | Network.Timeout | "Check your connection and try again." | "Offline. Showing events from HH:mm." |
| HTTP 401 / 403 | Network.Unauthorized | "Something went wrong. Try again." | "Couldn't refresh. Showing events from HH:mm." |
| HTTP 429 | Network.TooManyRequests | "Something went wrong. Try again." | "Couldn't refresh. Showing events from HH:mm." |
| HTTP 5xx | Network.Server | "Something went wrong. Try again." | "Couldn't refresh. Showing events from HH:mm." |
| Response body not parseable | Network.Serialization | "Something went wrong. Try again." | "Couldn't refresh. Showing events from HH:mm." |
| Anything else | Network.Unknown | "Something went wrong. Try again." | "Couldn't refresh. Showing events from HH:mm." |

The error screen title is always "Couldn't load events". `HH:mm` is the time the
shown data was fetched, in the device's time zone.

## ADDED Requirements

### Requirement: Home requests events for the fixed city and selected category
The system SHALL request events for Berlin, DE using the parameters in "Reference: API request", sending only future events (startDateTime = now) and at most 50 per category.

#### Scenario: AC-1 Request for All
- **WHEN** Home needs fresh data for category All
- **THEN** the request has `city=Berlin`, `countryCode=DE`, `sort=relevance,desc`, `size=50`, `page=0`, a `startDateTime` in `yyyy-MM-ddTHH:mm:ssZ` format, and no `classificationName`

#### Scenario: AC-2 Request for a specific category
- **WHEN** Home needs fresh data for category Arts & Theatre
- **THEN** the request has `classificationName=Arts & Theatre` (URL-encoded) and otherwise the same parameters as AC-1

### Requirement: Response parsing never invents data
The system SHALL turn a response into events using the rules in "Reference: response fields used", dropping entries that cannot be identified or dated and leaving optional values absent rather than filling them with placeholders.

#### Scenario: AC-3 Real Berlin sample parses completely
- **WHEN** `docs/api-samples/events-berlin-music.json` is parsed
- **THEN** 5 events result, in the response order, each with id, name and start instant

#### Scenario: AC-4 Real unfiltered sample parses completely
- **WHEN** `docs/api-samples/events-us-unfiltered.json` is parsed
- **THEN** 20 events result

#### Scenario: AC-5 Response without _embedded is an empty success
- **WHEN** a response has `page.totalElements = 0` and no `_embedded` object
- **THEN** the result is a successful, empty list of events, not an error

#### Scenario: AC-6 Missing venue name falls back to street address
- **WHEN** an event's venue has no `name` but has `address.line1 = "Nollendorfplatz 5"`
- **THEN** the event's venue text is "Nollendorfplatz 5"

#### Scenario: AC-7 Event missing id or name is dropped
- **WHEN** a response contains one event without `id` and one with a blank `name` among valid events
- **THEN** only the valid events result

#### Scenario: AC-8 Event without any date is dropped
- **WHEN** an event has neither `dates.start.dateTime` nor `dates.start.localDate`
- **THEN** that event is dropped

#### Scenario: AC-9 Start derived from local date and time
- **WHEN** an event has no `dateTime`, `localDate = 2026-10-20`, `localTime = 20:00:00`, `timezone = Europe/Berlin`
- **THEN** its start instant is 2026-10-20T18:00:00Z

#### Scenario: AC-10 Both cancelled spellings are recognised
- **WHEN** events have status `cancelled` and `canceled`
- **THEN** both are Cancelled

#### Scenario: AC-11 Images without ratio are ignored and hero is the smallest qualifying 16:9
- **WHEN** the first event of `events-berlin-music.json` is parsed
- **THEN** its hero image is the 1024-wide `TABLET_LANDSCAPE_16_9` URL, and the ratio-less 3222×4000 image is never chosen

#### Scenario: AC-12 Primary classification wins over order
- **WHEN** an event lists `Rock (primary=false)` before `Folk (primary=true)`
- **THEN** the event's classification is taken from Folk

#### Scenario: AC-13 No price is shown when the API has none
- **WHEN** an event has no `priceRanges`
- **THEN** its Trending subtitle is the venue only, with no "from" text

### Requirement: Events are cached per category on the device
The system SHALL store the last successful result for each category separately, with the time it was fetched, and SHALL show cached events for a category without a network request.

#### Scenario: AC-14 Categories do not overwrite each other
- **WHEN** Music and Sports have both been loaded and Sports is refreshed
- **THEN** the cached Music events and their fetch time are unchanged

#### Scenario: AC-15 Cache survives app restart
- **WHEN** events were loaded, the app is killed, and it is reopened with no connectivity
- **THEN** the previously loaded events for the selected category are shown

### Requirement: Cached data is refreshed when older than 15 minutes
The system SHALL request fresh data for a category when its cache is older than 15 minutes or missing, and SHALL NOT request when the cache is younger, unless the user explicitly refreshes.

#### Scenario: AC-16 Fresh cache makes no request
- **WHEN** Home opens on a category fetched 10 minutes ago
- **THEN** cached events are shown and no network request is made

#### Scenario: AC-17 Stale cache is shown, then replaced
- **WHEN** Home opens on a category fetched 20 minutes ago and the request succeeds
- **THEN** cached events are shown immediately (no skeleton) and replaced by the new result when it arrives

#### Scenario: AC-18 Pull to refresh ignores freshness
- **WHEN** the user pulls to refresh on a category fetched 1 minute ago
- **THEN** a request is made, a refresh indicator shows while it runs, and current content stays visible

### Requirement: Past events are never shown
The system SHALL hide any event whose start instant is earlier than the current time, evaluated when cached data is read, not only when it is fetched.

#### Scenario: AC-19 Event that started since caching is hidden
- **WHEN** the cache was filled at 18:00 with an event starting 19:00, and Home is opened at 19:05 with no connectivity
- **THEN** that event is not shown

#### Scenario: AC-20 Cache that is all past counts as empty
- **WHEN** every cached event for the category has started and the refresh fails
- **THEN** Home shows the Empty state for that category

### Requirement: Never loaded is distinct from loaded and empty
The system SHALL show the Loading state only while a category has never been loaded, and the Empty state only after a successful load returned no events (or none remain after filtering).

#### Scenario: AC-21 First open shows skeleton
- **WHEN** a category has never been loaded and its request is in flight
- **THEN** the Loading skeleton is shown, not the Empty state

#### Scenario: AC-22 Switching to an unloaded category shows skeleton
- **WHEN** Music is cached, the user selects Film, which has never been loaded
- **THEN** the Loading skeleton is shown for Film, not Music's events and not Empty

#### Scenario: AC-23 Successful empty load shows Empty
- **WHEN** a category's request succeeds with no events
- **THEN** the Empty state is shown

### Requirement: Trending shows up to five standout events from different acts
The system SHALL fill "Trending near you" with the first events in the API's relevance order that have a hero image and are not cancelled, at most one per act, up to five, and SHALL hide the section when none qualify.

#### Scenario: AC-24 Repeated shows appear once in Trending
- **WHEN** the relevance-ordered events contain 12 dates of "Eagles Live at Sphere" (same attraction) followed by other acts
- **THEN** Trending contains one Eagles event and up to four other acts

#### Scenario: AC-25 Fewer than five qualify
- **WHEN** only two events have a hero image
- **THEN** Trending shows two cards

#### Scenario: AC-26 None qualify
- **WHEN** no event has a hero image
- **THEN** the Trending section (title, See all, carousel) is not shown and Coming up is

### Requirement: Coming up lists the remaining events by start time
The system SHALL list in "Coming up" every non-cancelled event not shown in Trending, ordered by start instant ascending, each with title, date and time in the event's time zone, and venue text when present.

#### Scenario: AC-27 Ordering and exclusion
- **WHEN** the result contains events A (Trending), B (starts Oct 14), C (starts Oct 13), D (cancelled)
- **THEN** Coming up shows C then B, and neither A nor D

#### Scenario: AC-28 Row formatting
- **WHEN** an event starts 2026-10-13T17:30:00Z with time zone Europe/Berlin and venue "Konzertsaal Mitte"
- **THEN** its row shows "Tue 13 Oct · 19:30" and "Konzertsaal Mitte"

#### Scenario: AC-29 Status badge for changed events
- **WHEN** an event's status is Postponed, Rescheduled or OffSale
- **THEN** its row shows a "Postponed", "Rescheduled" or "Off sale" badge, and OnSale/Unknown events show none

### Requirement: Category chips filter Home
The system SHALL show chips All, Music, Sports, Arts & Theatre, Film, Miscellaneous in a horizontally scrollable row with exactly one selected, defaulting to All, and SHALL show that category's data when a chip is selected.

#### Scenario: AC-30 Default and selection
- **WHEN** Home opens for the first time
- **THEN** All is selected; and **WHEN** the user taps Sports **THEN** Sports is selected and Sports events (or Sports loading/empty/error) are shown

### Requirement: Home shows one of five states
The system SHALL always render the header, "What's on" title, search bar and chips, and below them exactly one of: Loading, Content, Content with banner, Empty, Error, as shown in the design states board.

#### Scenario: AC-31 Loading
- **WHEN** the selected category has never been loaded and a request is in flight
- **THEN** a skeleton (one large card block and four row placeholders) is shown

#### Scenario: AC-32 Content
- **WHEN** events are available and the last refresh did not fail
- **THEN** Trending (if any qualify) and Coming up are shown, with no banner

#### Scenario: AC-33 Content with offline banner
- **WHEN** cached events are shown and the latest refresh failed with NoInternet, fetched at 14:32
- **THEN** a banner reads "Offline. Showing events from 14:32." with a Retry action, above Trending and Coming up

#### Scenario: AC-34 Content with refresh-failed banner
- **WHEN** cached events are shown and the latest refresh failed with Server
- **THEN** the banner reads "Couldn't refresh. Showing events from HH:mm." with Retry

#### Scenario: AC-35 Empty
- **WHEN** the selected category (not All) loaded successfully with no events
- **THEN** "Nothing on for these filters", "Try another category." and a "Clear filters" button are shown; tapping it selects All

#### Scenario: AC-36 Empty on All
- **WHEN** All loaded successfully with no events
- **THEN** the Empty title and message are shown without the Clear filters button

#### Scenario: AC-37 Error with nothing cached
- **WHEN** the selected category has never been loaded and its request fails with NoInternet
- **THEN** "Couldn't load events", "Check your connection and try again." and a Retry button are shown; tapping Retry makes a new request and shows Loading

#### Scenario: AC-38 Error message by cause
- **WHEN** a never-loaded category's request fails with Server
- **THEN** the message is "Something went wrong. Try again."

### Requirement: Header and search entry point
The system SHALL show the "EventHub" wordmark, a location pill reading "Berlin, DE", and a search field reading "Search artists, venues, events" that cannot be typed into on Home and opens the Search tab when tapped.

#### Scenario: AC-39 Search bar navigates
- **WHEN** the user taps anywhere on the search field
- **THEN** the Search tab becomes selected and no keyboard opens on Home

#### Scenario: AC-40 See all navigates
- **WHEN** the user taps "See all" next to Trending
- **THEN** the Search tab becomes selected

#### Scenario: AC-41 Location pill is informational
- **WHEN** the user taps the location pill
- **THEN** nothing changes (city selection is out of scope)

### Requirement: Bottom navigation
The system SHALL show a bottom bar with Home, Search, Saved and Tickets, Home selected on launch, and SHALL switch tabs on tap; Search, Saved and Tickets show a placeholder in this change.

#### Scenario: AC-42 Tab switching keeps Home state
- **WHEN** the user selects Music on Home, switches to Saved, then back to Home
- **THEN** Home still shows Music without a new skeleton

### Requirement: Event taps are reported
The system SHALL report the tapped event's id when a Trending card or Coming-up row is tapped, for the app to route to Event detail in a later change.

#### Scenario: AC-43 Tap reports id
- **WHEN** the user taps the Coming-up row for event `Z698xZC2Z1k4o-Aap`
- **THEN** the event-click callback receives `Z698xZC2Z1k4o-Aap`

## Acceptance criteria → test layer

| AC | Verified by |
|---|---|
| 1–2 | Repository test (MockEngine captures request) |
| 3–4, 6–13 | Mapper/contract test on real fixtures and minimal hand-edited variants of them |
| 5 | Contract test on a captured empty response (`docs/api-samples/events-empty.json`, to capture) |
| 14–20, 23 | Repository test (MockEngine + in-memory Room + test clock) |
| 21–22, 30, 42 | ViewModel test (fake repository) |
| 24–27 | Use-case test |
| 28–29 | UI mapping test |
| 31–38 | Compose UI test, one per state |
| 39–41, 43 | Compose UI test (callback assertions) |
| 15, 42 | Manual check on Android and iOS |
