plugins {
    alias(libs.plugins.eventhub.kmp.library)
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

// Absolute path of docs/api-samples, generated so commonTest can read the real
// fixtures on both the JVM host and the iOS simulator (both see the host filesystem).
val generateFixturePaths = tasks.register("generateFixturePaths") {
    val samplesDir = rootProject.layout.projectDirectory.dir("docs/api-samples").asFile.absolutePath
    val outputDir = layout.buildDirectory.dir("generated/fixtures/kotlin")
    inputs.property("samplesDir", samplesDir)
    outputs.dir(outputDir)
    doLast {
        val file = outputDir.get()
            .file("me/nomanfaisal/eventhub/feature/events/data/FixturePaths.kt").asFile
        file.parentFile.mkdirs()
        file.writeText(
            """
            package me.nomanfaisal.eventhub.feature.events.data

            internal const val API_SAMPLES_DIR: String = "$samplesDir"
            """.trimIndent() + "\n"
        )
    }
}

room {
    schemaDirectory("$projectDir/schemas")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:model"))
            implementation(project(":core:network"))
            implementation(project(":core:database"))
            implementation(project(":feature:events:domain"))
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.json)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.datetime)
            implementation(libs.room.runtime)
            implementation(libs.sqlite.bundled)
            implementation(libs.koin.core)
        }
        commonTest {
            kotlin.srcDir(generateFixturePaths)
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.kotlinx.coroutines.test)
                implementation(libs.kotlinx.io.core)
                implementation(libs.ktor.client.mock)
                implementation(libs.turbine)
            }
        }
        getByName("androidHostTest").dependencies {
            implementation(libs.robolectric)
            implementation(libs.androidx.junit)
            implementation(libs.androidx.core.ktx)
            implementation(libs.sqlite.framework)
        }
    }
}

dependencies {
    add("kspAndroid", libs.room.compiler)
    add("kspIosArm64", libs.room.compiler)
    add("kspIosSimulatorArm64", libs.room.compiler)
}
