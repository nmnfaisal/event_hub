# ADR-001: Module Structure

**Status:** Accepted
**Date:** 2026-09-22
**Context:** EventHub is a Kotlin Multiplatform event discovery and ticketing app
(Android + iOS). The architecture uses Philipp Lackner's per-feature vertical
slicing: each feature owns its own `:domain`, `:data`, and `:ui` Gradle modules
instead of sharing root-level `:domain` and `:data` modules.

---

## Decision

### Module inventory (15 Gradle modules + 2 app targets)

```
androidApp                          Android entry point: MainActivity, Application, launcher manifest
iosApp                              Xcode project (not a Gradle module)
shared                              Umbrella: nav host, Koin assembly, single iOS framework export
build-logic                         Convention plugins (composite build, not an app module)

core/model                          DataError, Money, value-class IDs, cross-feature contracts (EventSummary) (pure Kotlin, zero dependencies)
core/network                        Ktor HttpClient, API-key plugin, base URL
core/database                       Room driver factory, BundledSQLiteDriver setup (no entities, no DAOs)
core/ui                             Theme, shared composables: loading/empty/error/offline views

feature/events/domain               Event model, EventRepository interface, EventId value class
feature/events/data                 Ktor API service, DTOs, mapper, repository impl, Room @Database + DAO
feature/events/ui                   EventListViewModel, EventDetailViewModel, screens, MVI state

feature/saved/domain                SavedEventRepository interface, domain types
feature/saved/data                  Room @Database + DAO, repository impl
feature/saved/ui                    SavedListViewModel, screen, offline banner

feature/tickets/domain              Ticket model, TicketRepository interface, TicketId value class
feature/tickets/data                Encrypted local store, seeded data, repository impl
feature/tickets/ui                  TicketListViewModel, TicketDetailViewModel, screens
```

### settings.gradle.kts shape

```kotlin
includeBuild("build-logic")

include(":androidApp")
include(":shared")

include(":core:model")
include(":core:network")
include(":core:database")
include(":core:ui")

include(":feature:events:domain")
include(":feature:events:data")
include(":feature:events:ui")

include(":feature:saved:domain")
include(":feature:saved:data")
include(":feature:saved:ui")

include(":feature:tickets:domain")
include(":feature:tickets:data")
include(":feature:tickets:ui")
```

---

## Dependency rules

### Rule 1: Features never depend on each other

No feature module (`:domain`, `:data`, or `:ui`) depends on any module from
another feature. Per the modularization skill: "A feature importing another
feature is always wrong." Two features that need shared data communicate
through contract types in `core/model`, assembled in `:shared`.

`:data` and `:ui` are **private to the feature** -- all top-level declarations
in these modules are `internal` (per the modularization skill: start `private`,
widen to `internal` when another file in the same module needs it, widen to
`public` only when a different module consumes it). Each feature's `:domain`
exposes `public` interfaces and models consumed only by its own `:data` and
`:ui`, plus `:shared` for DI assembly.

### Rule 2: `:ui` never depends on `:data`

The `:ui` module depends on its own feature's `:domain` (for interfaces and
models) and on `core/ui` (for shared composables) and `core/model` (for
`DataError`, `Money`). It never depends on `:data`. Koin in `:shared` binds
domain interfaces to data implementations at graph assembly time. This follows
the modularization skill's convention: features reach repositories through
interfaces declared in `:domain`, bound to implementations by DI.

### Rule 3: `core/*` never depends on any feature

If deleting all feature code does not break any core module, the boundary is
correct (CLAUDE.md Rule 9). Feature-specific API services, DTOs, mappers, DAOs,
and entities live in `feature/*/data`, never in `core/*`.

### Rule 4: Cross-feature communication goes through `core/model` and `:shared`

Features that need shared data use contract types defined in `core/model`.
Each feature's `:domain` depends only on `core/model` -- never on another
feature. `:shared` wires cross-feature flows via callbacks and Koin bindings.

Example: the save-event flow. `core/model` defines `EventId` (value class).
`feature:saved:domain` defines its own `SavedEvent` model containing
denormalized event data (name, date, venue, image URL). When the user taps
save on Event detail, `feature:events:ui` fires a callback
`onSaveEvent: (EventSummary) -> Unit` wired in `:shared`. The `EventSummary`
type lives in `core/model` as a lightweight cross-feature contract. `:shared`
routes it to `feature:saved:domain`'s `SavedEventRepository.save(...)`.
Neither feature imports the other.

**Staleness.** Denormalization means `feature:saved:data` stores a snapshot of
the event at save time. The fields copied are: name, date/time, venue name,
image URL, price range, and `EventId`. If the upstream event changes (venue
moves, date shifts, event cancelled), the saved copy is stale. In v1 nothing
reconciles this. There is no background sync, no TTL, and no "last refreshed"
timestamp. This is accepted because: (a) the Ticketmaster Discovery API has
no push/webhook mechanism, so detecting upstream changes requires polling per
saved event, which is disproportionate for v1; (b) the user can tap a saved
event to open event detail, which always fetches live data via
`feature:events:data`, so they see current data on drill-in. The saved list
itself may show stale data. A future version could add a `savedAt` timestamp
and refresh stale entries on app foreground, but that is not committed to.

**Admission rule for `core/model` contract types.** Not every cross-feature
type belongs in `core/model`. The modularization skill warns that core becomes
a dumping ground without discipline. A type qualifies for `core/model` only if:

1. It is consumed by two or more features that cannot otherwise communicate.
2. It carries no feature-specific logic -- pure data, no methods beyond
   accessors and `equals`/`hashCode`/`toString` (data class or value class).
3. It is stable: its fields do not change when a single feature's internal
   model changes. If a field is only meaningful to one feature, it does not
   belong in the contract type.

A type that fails any of these stays in the feature that owns it. The decision
to add a contract type is made in an ADR or design review, not ad hoc. In v1
the only cross-feature contract types are: `EventSummary` (events -> saved)
and the value-class IDs (`EventId`, `TicketId`). `DataError` and `Money` are
core primitives, not cross-feature contracts.

### Rule 5: Each feature `:data` owns its own Room @Database

A single shared database in `core/database` would force that core module to
import feature entities (`EventEntity`, `SavedEventEntity`), violating Rule 3.
Instead, each feature's `:data` declares its own `@Database` with
`@ConstructedBy`, using the `BundledSQLiteDriver` from `core/database`. The
core module provides only the driver factory and builder utilities -- zero
entity knowledge.

Per the android-data-layer skill, each database uses:
- `@ConstructedBy(...)` with an `expect object` constructor
- `BundledSQLiteDriver` (from `androidx.sqlite:sqlite-bundled`) for consistent
  SQLite versions across Android/iOS
- `setQueryCoroutineContext(Dispatchers.IO)`

### Rule 6: Cross-feature navigation via callbacks

Navigation uses callbacks like `onEventClick: (EventId) -> Unit`, wired in
`:shared`'s nav host. No navigation module in v1. This follows the
modularization skill's guidance: two features that need to communicate do so
through a shared contract type in `:domain` or `core/model`, assembled in the
app/shared module.

### Rule 7: `core/ui` does NOT depend on `core/model`

CLAUDE.md Rule 5 requires design system composables to take primitives
(`String`, `Boolean`, `() -> Unit`), never domain types. Therefore `core/ui`
has zero knowledge of `DataError`, `Event`, or any model class. The
presentation layer in each feature's `:ui` maps domain models to composable
parameters.

### Rule 8: iOS consumes exactly one framework from `:shared`

The `:shared` module is the single umbrella that re-exports everything iOS
needs. Exporting multiple KMP modules as separate frameworks is wrong because:

1. **Duplicate runtime.** Each framework bundles its own copy of the Kotlin
   runtime and stdlib, inflating the binary and risking class-identity
   conflicts at the ObjC boundary.
2. **Cross-framework type identity.** A `Event` class from framework A is a
   different ObjC type than `Event` from framework B, even if the Kotlin source
   is identical. Passing objects between frameworks silently fails type checks.
3. **Dependency ordering.** Frameworks cannot express Gradle-style dependency
   edges. The consumer (Xcode/CocoaPods/SPM) sees independent binaries with no
   guaranteed initialization order or shared state.
4. **Swift interop complexity.** Per the kmp-boundaries iOS interop reference,
   Kotlin types are already subject to naming translation (`BarKt.foo()`,
   `AppInit.shared`), type-width mismatches (`Int` -> `Int32`), and collection
   copying at the boundary. Multiplying frameworks multiplies these friction
   surfaces.

The correct pattern: `:shared` depends on all feature and core modules, exports
a single `XCFramework`, and iOS code imports one module name.

---

## Dependency diagram

```
                            androidApp
                                |
                                v
                             shared
                     (nav host, Koin assembly,
                      single iOS framework,
                      cross-feature wiring)
                   /        |          \
                  v         v           v
        feature:events  feature:saved  feature:tickets
          /   |   \       /   |  \       /   |   \
         v    v    v     v    v   v     v    v    v
       ui   data domain  ui  data domain  ui  data domain
        |    |     |      |   |     |      |   |     |
        |    |     |      |   |     |      |   |     |
        +----+--+--+      +---+--+--+      +---+--+--+
             |   \             |   \            |   \
             v    \            v    \           v    \
          core/model\       core/model\     core/model\
                     v                v               v
    :ui ---> core/ui            core/network  <-- feature/*/data
                                core/database <-- feature/*/data
```

**No feature depends on any other feature.** Every arrow flows downward
to `core/*` modules. `:shared` is the only module that sees all features,
and it wires cross-feature callbacks at the navigation level.

### The concrete case: Saved displays Events

`feature:saved` needs to show event data (name, date, venue) for saved events.
It does **not** depend on `feature:events`. Instead:

1. `core/model` defines `EventSummary` -- a lightweight contract type with
   the fields needed to display a saved event (name, date, venue, image URL,
   `EventId`). This type has no feature-specific logic.

2. When the user taps save on Event detail, `feature:events:ui` fires a
   callback `onSaveEvent: (EventSummary) -> Unit`, wired in `:shared`.

3. `:shared` routes the call to `feature:saved:domain`'s
   `SavedEventRepository.save(EventSummary)`.

4. `feature:saved:data` stores the denormalized `EventSummary` fields in its
   own Room database. It never queries the events database.

5. `feature:saved:ui` reads from `SavedEventRepository` and displays the
   stored data. No dependency on `feature:events` at any layer.

```
feature:events:ui  -- onSaveEvent callback -->  :shared  -->  feature:saved:domain
       |                                                            |
       v                                                            v
    core/model (EventSummary)                                   core/model
```

Similarly, Event detail's save button state (is this event saved?) is exposed
by `:shared` as a callback `isEventSaved: (EventId) -> Flow<Boolean>`, which
`:shared` implements by calling `SavedEventRepository.isEventSaved(EventId)`.
`feature:events:ui` consumes the flow without knowing which feature provides it.

This pattern follows the modularization skill exactly: "Two features that need
to communicate do so through a shared contract type in `:core:model`, assembled
in the app module."

### DI wiring in `:shared`

Per the Koin skill, each feature declares its own Koin module (file-scope
`val`, `public` because `:shared` must import it). Implementation classes
stay `internal` within `:data`. Binding happens in `:shared`:

```kotlin
// shared/src/commonMain/kotlin/di/AppModule.kt
startKoin {
    modules(
        // core
        networkModule,
        databaseModule,
        platformModule,  // expect val platformModule: Module
        // features
        eventsDataModule,
        eventsDomainModule,  // if use cases exist
        eventsUiModule,
        savedDataModule,
        savedUiModule,
        ticketsDataModule,
        ticketsUiModule,
    )
}
```

---

## Build-time consequences of 15 modules

### Benefits
- **Incremental builds:** A change to `feature:events:ui` recompiles only that
  module and `:shared`/`androidApp` above it. No other feature recompiles.
- **Parallel compilation:** Gradle compiles independent modules concurrently.
  The 9 feature modules (3 features x 3 layers) plus 4 core modules can
  compile in parallel where dependencies allow.
- **Test isolation:** Each module has its own test source set. CI can run
  feature tests independently without compiling unrelated features.

### Costs and mitigations

| Cost | Mitigation |
|------|------------|
| **Configuration time** scales with module count | Enable Gradle configuration cache (`org.gradle.configuration-cache=true`). Required at this scale. |
| **Convention plugin boilerplate** for 15 modules | `build-logic/` composite build with convention plugins (`myapp.kmp.library`, `myapp.kmp.compose`). Each module's `build.gradle.kts` collapses to plugin alias + namespace. Per the android-gradle-logic skill, `build-logic/settings.gradle.kts` must recreate the `libs` catalog via `from(files("../gradle/libs.versions.toml"))`. |
| **iOS compilation time** multiplied by targets | Limit iOS targets to the minimum needed. In dev, build only `iosSimulatorArm64`. Add `iosX64` and `iosArm64` in CI only. Use `KOTLIN_NATIVE_BINARY_VALIDATION=false` for debug builds. |
| **KSP per-target wiring** for Room | Room KSP compiler must be wired per target, not just as a top-level `ksp(...)`. Convention plugin handles this once. |
| **AGP 9 constraints** | Per the kmp-boundaries skill: no `BuildConfig` in shared modules (use BuildKonfig or DI-injected config), no build variants in library modules, `kapt` incompatible (KSP only). Convention plugins absorb these constraints. |

---

## How this scales

At three features this pattern is light. The question is what happens at ten
or fifteen.

### Contract type growth in `core/model`

At three features, `core/model` holds one cross-feature contract type
(`EventSummary`) plus shared primitives (`DataError`, `Money`, value-class
IDs). The number of contract types grows with the number of cross-feature
data flows, not with the number of features. Most features do not share data:
a notifications feature, a profile feature, and a settings feature each stand
alone. My guess is that an app with 10-15 features would have 3-6 contract
types, not 10-15.

The real risk is not count but churn. Every feature depends on `core/model`
via `implementation`. If `core/model` changes, every dependent module
recompiles. At three features, that is 9 feature modules plus `:shared`.
At fifteen features, that is 45 feature modules. A change to `EventSummary`
that only matters to events and saved would force recompilation of all 45.

**Breaking-point signal:** `core/model` changes more than once per feature
development cycle (i.e., building feature X requires changing a contract type
that feature Y also depends on, causing Y to recompile for no functional
reason). A second signal: a contract type's fields start diverging by
consumer -- saved wants `imageUrl` but search wants `thumbnailUrl` and
`highResUrl`, so the "shared" type is really two types stapled together.

### Callback growth in `:shared`

At three features, `:shared` wires roughly 5-8 callbacks: navigation
(`onEventClick`, `onTicketClick`, `onSavedEventClick`) plus cross-feature
actions (`onSaveEvent`, `isEventSaved`, `onShareEvent`). At fifteen features,
this is a guess: 20-40 callbacks. `:shared` becomes the module that knows
every feature's public surface -- the navigation host, DI assembly, and
cross-feature wiring all live there.

`:shared` is already a god module in the sense that it depends on everything.
That is inherent to the umbrella role (nav host, Koin assembly, iOS framework
export). The question is whether its *code* grows to god-module size.
Navigation wiring is mechanical (route -> screen composable + callbacks) and
scales linearly. DI assembly is a list of module references. The dangerous
growth is cross-feature action wiring, because each new cross-feature flow
adds logic, not just a line.

**Breaking-point signal:** `:shared` contains `when` branches or `if` logic
that decides *what* to do with a cross-feature action, rather than just
routing it. A second signal: `:shared`'s source file count exceeds 10, or
a single wiring file exceeds 200 lines.

### What to move to when these signals fire

**Option 1: Per-feature `:api` module (the likely next step).**

Each feature that exports a cross-feature contract gets a fourth module:
`feature/events/{api,domain,data,ui}`. The `:api` module holds only the
contract types and callback interfaces that other features consume. Other
features depend on `feature:events:api`, never on `:domain`, `:data`, or
`:ui`. This is a controlled relaxation of "features never depend on each
other" -- they depend only on a stable, minimal API surface.

```
feature:saved:ui  -->  feature:events:api  (EventSummary, EventId)
feature:saved:ui  -->  feature:saved:domain
feature:events:api -->  core/model
```

**Cost:** One additional module per exporting feature (not per feature -- most
features export nothing). At 15 features, maybe 4-5 `:api` modules. Total
module count goes from ~49 to ~53. Configuration time increases marginally.
The real cost is the discipline to keep `:api` minimal -- it must not grow
into a second `:domain`.

**Why not now:** At three features with one cross-feature contract, the
`:api` module adds a Gradle module, a build file, and a convention plugin
application for a single data class. The modularization skill's warning
applies: "A `:core:date-utils` module with two extension functions adds a
Gradle module, a build file, and a dependency edge for near-zero benefit."
The indirection is not justified until `core/model` shows the churn signal.

**Option 2: Separate `:contracts` module per feature.**

Same idea as Option 1 but named differently (`feature:events:contracts`
instead of `feature:events:api`). The semantics are identical. The name
`:api` is more conventional in Android/KMP; `:contracts` is clearer about
what it contains. This is a naming choice, not an architectural one. We
would pick one name at migration time.

**Option 3: Event bus or mediator in `:shared`.**

Replace callbacks with a typed event bus or mediator pattern. Features
publish and subscribe to events via an interface in `core/model`, with the
implementation in `:shared`.

**Cost:** Indirection makes control flow harder to trace. Event ordering
and lifecycle become concerns. Testing requires a fake bus. This is the
standard "observer pattern at scale" problem.

**Why not now or likely ever:** The callback pattern is explicit and
type-checked at compile time. An event bus trades compile-time safety for
runtime flexibility we do not need. This is a last resort, not a next step.

---

## Alternatives considered

### A. Global `:domain` and `:data` modules

The modularization skill's default: one `:domain` for all use cases and
interfaces, one `:data` for all repositories and data sources.

**Rejected because:**
- A single `:data` module becomes a recompilation bottleneck. Changing the
  events API service recompiles saved and tickets data sources unnecessarily.
- A single `:domain` module couples unrelated feature contracts. Adding a
  `TicketRepository` interface triggers recompilation of events ViewModels
  that depend on `:domain` but never use tickets.
- Feature teams cannot own their vertical slice end-to-end. Code review
  boundaries blur.
- The failed assessment feedback specifically cited lack of modular isolation
  as a shortcoming. Per-feature slicing directly addresses this.

### B. api/impl split per feature

Each feature has `:api` (public interfaces + models) and `:impl` (everything
else), instead of `:domain` / `:data` / `:ui`.

**Rejected because:**
- Conflates presentation and data into a single `:impl` module, losing the
  `:ui` never depends on `:data` invariant.
- Does not enforce build order (domain -> presentation -> data, CLAUDE.md
  Rule 7) because there is no separate presentation module.
- More Java/backend convention than Android/KMP convention. The
  domain/data/presentation split is idiomatic in Android architecture.

### C. One module per screen

Each screen (Home, Search, EventDetail, SavedList, TicketList, TicketDetail)
gets its own module.

**Rejected because:**
- Over-modularization: 6 screens x 3 layers = 18 feature modules, plus core.
  Per the modularization skill: "if you cannot write a one-sentence description
  of what the module owns, it is too granular."
- Home, Search, and EventDetail share the `Event` domain model and
  `EventRepository` -- splitting them into separate domain modules duplicates
  the contract or forces a shared events domain module anyway, arriving back
  at the chosen structure.
- Configuration time and Gradle overhead are not justified for a 3-feature app.

### D. Single module (no modularization)

**Rejected because:**
- The project exists specifically to practice multi-module architecture.
- No encapsulation: `internal` visibility has no effect in a single module.
- No incremental build benefit.
- Does not demonstrate the architectural maturity expected in the interview.

---

## Consequences

- Every new feature follows the `feature/X/{domain,data,ui}` pattern. Not
  every feature needs all three layers -- a feature with no persistent data
  may skip `:data` (Rule 7: only create justified modules).
- Convention plugins in `build-logic/` are a prerequisite. Without them,
  15 `build.gradle.kts` files with duplicated configuration become
  unmaintainable.
- The Gradle configuration cache must be enabled from day one.
- iOS targets should be limited during development to avoid multiplying
  native compilation time across 15 modules.
- Features never depend on each other. Cross-feature communication uses
  contract types in `core/model` and callback wiring in `:shared`. This
  aligns with the modularization skill's rule without exception.
