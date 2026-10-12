plugins {
    alias(libs.plugins.eventhub.kmp.compose)
}

@Suppress("DEPRECATION_ERROR")
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:model"))
            implementation(compose.material3)
            implementation(compose.foundation)
            implementation(compose.ui)
        }
    }
}
