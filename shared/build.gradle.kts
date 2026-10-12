plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose)
}

kotlin {
    @Suppress("DEPRECATION")
    androidLibrary {
        namespace = "me.nomanfaisal.eventhub.shared"
        compileSdk = 37
        minSdk = 26

        withHostTest {
            isIncludeAndroidResources = true
        }
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { target ->
        target.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }

    jvmToolchain(17)

    @Suppress("DEPRECATION_ERROR")
    sourceSets {
        commonMain.dependencies {
            implementation(compose.material3)
            implementation(compose.foundation)
            implementation(compose.ui)

            implementation(project(":core:model"))
            implementation(project(":core:network"))
            implementation(project(":core:database"))
            implementation(project(":core:ui"))

            implementation(project(":feature:events:domain"))
            implementation(project(":feature:events:data"))
            implementation(project(":feature:events:ui"))

            implementation(project(":feature:saved:domain"))
            implementation(project(":feature:saved:data"))
            implementation(project(":feature:saved:ui"))

            implementation(project(":feature:tickets:domain"))
            implementation(project(":feature:tickets:data"))
            implementation(project(":feature:tickets:ui"))

            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.kotlinx.coroutines.core)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
        getByName("androidHostTest").dependencies {
            implementation(libs.robolectric)
            implementation(libs.androidx.junit)
            implementation(libs.androidx.core.ktx)
            implementation(libs.koin.android)
        }

        androidMain.dependencies {
            implementation(libs.koin.android)
        }
    }
}
