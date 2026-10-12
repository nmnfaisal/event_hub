package me.nomanfaisal.eventhub.feature.events.data.db

import kotlin.time.Instant
import me.nomanfaisal.eventhub.core.model.EventId
import me.nomanfaisal.eventhub.core.model.Money
import me.nomanfaisal.eventhub.feature.events.domain.Event
import me.nomanfaisal.eventhub.feature.events.domain.EventStatus

internal fun Event.toEntity(category: String) = EventEntity(
    category = category,
    eventId = id.value,
    name = name,
    startsAtEpochMs = startsAt.toEpochMilliseconds(),
    timeZoneId = timeZoneId,
    hasSpecificTime = hasSpecificTime,
    venueName = venueName,
    heroImageUrl = heroImageUrl,
    thumbnailUrl = thumbnailUrl,
    status = status.name,
    attractionKey = attractionKey,
    relevanceRank = relevanceRank,
    priceMinMinor = priceFrom?.amountMinor,
    priceCurrency = priceFrom?.currencyCode,
)

internal fun EventEntity.toDomain() = Event(
    id = EventId(eventId),
    name = name,
    startsAt = Instant.fromEpochMilliseconds(startsAtEpochMs),
    timeZoneId = timeZoneId,
    hasSpecificTime = hasSpecificTime,
    venueName = venueName,
    heroImageUrl = heroImageUrl,
    thumbnailUrl = thumbnailUrl,
    status = EventStatus.entries.firstOrNull { it.name == status } ?: EventStatus.Unknown,
    attractionKey = attractionKey,
    relevanceRank = relevanceRank,
    priceFrom = if (priceMinMinor != null && priceCurrency != null) Money(priceMinMinor, priceCurrency) else null,
)
