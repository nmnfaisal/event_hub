package me.nomanfaisal.eventhub.feature.events.data

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import me.nomanfaisal.eventhub.core.model.DataError
import me.nomanfaisal.eventhub.core.model.Result
import me.nomanfaisal.eventhub.core.network.safeCall
import me.nomanfaisal.eventhub.feature.events.data.dto.EventsResponseDto
import me.nomanfaisal.eventhub.feature.events.domain.EventCategory

/** Ticketmaster Discovery v2 `events.json`. Base URL and `apikey` come from the shared client. */
internal class TicketmasterEventsApi(private val client: HttpClient) {

    suspend fun fetchEvents(
        category: EventCategory,
        now: Instant,
    ): Result<EventsResponseDto, DataError.Network> = safeCall {
        client.get("events.json") {
            parameter("city", CITY)
            parameter("countryCode", COUNTRY_CODE)
            category.classificationName?.let { parameter("classificationName", it) }
            parameter("sort", "relevance,desc")
            parameter("size", PAGE_SIZE)
            parameter("page", 0)
            parameter("startDateTime", now.toApiDateTime())
        }
    }

    private companion object {
        const val CITY = "Berlin"
        const val COUNTRY_CODE = "DE"
        const val PAGE_SIZE = 50
    }
}

/** `null` for All: the parameter is omitted. */
internal val EventCategory.classificationName: String?
    get() = when (this) {
        EventCategory.All -> null
        EventCategory.Music -> "Music"
        EventCategory.Sports -> "Sports"
        EventCategory.ArtsAndTheatre -> "Arts & Theatre"
        EventCategory.Film -> "Film"
        EventCategory.Miscellaneous -> "Miscellaneous"
    }

/** `yyyy-MM-ddTHH:mm:ssZ` in UTC with no fraction, the only format the API accepts. */
internal fun Instant.toApiDateTime(): String {
    val t = toLocalDateTime(TimeZone.UTC)
    fun Int.p(width: Int = 2) = toString().padStart(width, '0')
    return "${t.year.p(4)}-${t.monthNumber.p()}-${t.dayOfMonth.p()}T${t.hour.p()}:${t.minute.p()}:${t.second.p()}Z"
}
