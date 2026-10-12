package me.nomanfaisal.eventhub.feature.events.data

import kotlin.math.roundToLong
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toInstant
import me.nomanfaisal.eventhub.core.model.EventId
import me.nomanfaisal.eventhub.core.model.Money
import me.nomanfaisal.eventhub.feature.events.data.dto.EventDto
import me.nomanfaisal.eventhub.feature.events.data.dto.EventsResponseDto
import me.nomanfaisal.eventhub.feature.events.data.dto.ImageDto
import me.nomanfaisal.eventhub.feature.events.domain.Event
import me.nomanfaisal.eventhub.feature.events.domain.EventStatus

private const val HERO_MIN_WIDTH = 1024
private const val THUMB_MIN_WIDTH = 300

/**
 * Turns a response into events. A response without `_embedded` is an empty list (Ticketmaster
 * omits it when nothing matches). Entries that cannot be identified or placed in time are
 * dropped; optional values stay null rather than being filled with placeholders.
 */
internal fun EventsResponseDto.toEvents(): List<Event> =
    embedded?.events.orEmpty()
        .mapNotNull { it.toEvent() }
        .mapIndexed { rank, event -> event.copy(relevanceRank = rank) }

internal fun EventDto.toEvent(): Event? {
    val id = id?.takeUnless { it.isBlank() } ?: return null
    val name = name?.takeUnless { it.isBlank() } ?: return null
    val timeZoneId = dates?.timezone ?: embedded?.venues?.firstOrNull()?.timezone
    val start = dates?.start?.resolveStart(timeZoneId) ?: return null
    val venue = embedded?.venues?.firstOrNull()
    val hero = images.heroImage()

    return Event(
        id = EventId(id),
        name = name,
        startsAt = start.instant,
        timeZoneId = timeZoneId,
        hasSpecificTime = start.hasSpecificTime,
        venueName = venue?.name?.takeUnless { it.isBlank() }
            ?: venue?.address?.line1?.takeUnless { it.isBlank() },
        heroImageUrl = hero?.url,
        thumbnailUrl = images.thumbnailImage()?.url ?: hero?.url,
        status = dates?.status?.code.toEventStatus(),
        attractionKey = embedded?.attractions?.firstOrNull()?.let { it.id ?: it.name } ?: name,
        relevanceRank = 0,
        priceFrom = priceRanges.cheapest(),
    )
}

/** Name of the primary classification segment, falling back to the first one. */
internal fun EventDto.primaryClassificationName(): String? {
    val chosen = classifications?.firstOrNull { it.primary == true } ?: classifications?.firstOrNull()
    return chosen?.segment?.name ?: chosen?.genre?.name
}

private class ResolvedStart(val instant: Instant, val hasSpecificTime: Boolean)

private fun me.nomanfaisal.eventhub.feature.events.data.dto.StartDto.resolveStart(
    timeZoneId: String?,
): ResolvedStart? {
    val specific = noSpecificTime != true && timeTBA != true
    dateTime?.let { text ->
        runCatching { Instant.parse(text) }.getOrNull()?.let { return ResolvedStart(it, specific) }
    }
    val zone = timeZoneId?.let { runCatching { TimeZone.of(it) }.getOrNull() } ?: return null
    val date = localDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: return null
    val time = localTime?.let { runCatching { LocalTime.parse(it) }.getOrNull() }
        ?: return ResolvedStart(date.atStartOfDayIn(zone), hasSpecificTime = false)
    return ResolvedStart(LocalDateTime(date, time).toInstant(zone), specific)
}

private fun String?.toEventStatus(): EventStatus = when (this?.lowercase()) {
    "onsale" -> EventStatus.OnSale
    "offsale" -> EventStatus.OffSale
    "cancelled", "canceled" -> EventStatus.Cancelled
    "postponed" -> EventStatus.Postponed
    "rescheduled" -> EventStatus.Rescheduled
    else -> EventStatus.Unknown
}

/** Smallest sized 16:9 image of at least 1024px. Images without a ratio are never used. */
private fun List<ImageDto>?.heroImage(): ImageDto? =
    this.orEmpty()
        .filter { it.ratio == "16_9" && it.fallback != true && it.url != null && (it.width ?: 0) >= HERO_MIN_WIDTH }
        .minByOrNull { it.width ?: Int.MAX_VALUE }

/** Smallest 4:3 or 3:2 image of at least 300px; cropped square by the UI. */
private fun List<ImageDto>?.thumbnailImage(): ImageDto? =
    this.orEmpty()
        .filter {
            (it.ratio == "4_3" || it.ratio == "3_2") && it.fallback != true &&
                it.url != null && (it.width ?: 0) >= THUMB_MIN_WIDTH
        }
        .minByOrNull { it.width ?: Int.MAX_VALUE }

private fun List<me.nomanfaisal.eventhub.feature.events.data.dto.PriceRangeDto>?.cheapest(): Money? =
    this.orEmpty()
        .mapNotNull { range ->
            val min = range.min ?: return@mapNotNull null
            val currency = range.currency ?: return@mapNotNull null
            Money((min * 100).roundToLong(), currency)
        }
        .minByOrNull { it.amountMinor }
