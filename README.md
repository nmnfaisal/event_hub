# EventHub

EventHub is a Kotlin Multiplatform event-discovery app for Android and iOS, sharing one
Compose Multiplatform UI and one business-logic layer across both platforms. It browses
upcoming events from the Ticketmaster Discovery API, caches them offline in Room, and is
built as a set of per-feature vertical-slice Gradle modules. Stack: Kotlin Multiplatform,
Compose Multiplatform, Koin, Ktor, Room KMP, Coil 3, kotlinx.serialization, and Gradle
convention plugins, with feature work planned through OpenSpec and recorded in ADRs.

## Why this project exists

I am building this for two reasons, and I would rather state both than dress one up.

First, it is a long-term KMP learning lab. I wanted a real project — not a tutorial — where
I could practise multi-module architecture, offline-first data, and shared UI against actual
API responses and actual toolchain breakage, and keep the decision trail.

Second, it is interview preparation. I recently failed a take-home assessment for a Berlin
fintech company that gave detailed feedback on architectural shortcomings. This project is
where I work through every one of those patterns deliberately — nullable DTOs, `Result`
types at the repository boundary, reactive state derivation, the null-vs-empty distinction —
so they become habit rather than things I remember under pressure. It is a portfolio piece,
and I am not hiding that.

## Architecture

Per-feature vertical slicing (after Philipp Lackner): each feature owns its own `:domain`,
`:data`, and `:ui` modules rather than sharing global `:domain`/`:data` modules. Fifteen
Gradle modules plus two app targets.

```
                            androidApp / iosApp
                                    |
                                    v
                                 shared
                        (nav host, Koin assembly,
                         single iOS framework,
                         cross-feature wiring)
                     /            |            \
                    v             v             v
            feature:events   feature:saved   feature:tickets
             /   |   \         /   |   \        /   |   \
            ui  data domain   ui  data domain  ui  data domain
             |    |    |        \   |   /        \   |   /
             v    v    v         (same shape)     (same shape)
          core:ui  core:model  core:network  core:database
```

Three rules hold the shape together. Features never depend on each other — cross-feature
types live in `core:model` and callbacks are wired in `:shared`. A feature's `:ui` depends
on its own `:domain` and on `core:ui`, never on `:data`; Koin binds the interface to the
implementation at assembly time. Core modules have zero feature knowledge — if deleting all
feature code does not break a core module, the boundary is correct.

Full rationale, the dependency diagram, and how this scales to 10–15 features (the likely
next step is a per-feature `:api` module) are in
[ADR-001](docs/adr/ADR-001-module-structure.md). Data source and offline caching are in
[ADR-003](docs/adr/ADR-003-data-source.md).

## What is shared, what is native

Almost everything lives in `commonMain`. The native boundaries are deliberate and few.

| Concern | Where | Why |
|---|---|---|
| UI, ViewModels, MVI state | shared (`commonMain`) | Compose Multiplatform renders the same screens on both platforms |
| Domain, repositories, mappers, DTOs | shared (`commonMain`) | Pure Kotlin; no reason to split |
| HTTP engine | native (`androidMain`/`iosMain`) | Ktor uses OkHttp on Android, Darwin on iOS |
| Room driver + database file path | native (`androidMain`/`iosMain`) | Android needs a `Context`; iOS needs the documents directory |
| iOS framework export | `:shared` only | One umbrella framework, not one per module — see below |
| Encrypted ticket storage (planned) | native | Ticket secrets belong behind the platform keystore/Keychain, not in shared code |

iOS consumes exactly **one** framework from `:shared`. Exporting several would bundle
duplicate Kotlin runtimes and make a type from framework A a different ObjC type than the
same type from framework B. Reasoning in [ADR-001, Rule 8](docs/adr/ADR-001-module-structure.md).
Toolchain versions and the three version reversions that getting here required are in
[ADR-002](docs/adr/ADR-002-toolchain-versions.md).

## AI-first workflow

This is the part I care most about, because the architecture above is a product of the
workflow, not the other way around.

**The skill set.** The project runs against 26 installed Claude Code skills — domain
references (`modularization`, `android-data-layer`, `kmp-ktor`, `koin`, `compose`, …) plus
the OpenSpec workflow skills. With that many skills, overlap is a real risk, so they are
audited for conflicts against each other and against CLAUDE.md. The `modularization` skill
in particular was adapted from an external write-up and then stress-tested against this
codebase, which is how its gap surfaced (see below).

**The friction log.** [docs/friction-log.md](docs/friction-log.md) records every place a
skill, a CLAUDE.md rule, and a project decision disagree. The rule is that new or changed
skills come *from* the friction log, not from a hunch — if there is no recorded friction, I
do not touch a skill.

One concrete example, [FL-001](docs/friction-log.md):

> **Before.** The `modularization` skill says "a feature importing another feature is always
> wrong," but it only illustrates a *global* `:domain`/`:data` layout. With per-feature
> slicing, when `feature:saved` needs to show event data, the skill gives no answer. My first
> draft relaxed the rule: let `feature:saved:ui` depend on `feature:events:domain`.
>
> **After.** Rejected. I kept the rule intact and worked the case through to the
> contract-type-plus-callback pattern: `core:model` holds an `EventSummary` contract,
> `feature:events:ui` fires an `onSaveEvent` callback wired in `:shared`, and neither feature
> imports the other. The friction log records this as a pending skill update — add the
> per-feature illustration — which is not applied until explicitly instructed.

**Spec-driven development.** Features go through [OpenSpec](openspec/): a proposal, a spec
with GIVEN/WHEN/THEN scenarios, a design, and ordered tasks — *before* code. The Home screen
was specified first, including a design-deviation list checking the plan against the actual
Ticketmaster response shapes, then implemented. [openspec/config.yaml](openspec/config.yaml)
encodes the project's rules so every generated spec demands the empty-response, network-error,
and never-loaded scenarios.

**The model gate.** Plans are approved before implementation starts, and the model switches
by task: Opus for architecture and decisions, Sonnet for implementation. Decisions are where
a wrong call is expensive and hard to reverse; implementation is where throughput against an
approved plan matters more. CLAUDE.md enforces a hard stop — no code until the plan is
approved and I say go.

**What I learned.** Better than expected: writing the spec and the deviation list first paid
off immediately — checking the planned domain model against real API samples caught that
`priceRanges` is absent in all 25 sampled events and that venue names are missing in 3 of 5,
*before* any mapper code assumed otherwise. Worse than expected: the toolchain fought back
harder than the architecture did. ADR-002 records three separate version reversions (KSP,
Room, serialization) just to get 15 modules to compile on both targets — more of the effort
went into dependency compatibility than into any design decision.

## Project status

- **Built:** the Events Home screen — category chips, a "Trending near you" carousel, a
  "Coming up" list, pull-to-refresh, and five UI states (loading skeleton, content, content
  with offline banner, empty, error). Offline-first per-category Room cache with 15-minute
  staleness. Real Ticketmaster data loads on the Android emulator. Tests run on both the
  Android host (Robolectric) and the iOS simulator.
- **Planned:** Search, Event detail, the Saved feature (offline list), and the Tickets
  feature (encrypted local storage). These exist as placeholder tabs today.
- **Planned toolchain work:** upgrade to Kotlin 2.3.x + CMP 1.11, which unblocks
  serialization 1.10 and removes the compose-accessor deprecation suppressions.

## Tech stack

Kotlin · Kotlin Multiplatform · Compose Multiplatform · Koin · Ktor · Room KMP · Coil 3 ·
kotlinx.serialization · Gradle convention plugins · OpenSpec

## How to build

**Prerequisites:** JDK 17, Android Studio (latest stable) with an Android SDK, and Xcode
(for the iOS target). A free Ticketmaster Discovery API key.

**API key.** Add your key to `local.properties` at the repo root (it is gitignored and
generated into `core:network` at build time):

```properties
TICKETMASTER_API_KEY=your_key_here
```

**Android:**

```bash
./gradlew :androidApp:installDebug
```

**iOS:** open `iosApp/iosApp.xcodeproj` in Xcode and run on a simulator.
