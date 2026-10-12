plugins {
    alias(libs.plugins.eventhub.kmp.library)
}

// Room and sqlite-bundled will be added when feature :data modules
// define their first @Entity. No entities exist in the skeleton.
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:model"))
        }
    }
}
