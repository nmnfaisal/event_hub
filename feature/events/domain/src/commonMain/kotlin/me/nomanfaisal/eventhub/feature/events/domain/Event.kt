package me.nomanfaisal.eventhub.feature.events.domain

import kotlin.time.Instant
import me.nomanfaisal.eventhub.core.model.EventId
import me.nomanfaisal.eventhub.core.model.Money

/**
 * An upcoming event. Everything the API may omit is nullable; no field holds a
 * placeholder value.
 */
data class Event(
    val id: EventId,
    val name: String,
    val startsAt: Instant,
    /** IANA zone id of the venue, used to show local date and time. */
    val timeZoneId: String?,
    /** False when the API says the time is TBA or the event has no specific time. */
    val hasSpecificTime: Boolean,
    val venueName: String?,
    val heroImageUrl: String?,
    val thumbnailUrl: String?,
    val status: EventStatus,
    /** Identifies the act, so repeated dates of one tour can be collapsed. */
    val attractionKey: String,
    /** Position in the API's relevance ordering (0 = most relevant). */
    val relevanceRank: Int,
    val priceFrom: Money?,
)

enum class EventStatus {
    OnSale,
    OffSale,
    Cancelled,
    Postponed,
    Rescheduled,
    Unknown,
}

enum class EventCategory {
    All,
    Music,
    Sports,
    ArtsAndTheatre,
    Film,
    Miscellaneous,
}
