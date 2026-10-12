plugins {
    alias(libs.plugins.eventhub.kmp.compose)
}

@Suppress("DEPRECATION_ERROR")
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:model"))
            implementation(project(":core:ui"))
            implementation(project(":feature:tickets:domain"))
            implementation(compose.material3)
        }
    }
}
