package me.nomanfaisal.eventhub.feature.events.ui

import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import me.nomanfaisal.eventhub.core.model.DataError
import me.nomanfaisal.eventhub.core.model.EventId
import me.nomanfaisal.eventhub.core.model.Money
import me.nomanfaisal.eventhub.feature.events.domain.Event
import me.nomanfaisal.eventhub.feature.events.domain.EventStatus
import me.nomanfaisal.eventhub.feature.events.domain.HomeFeed
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class EventUiMapperTest {
    private val mapper = EventUiMapper(deviceZone = TimeZone.of("Europe/Berlin"))

    private fun event(
        startsAt: String = "2026-10-13T17:30:00Z",
        zone: String? = "Europe/Berlin",
        venue: String? = "Konzertsaal Mitte",
        status: EventStatus = EventStatus.OnSale,
        price: Money? = null,
        hasTime: Boolean = true,
    ) = Event(
        id = EventId("e1"), name = "Film Scores Live", startsAt = Instant.parse(startsAt), timeZoneId = zone,
        hasSpecificTime = hasTime, venueName = venue, heroImageUrl = "hero", thumbnailUrl = null,
        status = status, attractionKey = "a", relevanceRank = 0, priceFrom = price,
    )

    @Test
    fun rowShowsDateAndTimeInEventTimeZone() {
        val row = mapper.toRow(event())
        assertEquals("Tue 13 Oct · 19:30", row.dateText)
        assertEquals("Konzertsaal Mitte", row.venueText)
    }

    @Test
    fun rowOmitsTimeWhenNoSpecificTime() {
        assertEquals("Tue 13 Oct", mapper.toRow(event(hasTime = false)).dateText)
    }

    @Test
    fun rowFallsBackToDeviceZoneForUnknownZone() {
        assertEquals("Tue 13 Oct · 19:30", mapper.toRow(event(zone = "Not/AZone")).dateText)
    }

    @Test
    fun thumbnailFallsBackToHero() {
        assertEquals("hero", mapper.toRow(event()).imageUrl)
    }

    @Test
    fun statusBadgeOnlyForChangedEvents() {
        assertEquals("Postponed", mapper.toRow(event(status = EventStatus.Postponed)).badge)
        assertEquals("Rescheduled", mapper.toRow(event(status = EventStatus.Rescheduled)).badge)
        assertEquals("Off sale", mapper.toRow(event(status = EventStatus.OffSale)).badge)
        assertNull(mapper.toRow(event(status = EventStatus.OnSale)).badge)
        assertNull(mapper.toRow(event(status = EventStatus.Unknown)).badge)
    }

    @Test
    fun featuredSubtitleIsVenueOnlyWithoutPrice() {
        val card = mapper.toFeatured(event(price = null))
        assertEquals("Konzertsaal Mitte", card.subtitle)
        assertEquals("Tue 13 Oct", card.dateChip)
    }

    @Test
    fun featuredSubtitleAddsPriceOnlyWhenPresent() {
        assertEquals("Konzertsaal Mitte · from €39", mapper.toFeatured(event(price = Money(3900, "EUR"))).subtitle)
        assertEquals("from €39.50", mapper.toFeatured(event(venue = null, price = Money(3950, "EUR"))).subtitle)
    }

    @Test
    fun featuredSubtitleIsNullWithNeitherVenueNorPrice() {
        assertNull(mapper.toFeatured(event(venue = null)).subtitle)
    }

    @Test
    fun bannerWordingDependsOnErrorAndShowsFetchTimeInDeviceZone() {
        val cachedAt = Instant.parse("2026-10-12T12:32:00Z") // 14:32 in Berlin
        fun body(error: DataError?) = mapper.toBody(
            HomeFeed(listOf(event()), emptyList(), cachedAt, error, isRefreshing = false),
            showRefreshing = false, isAllCategory = true,
        ) as HomeBody.Content

        assertEquals(BannerUi(BannerKind.Offline, "14:32"), body(DataError.Network.NoInternet).banner)
        assertEquals(BannerUi(BannerKind.Offline, "14:32"), body(DataError.Network.Timeout).banner)
        assertEquals(BannerUi(BannerKind.RefreshFailed, "14:32"), body(DataError.Network.Server).banner)
        assertNull(body(null).banner)
        assertEquals("Offline. Showing events from 14:32.", HomeStrings.bannerText(BannerKind.Offline, "14:32"))
    }

    @Test
    fun emptyFeedMapsToEmptyAndClearFiltersOnlyOffAll() {
        val empty = HomeFeed(emptyList(), emptyList(), Instant.parse("2026-10-12T12:00:00Z"), null)
        assertEquals(HomeBody.Empty(showClearFilters = true), mapper.toBody(empty, false, isAllCategory = false))
        assertEquals(HomeBody.Empty(showClearFilters = false), mapper.toBody(empty, false, isAllCategory = true))
    }

    @Test
    fun refreshIndicatorOnlyForUserInitiatedRefresh() {
        val feed = HomeFeed(emptyList(), listOf(event()), Instant.parse("2026-10-12T12:00:00Z"), null, isRefreshing = true)
        assertTrue((mapper.toBody(feed, showRefreshing = true, isAllCategory = true) as HomeBody.Content).isRefreshing)
        assertEquals(false, (mapper.toBody(feed, showRefreshing = false, isAllCategory = true) as HomeBody.Content).isRefreshing)
    }

    @Test
    fun errorKindSeparatesConnectionFromGeneric() {
        assertEquals(ErrorKind.Connection, mapper.errorKind(DataError.Network.NoInternet))
        assertEquals(ErrorKind.Connection, mapper.errorKind(DataError.Network.Timeout))
        assertEquals(ErrorKind.Generic, mapper.errorKind(DataError.Network.Server))
        assertEquals(ErrorKind.Generic, mapper.errorKind(DataError.Network.TooManyRequests))
    }
}
