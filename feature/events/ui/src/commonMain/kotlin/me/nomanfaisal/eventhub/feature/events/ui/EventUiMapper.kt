package me.nomanfaisal.eventhub.feature.events.ui

import kotlin.time.Instant
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import me.nomanfaisal.eventhub.core.model.DataError
import me.nomanfaisal.eventhub.core.model.Money
import me.nomanfaisal.eventhub.feature.events.domain.Event
import me.nomanfaisal.eventhub.feature.events.domain.EventStatus
import me.nomanfaisal.eventhub.feature.events.domain.HomeFeed

/** Domain -> presentation. Date text is shown in the event's own time zone. */
internal class EventUiMapper(private val deviceZone: TimeZone) {

    fun toBody(feed: HomeFeed, showRefreshing: Boolean, isAllCategory: Boolean): HomeBody =
        if (feed.isEmpty) {
            HomeBody.Empty(showClearFilters = !isAllCategory)
        } else {
            HomeBody.Content(
                trending = feed.trending.map(::toFeatured),
                comingUp = feed.comingUp.map(::toRow),
                banner = feed.refreshError?.let { BannerUi(bannerKind(it), clockTime(feed.cachedAt)) },
                isRefreshing = showRefreshing && feed.isRefreshing,
            )
        }

    fun toFeatured(event: Event) = FeaturedItemUi(
        id = event.id,
        title = event.name,
        dateChip = dayAndMonth(event),
        subtitle = listOfNotNull(event.venueName, event.priceFrom?.let { "from ${format(it)}" })
            .joinToString(" · ")
            .ifEmpty { null },
        imageUrl = event.heroImageUrl,
    )

    fun toRow(event: Event) = EventRowUi(
        id = event.id,
        title = event.name,
        dateText = if (event.hasSpecificTime) "${dayAndMonth(event)} · ${time(local(event))}" else dayAndMonth(event),
        venueText = event.venueName,
        imageUrl = event.thumbnailUrl ?: event.heroImageUrl,
        badge = badge(event.status),
    )

    fun errorKind(error: DataError): ErrorKind = when (error) {
        DataError.Network.NoInternet, DataError.Network.Timeout -> ErrorKind.Connection
        else -> ErrorKind.Generic
    }

    private fun bannerKind(error: DataError): BannerKind = when (error) {
        DataError.Network.NoInternet, DataError.Network.Timeout -> BannerKind.Offline
        else -> BannerKind.RefreshFailed
    }

    private fun badge(status: EventStatus): String? = when (status) {
        EventStatus.Postponed -> "Postponed"
        EventStatus.Rescheduled -> "Rescheduled"
        EventStatus.OffSale -> "Off sale"
        EventStatus.OnSale, EventStatus.Cancelled, EventStatus.Unknown -> null
    }

    private fun local(event: Event): LocalDateTime {
        val zone = event.timeZoneId?.let { runCatching { TimeZone.of(it) }.getOrNull() } ?: deviceZone
        return event.startsAt.toLocalDateTime(zone)
    }

    private fun dayAndMonth(event: Event): String {
        val d = local(event)
        return "${d.dayOfWeek.shortName()} ${d.day} ${d.month.shortName()}"
    }

    private fun clockTime(instant: Instant): String = time(instant.toLocalDateTime(deviceZone))

    private fun time(d: LocalDateTime): String = "${d.hour.pad()}:${d.minute.pad()}"

    private fun Int.pad() = toString().padStart(2, '0')

    private fun DayOfWeek.shortName() = name.take(3).lowercase().replaceFirstChar { it.uppercase() }

    private fun Month.shortName() = name.take(3).lowercase().replaceFirstChar { it.uppercase() }

    private fun format(money: Money): String {
        val symbol = when (money.currencyCode) {
            "EUR" -> "€"
            "USD" -> "$"
            "GBP" -> "£"
            else -> money.currencyCode + " "
        }
        val whole = money.amountMinor / 100
        val cents = money.amountMinor % 100
        return if (cents == 0L) "$symbol$whole" else "$symbol$whole.${cents.toString().padStart(2, '0')}"
    }
}
