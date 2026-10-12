pluginManagement {
    includeBuild("build-logic")
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Event Hub"

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
