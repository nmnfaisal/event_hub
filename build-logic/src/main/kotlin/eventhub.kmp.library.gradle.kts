plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.android.kotlin.multiplatform.library")
}

kotlin {
    @Suppress("DEPRECATION")
    androidLibrary {
        namespace = "me.nomanfaisal.eventhub.${project.path.removePrefix(":").replace(":", ".")}"
        compileSdk = 37
        minSdk = 26

        withHostTest {
            isIncludeAndroidResources = true
        }
    }

    iosArm64()
    iosSimulatorArm64()

    jvmToolchain(17)

    compilerOptions {
        optIn.add("kotlin.time.ExperimentalTime")
    }
}
