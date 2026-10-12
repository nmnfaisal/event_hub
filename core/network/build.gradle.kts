import java.util.Properties

plugins {
    alias(libs.plugins.eventhub.kmp.library)
    alias(libs.plugins.kotlinx.serialization)
}

val generateApiKey = tasks.register("generateApiKey") {
    val localProps = rootProject.file("local.properties")
    val outputDir = layout.buildDirectory.dir("generated/apikey/kotlin")

    inputs.file(localProps).optional()
    outputs.dir(outputDir)

    doLast {
        val props = Properties()
        if (localProps.exists()) {
            localProps.inputStream().buffered().use { stream -> props.load(stream) }
        }
        val key = props.getProperty("TICKETMASTER_API_KEY") ?: ""
        val file = outputDir.get()
            .file("me/nomanfaisal/eventhub/core/network/ApiKey.kt")
            .asFile
        file.parentFile.mkdirs()
        file.writeText(
            buildString {
                appendLine("package me.nomanfaisal.eventhub.core.network")
                appendLine()
                append("internal const val TICKETMASTER_API_KEY: String = \"")
                append(key.replace("\\", "\\\\").replace("\"", "\\\""))
                appendLine("\"")
            }
        )
    }
}

kotlin {
    sourceSets {
        commonMain {
            kotlin.srcDir(generateApiKey)
            dependencies {
                implementation(project(":core:model"))
                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.content.negotiation)
                implementation(libs.ktor.serialization.json)
                implementation(libs.kotlinx.serialization.json)
            }
        }
        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
    }
}
