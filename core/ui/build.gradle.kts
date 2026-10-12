plugins {
    alias(libs.plugins.eventhub.kmp.compose)
}

// core:ui depends on no other project module: its composables take primitives only
// (CLAUDE.md Rule 5, ADR-001 Rule 7).
@Suppress("DEPRECATION_ERROR")
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(compose.material3)
            implementation(compose.foundation)
            implementation(compose.ui)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.coil.compose)
            implementation(libs.coil.network.ktor)
        }
    }
}
