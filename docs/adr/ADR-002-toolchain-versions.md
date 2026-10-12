# ADR-002: Toolchain Versions

**Status:** Accepted
**Date:** 2026-09-22

## Decision

Pinned versions for the project skeleton:

| Tool | Version | Constraint |
|------|---------|-----------|
| Kotlin | 2.2.20 | Must match KSP and CMP compatibility |
| Compose Multiplatform | 1.10.3 | Requires Kotlin >= 2.2.20 for iOS targets |
| KSP | 2.3.4 | Changed from 2.2.20-2.0.3: that plugin throws `KotlinMultiplatformAndroidCompilationImpl cannot be cast to KotlinJvmAndroidCompilation` with AGP's `com.android.kotlin.multiplatform.library`. 2.3.0 fails the same way; 2.3.4 configures and runs on Kotlin 2.2.20 |
| AGP | 9.4.1 | Already in project, stable |
| Gradle | 9.6.0 | Required by AGP 9.4.x |
| kotlinx-serialization | 1.9.0 | 1.10.0 requires Kotlin 2.3+; KLIBs fail on 2.2.20 |
| Koin BOM | 4.1.1 | Explicit per-artifact versions (KMP platform() is deprecated) |
| Ktor | 3.1.0 | Latest stable |
| Room / sqlite-bundled | 2.7.0 / 2.5.0 | Activated in `events-home`. Changed from 2.7.1: the 2.7.1 iOS klibs are built by Kotlin 2.3.20 (ABI 2.3.0) and the 2.2.20 compiler rejects them. 2.7.0 consumes fine. Revisit with the Kotlin 2.3 upgrade |
| Coil 3 | 3.3.0 | Deferred from skeleton — no image loading yet |
| kotlinx-coroutines | 1.10.2 | Added in `events-home` (core, test) |
| kotlinx-datetime | 0.7.1 | Added in `events-home` (`:feature:events:data` date parsing/time zones) |
| kotlinx-io-core | 0.8.0 | Test only: reads JSON fixtures on JVM and iOS simulator |
| JetBrains lifecycle-viewmodel-compose | 2.10.0 | Added in `events-home` (ViewModel in common code) |
| ktor-client-mock | 3.1.0 | Test only; matches Ktor |
| Turbine | 1.2.0 | Test only: Flow assertions |
| Robolectric | 4.16 | Test only: Android host tests for Room and Compose |

## CMP compose accessor deprecation

CMP 1.10.3 deprecates `compose.material3`, `compose.foundation`, `compose.ui`
accessors with level `ERROR`. The internal artifact versions don't match the
CMP release version, so direct Maven coordinates are not straightforward.
Build scripts use `@Suppress("DEPRECATION_ERROR")` with the accessors until
the migration to CMP 1.11+ resolves this by providing documented coordinates.

## Why one minor behind the latest

Compose Multiplatform 1.11.0 requires Kotlin 2.3.10 for native targets.
Kotlin 2.3.x is a minor version jump from 2.2.x with potential breaking
changes in the Kotlin/Native compiler and source-set resolution. At project
start with 15 modules to wire, stability matters more than features.

CMP 1.10.3 is the latest patch of the 1.10 series, includes iOS bug fixes
and native interop improvements, and is compatible with Kotlin 2.2.20.

## Upgrade path

Upgrading to CMP 1.11.x + Kotlin 2.3.x is a later lab task. Steps:

1. Bump Kotlin to 2.3.10 in `libs.versions.toml`
2. Bump KSP to the matching 2.3.10-x.y.z version
3. Bump CMP to 1.11.1
4. Run `./gradlew build` and fix any Kotlin/Native or Compose API breaks
5. Re-verify iOS simulator launch

This should be done after the first feature (events list) ships, not during
skeleton setup. The Kotlin upgrade also unblocks:
- kotlinx-serialization 1.10.0 (currently pinned at 1.9.0)
- Removing `@Suppress("DEPRECATION_ERROR")` from compose accessor usage
