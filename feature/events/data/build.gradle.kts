plugins {
    alias(libs.plugins.eventhub.kmp.library)
    alias(libs.plugins.kotlinx.serialization)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:model"))
            implementation(project(":core:network"))
            // implementation(project(":core:database")) — add when first @Entity is defined
            implementation(project(":feature:events:domain"))
        }
    }
}
