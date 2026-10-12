package me.nomanfaisal.eventhub.feature.events.data

import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readString
import kotlinx.serialization.json.Json
import me.nomanfaisal.eventhub.feature.events.data.dto.EventsResponseDto

internal val testJson = Json { ignoreUnknownKeys = true }

/** Reads a real captured response from docs/api-samples. */
internal fun fixture(name: String): String =
    SystemFileSystem.source(Path("$API_SAMPLES_DIR/$name")).buffered().use { it.readString() }

internal fun parse(json: String): EventsResponseDto = testJson.decodeFromString(json)

internal fun parseFixture(name: String): EventsResponseDto = parse(fixture(name))

/** One minimal valid event, with [extra] replacing the fields after `name`. */
internal fun eventJson(
    id: String? = "E1",
    name: String? = "Name",
    dates: String? = """"dates":{"start":{"dateTime":"2026-10-20T18:00:00Z"},"timezone":"Europe/Berlin","status":{"code":"onsale"}}""",
    extra: String = "",
): String = listOfNotNull(
    id?.let { """"id":"$it"""" },
    name?.let { """"name":"$it"""" },
    dates,
    extra.takeIf { it.isNotBlank() },
).joinToString(",", "{", "}")

internal fun responseOf(vararg events: String): String =
    """{"_embedded":{"events":[${events.joinToString(",")}]},"page":{"totalElements":${events.size}}}"""
