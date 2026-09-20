---
name: modularization
description: >
  Use when writing or changing any class, object, function, property, or constant in a
  multi-module Android/Kotlin project (settings.gradle(.kts) includes more than one module:
  :app, :core:*, :feature:*, :data, etc.). Also use for expect/actual placement, commonMain
  module boundaries, and source set questions. Symptoms: a top-level declaration left at
  Kotlin's default public visibility; an implementation class reachable from another module
  though only used within its own; "should this be public or internal?"; choosing between
  implementation and api dependency; deciding which module a new class belongs in; "where
  should this class live?"; "core or feature?"; creating a new module; a feature module
  importing from another feature module; a domain module importing a framework type; circular
  dependency errors; core:common growing into a grab-bag; wondering whether to split or merge
  modules.
---

# Multi-Module Architecture

A module is an independently compilable, independently testable unit of code. The goal is high cohesion within each module and low coupling between them. Every rule below serves that goal. When a rule creates pointless ceremony for a small project, the project is not ready for multi-module architecture yet.

## Why modularize

Incremental builds recompile only changed modules and their dependents, not the entire project. Team ownership is per-module: fewer merge conflicts, clearer code review boundaries, and each module has one accountable owner. Encapsulation via `internal` hides implementation behind the module boundary so consumers depend on a contract, not on internals. Core modules are reusable across features without copy-paste. Tests run per module in isolation, and CI can parallelize them.

**When not to modularize.** A single-module architecture is fine for prototypes, solo projects, and apps with a handful of screens. Introduce modules when multiple developers collaborate, build times hurt, features need independent testing, or the codebase has grown past the point where a single module is navigable. The goal is the right modules, not the most modules.

## Module types

| Type             | Contains                                                                       | Depends on                                                   | Never contains                                                        |
| ---------------- | ------------------------------------------------------------------------------ | ------------------------------------------------------------ | --------------------------------------------------------------------- |
| **app**          | `Application` class, navigation host, DI graph assembly, launcher manifest     | feature, domain, data, core                                   | Business logic, repository implementations, data sources              |
| **feature:***    | One user-facing capability: screens, ViewModels, feature-scoped DI, navigation | domain, core:model, core:ui, core:common                      | Another feature module, repository implementations                    |
| **domain**       | Use cases, business rules, domain model interfaces                             | core:model                                                    | Framework types (see exception below), Android imports, platform APIs |
| **data**         | Repository implementations, data sources (remote + local), DTOs, mappers       | core:network, core:database, core:model, domain (interfaces)  | UI code, ViewModels, Compose                                          |
| **core:model**   | Domain model classes shared across modules                                     | nothing (leaf module)                                         | Android dependencies, framework types                                 |
| **core:network** | HTTP client configuration, API service definitions                             | core:model                                                    | UI code, business logic                                               |
| **core:database**| Room/SQLDelight setup, DAOs, entities, migrations                              | core:model                                                    | UI code, business logic                                               |
| **core:ui**      | Shared composables, theme, design tokens                                       | core:model                                                    | Business logic, data access                                           |
| **core:common**  | Genuinely shared utilities (date formatting, string extensions)                 | nothing or core:model only                                    | Feature-specific logic, "everything else"                             |

Features depend on `:domain` but not on `:data`. Features reach repositories through interfaces declared in `:domain`, bound to `:data` implementations by the DI graph assembled in the app module. A feature depending directly on `:data` is wrong.

**The domain exception for paging-common.** `PagingSource`, `PagingData`, and `LoadResult` from `androidx.paging:paging-common` are allowed in `:domain` because `paging-common` is pure Kotlin, available on all KMP targets, and carries no platform runtime. This is an explicitly named exception to the "no framework types in domain" rule, not a precedent for importing other AndroidX artifacts. See `android-skills:paging` for the full paging setup.

## Dependency direction

Dependencies flow downward only. Circular dependencies are forbidden and Gradle will reject them at configuration time.

```
                              app
                 (assembles DI, navigation host)
                    /          |          \
                   v           v           v
          feature:home   feature:search   feature:profile
                   \          |          /
                    \         |         /          features never depend
                     \        |        /           on each other; they
                      v       v       v            share only :domain
                          domain
                    (use cases, interfaces)
                            |
                            v
                           data
                 (repository implementations)
                    /       |        \
                   v        v         v
          core:network  core:database  core:model  core:ui  core:common
```

The layer flow through a single request is: UI (Composable) -> ViewModel -> UseCase -> Repository -> DataSource -> Network/Database. Each arrow is a downward dependency. No layer may call upward.

**Features never depend on each other.** Two features that need to communicate do so through a navigator interface or a shared contract type in `:domain` or `:core:model`, assembled in the app module. A feature importing another feature is always wrong.

## `implementation` vs `api`

Default to `implementation` for every dependency. Switch to `api` only when a type from the dependency appears in your module's **public API surface**: a public function signature, a public class's supertype, or a public property type. The cost of `api` is transitive recompilation: every module that depends on yours also recompiles when the `api` dependency changes.

```kotlin
// build.gradle.kts for :feature:home
dependencies {
    implementation(projects.domain)              // HomeViewModel calls use cases, but callers of :feature:home don't see them
    implementation(projects.core.model)          // Model types used internally
    implementation(libs.koin.core)               // DI wiring internal to this module
    api(projects.core.ui)                        // Only if :feature:home exposes a composable whose signature includes a core:ui type
}
```

If you are unsure, use `implementation`. You will get a compile error if a consumer needs the transitive type, and that error is the signal to promote to `api`. Promoting reactively is safe; starting with `api` "just in case" leaks coupling silently. For convention plugin mechanics that centralize dependency declarations, see `android-skills:android-gradle-logic`.

## Visibility rules

**Declare everything at the lowest visibility that still compiles.** Start `private`; widen to `internal` only when another file in the same module needs it; widen to `public` only when a different module actually consumes it. Kotlin defaults to `public`, and in a multi-module project that default is usually wrong. A `public` symbol joins the module's API surface, leaks implementation across the boundary, and obliges you to keep it stable.

- **Public interface as the module's API.** The interface is `public`; its **implementation class stays `internal`**, bound through DI and never named from another module.
- **Mapper, helper, extension, constant, UI-state holder** used only inside the module: `internal` (or `private` if a single file uses it). "Rendered by a composable in this module" does **not** require `public`. `internal` is visible to the whole module.
- **Member used only within its class:** `private`.

Widening a symbol to `public` is a decision, not a default. Confirm a real cross-module consumer first.

### KMP visibility

In a Kotlin Multiplatform module, `internal` scopes to the Gradle module across all of its source sets. A declaration marked `internal` in `commonMain` is visible in `androidMain`, `iosMain`, and every other source set of the same module, but invisible to any other module. This is the same boundary as in a pure-Android module; KMP does not add a per-source-set visibility layer.

`expect` and `actual` declarations must have matching visibility. An `expect internal interface TokenStorage` in `commonMain` requires `actual internal class DataStoreTokenStorage` in `androidMain` and `actual internal class KeychainTokenStorage` in `iosMain`. A mismatch is a compile error.

Test source sets (`commonTest`, `androidUnitTest`, `iosTest`) already see `internal` declarations from their corresponding main source sets. Needing to test a class or function is never a reason to widen it to `public`. If a test in `commonTest` needs access, `internal` is sufficient. If a test in a separate test module needs access, that test module should depend on the module under test, and the symbol should still be `internal`.

## Per-feature folder structure

Each feature module organizes its code by layer:

```
feature/home/
  src/commonMain/kotlin/com/example/feature/home/
    data/           -- feature-local data sources or DTOs (if the feature has private data needs)
    domain/         -- feature-local use cases (if business logic is feature-specific)
    presentation/   -- screens, ViewModels, UI models, UI state
    navigation/     -- route definitions, navigator interface
    di/             -- Koin module declaration for this feature
```

Not every feature needs all five folders. A simple feature with no private data access and one screen may have only `presentation/` and `di/`. Do not create empty folders for symmetry.

Shared data access (repositories, DAOs, API services used by more than one feature) lives in the `:data` module, not inside a feature. Feature-local data sources are for truly private needs: a local cache, a feature-specific API endpoint not shared with anyone else.

## DI module ownership

Each feature module declares its own Koin module. The app module assembles all feature modules, plus data and core modules, into the graph at startup via `startKoin { modules(...) }`.

```kotlin
// feature/home/di/HomeModule.kt
val homeModule = module {
    viewModel { HomeViewModel(get(), get()) }
    // feature-scoped bindings
}

// app module, Application.onCreate
startKoin {
    modules(
        homeModule,
        searchModule,
        profileModule,
        dataModule,
        networkModule,
        platformModule,
    )
}
```

Implementation classes bound through DI stay `internal`. The Koin module declaration itself is `val` at file scope (Kotlin default `public`), because the app module must import it. For Koin-specific syntax (KSP annotations, `@Module @ComponentScan`, scopes, `verify()`, KMP `expect val platformModule`), see `android-skills:koin`.

## Core module discipline

`core:common` (or `core:util`) is the most abused module in any multi-module project. It becomes a dumping ground for anything that does not have an obvious home, and eventually everything depends on it, defeating the purpose of modularization.

Rules:

1. A utility used by one feature belongs in that feature, not in core.
2. A utility moves to core only when a second feature genuinely needs it.
3. `core:model` has zero framework dependencies. It holds domain model data classes, enums, and type aliases. No Android imports, no `Context`, no `Parcelable` (use `@Parcelize` in the Android-specific source set of the feature that needs it, or in a core:model `androidMain` source set with the annotation processor scoped there).
4. `core:network` owns the HTTP client. For Ktor client configuration, engine selection, and error mapping, see `android-skills:kmp-ktor`.
5. `core:database` owns Room or SQLDelight setup. For Room in `commonMain` (`@ConstructedBy`, `BundledSQLiteDriver`, per-target KSP), see `android-skills:android-data-layer`.

If `core:common` has more than a handful of files, audit it. Anything feature-specific should move to the feature; anything that is really "data layer utility" should move to `:data` or `core:network`.

## Build variants and dynamic delivery

In a KMP project using AGP 9's `com.android.kotlin.multiplatform.library` plugin, shared library modules have no build variants. Variant-specific configuration (debug/release signing, `applicationId`, `versionCode`) lives exclusively in the Android app module. A debug-vs-release decision inside a shared module surfaces as a runtime configuration value injected through DI, not as a Gradle build variant. See `android-skills:kmp-boundaries` for the full AGP-9 constraint set.

Dynamic feature modules (Play Feature Delivery) are Android-app-only. They are not available in KMP shared modules and do not apply to `commonMain` code. If on-demand delivery is needed, the dynamic feature module wraps or delegates to a shared KMP module rather than containing shared code itself.

## Common mistakes

**Over-modularization.** Not every package deserves its own module. A `:core:date-utils` module with two extension functions adds a Gradle module, a build file, and a dependency edge for near-zero benefit. Group related utilities into a cohesive module. If you cannot write a one-sentence description of what the module owns, it is too granular or too vague.

**Circular dependencies.** Feature A depends on Feature B which depends on Feature A. Gradle catches this at configuration time, but the fix is architectural: extract the shared contract into `:domain` or `:core:model` and have both features depend on that.

**Overusing `api`.** Every `api` dependency is a transitive recompilation edge. A module with five `api` dependencies forces all of its consumers to recompile whenever any of those five change. Start with `implementation`; promote to `api` only when the compiler tells you to.

**Core as dumping ground.** A `core:common` module that every feature depends on and that grows without discipline re-creates the monolith inside a multi-module project. Audit it regularly.

**Feature-to-feature dependencies.** If `:feature:checkout` imports from `:feature:cart`, a change to cart's internals can break checkout. Extract the shared type into `:domain` or `:core:model` and route communication through a navigator interface or shared contract.

**Defaulting to `public`.** Kotlin's default visibility is `public`. In a multi-module project, every `public` top-level declaration is part of the module's API surface. The majority of classes, functions, and properties should be `internal` or `private`.

## Error types across module boundaries

Exception types from `:core:network` and `:core:database` do not appear in `:domain` or `:feature:*` module APIs. They are caught and mapped to domain error types at the repository boundary. For the full error-propagation model, see `android-skills:android-data-layer`.

## Testing

Each module has its own test source set (`commonTest`, `androidUnitTest`, or both). Tests for a module depend only on that module and its test dependencies, not on the full app graph. This keeps test compilation fast and failure signals precise: a red test points at the module that broke, not at "the app."

CI can compile and test modules independently or in parallel. A feature module's tests run without compiling unrelated features. A core module's tests run without any feature at all.

For test shapes, Compose test dispatching, KMP test name restrictions, and the test-first discipline, see `android-skills:android-testing`.

## Related skills

This skill is authoritative for module placement, visibility, dependency type (`implementation` vs `api`), and dependency direction. For specific topics within a module, defer to:

| Topic                                                       | Skill                                                  |
| ----------------------------------------------------------- | ------------------------------------------------------ |
| Koin DI syntax, KSP annotations, `verify()`, scopes        | `android-skills:koin`                                  |
| Repository pattern, error propagation, Room KMP setup       | `android-skills:android-data-layer`                    |
| KMP `expect`/`actual` boundary shape, AGP-9 constraints     | `android-skills:kmp-boundaries`                        |
| Ktor client, engine selection, error mapping                | `android-skills:kmp-ktor`                              |
| Convention plugins, `build-logic/`, version catalogs        | `android-skills:android-gradle-logic`                  |
| Build speed, configuration cache, kapt to KSP              | `android-skills:gradle-build-performance`              |
| Compose architecture, screen structure, state               | `android-skills:compose`, `android-skills:android-dev` |
| Paging setup, `PagingSource`, `cachedIn`, `LazyPagingItems` | `android-skills:paging`                                |
| Test shapes, Compose test dispatching, KMP test names       | `android-skills:android-testing`                       |
| Coroutine scope ownership, dispatcher injection             | `android-skills:kotlin-coroutines`                     |
| DataStore KMP setup, Preferences vs Typed vs Room           | `android-skills:datastore`                             |
