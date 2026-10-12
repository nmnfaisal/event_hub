package me.nomanfaisal.eventhub.feature.events.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Every field is nullable with a null default (Rule 2): the API omits fields freely and a
// missing value must stay distinguishable from an empty one. Lists default to null, not
// emptyList(). Only fields the mapper reads are declared; unknown keys are ignored.

@Serializable
internal data class EventsResponseDto(
    @SerialName("_embedded") val embedded: EventsEmbeddedDto? = null,
    val page: PageDto? = null,
)

@Serializable
internal data class PageDto(
    val size: Int? = null,
    val totalElements: Int? = null,
    val totalPages: Int? = null,
    val number: Int? = null,
)

@Serializable
internal data class EventsEmbeddedDto(
    val events: List<EventDto>? = null,
)

@Serializable
internal data class EventDto(
    val id: String? = null,
    val name: String? = null,
    val dates: DatesDto? = null,
    val images: List<ImageDto>? = null,
    val classifications: List<ClassificationDto>? = null,
    val priceRanges: List<PriceRangeDto>? = null,
    @SerialName("_embedded") val embedded: EventEmbeddedDto? = null,
)

@Serializable
internal data class DatesDto(
    val start: StartDto? = null,
    val timezone: String? = null,
    val status: StatusDto? = null,
)

@Serializable
internal data class StartDto(
    val localDate: String? = null,
    val localTime: String? = null,
    val dateTime: String? = null,
    val timeTBA: Boolean? = null,
    val noSpecificTime: Boolean? = null,
)

@Serializable
internal data class StatusDto(val code: String? = null)

@Serializable
internal data class ImageDto(
    val ratio: String? = null,
    val url: String? = null,
    val width: Int? = null,
    val height: Int? = null,
    val fallback: Boolean? = null,
)

@Serializable
internal data class ClassificationDto(
    val primary: Boolean? = null,
    val segment: NamedDto? = null,
    val genre: NamedDto? = null,
)

@Serializable
internal data class NamedDto(val id: String? = null, val name: String? = null)

@Serializable
internal data class PriceRangeDto(
    val type: String? = null,
    val currency: String? = null,
    val min: Double? = null,
    val max: Double? = null,
)

@Serializable
internal data class EventEmbeddedDto(
    val venues: List<VenueDto>? = null,
    val attractions: List<AttractionDto>? = null,
)

@Serializable
internal data class VenueDto(
    val id: String? = null,
    val name: String? = null,
    val address: AddressDto? = null,
    val timezone: String? = null,
)

@Serializable
internal data class AddressDto(val line1: String? = null)

@Serializable
internal data class AttractionDto(val id: String? = null, val name: String? = null)
