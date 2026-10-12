plugins {
    alias(libs.plugins.eventhub.kmp.library)
}

kotlin {
    sourceSets {
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
