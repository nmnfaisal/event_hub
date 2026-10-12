# Ticketmaster Discovery v2 — `events.json` API notes (Rule 8)

Endpoint: `GET https://app.ticketmaster.com/discovery/v2/events.json`
Evidence: `docs/api-samples/` (25 real events), spec `openspec/changes/events-home`, ADR-003.
Status: **Observed** = seen in a captured response. **Documented** = from the API docs only. **Open** = unknown, needs a live check.

## Pagination
- **Observed:** `page { size, totalElements, totalPages, number }`. Berlin/Music had 335 results over 67 pages of 5.
- **Documented:** deep paging is limited: `size * page` must stay below 1000.
- **Our use:** one page of 50, `page=0`, per category. No paging in v1 (spec non-goal).
- **Open:** whether `sort=relevance,desc` is stable between requests (affects Trending flicker on refresh). Owner: check during task 9.3.

## Error contract
Two different shapes were seen, so the client must not assume one:
- **Gateway fault** (bad key): `{"fault":{"faultstring":"Invalid ApiKey","detail":{"errorcode":"oauth.v2.InvalidApiKey"}}}` — `docs/api-samples/error-invalid-key.json`.
- **Discovery error** (e.g. 404 `DIS1004`): `{"errors":[{"code":"DIS1004","detail":"…","status":"404"}]}` — currently in `events-empty.json`.
- We map by HTTP status only (`safeCall`): 401/403 → Unauthorized, 429 → TooManyRequests, 5xx → Server, other → Unknown. Bodies are not parsed.
- **Open:** the status code returned for an invalid key (the sample file has no status). Mapping assumes 401. Owner: re-capture with status when doing task 1.2.
- **Open (task 1.2):** `events-empty.json` is a 404 error, not an empty success. A real empty result is documented as HTTP 200 with `page.totalElements = 0` and **no `_embedded`**; AC-5 is tested against a hand-written body of that shape until a real capture exists.

## Rate limits
- **Documented:** 5 requests/second, 5000/day per key. Exceeding gives HTTP 429.
- **Our use:** at most one request per category per 15 minutes plus explicit pull-to-refresh; six categories gives a worst case of a handful of requests per session.
- **Open:** whether 429 carries `Retry-After` or the `Rate-Limit-*` headers (documented as `Rate-Limit`, `Rate-Limit-Available`, `Rate-Limit-Over`, `Rate-Limit-Reset`). We ignore them today. Owner: log headers on a live call in 9.3.

## Caching headers
- **Open:** no `Cache-Control` or `ETag` has been inspected. We do our own 15-minute staleness (ADR-003) and never send conditional requests. Revisit if the live response supports `ETag`.

## Field stability (what the mapper cannot rely on)
| Field | Finding | Handling |
|---|---|---|
| `priceRanges` | missing in 25/25 events | `priceFrom = null`, no price shown |
| `_embedded.venues[0].name` | missing in 3 of 5 Berlin events | fall back to `address.line1`, else null |
| `dates.status.code` | `cancelled` in data, `canceled` in docs | both map to Cancelled; unknown → Unknown |
| `images[].ratio` | absent on `_SOURCE` originals | ignored; never chosen |
| `images[]` sizes | `_SOURCE` can be multi-MB | hero = smallest 16:9 ≥ 1024px |
| `classifications[]` | `primary` not always first | pick `primary = true`, else first |
| `_embedded` (top level) | omitted when there are no results | empty list, not an error |
| `dates.start.dateTime` | present in all samples | fallback to `localDate`+`localTime`+`timezone` |
| `classificationName` values for Film and Miscellaneous | **never sampled** | **Open.** A wrong value shows Empty, not a crash. Owner: verify in 9.3 |

## Other questions
- `startDateTime` must be `yyyy-MM-ddTHH:mm:ssZ` with no fraction (observed requirement in docs). Covered by `TicketmasterEventsApiTest`.
- API key travels as a query parameter, so it appears in any request log. Never log full URLs.
