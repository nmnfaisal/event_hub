plugins {
    alias(libs.plugins.eventhub.kmp.library)
}

// Room driver and path helpers only. Each feature's :data module owns its @Database.
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:model"))
            api(libs.room.runtime)
            api(libs.sqlite.bundled)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.koin.core)
        }
        androidMain.dependencies {
            implementation(libs.koin.android)
        }
    }
}
