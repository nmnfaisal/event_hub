package me.nomanfaisal.eventhub.feature.events.data

import kotlin.time.Instant
import me.nomanfaisal.eventhub.core.model.EventId
import me.nomanfaisal.eventhub.core.model.Money
import me.nomanfaisal.eventhub.feature.events.domain.EventStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class EventMapperTest {
    private fun events(vararg json: String) = parse(responseOf(*json)).toEvents()

    @Test // AC-3
    fun berlinSampleParsesCompletelyInOrder() {
        val events = parseFixture("events-berlin-music.json").toEvents()
        assertEquals(5, events.size)
        assertEquals(
            listOf("Z698xZC2Z16vxkJ3ut", "Z698xZC2Z1kG3GyZZ", "Z698xZC2Z1krp47AN", "Z698xZC2Z1k4o-Aap", "Z698xZC2Z16v194Y1A"),
            events.map { it.id.value },
        )
        assertEquals(listOf(0, 1, 2, 3, 4), events.map { it.relevanceRank })
    }

    @Test // AC-4
    fun unfilteredSampleParsesCompletely() {
        assertEquals(20, parseFixture("events-us-unfiltered.json").toEvents().size)
    }

    @Test // AC-5
    fun responseWithoutEmbeddedIsEmptyList() {
        assertEquals(emptyList(), parse("""{"page":{"totalElements":0}}""").toEvents())
    }

    @Test // AC-6
    fun missingVenueNameFallsBackToAddressLine() {
        val first = parseFixture("events-berlin-music.json").toEvents().first()
        assertEquals("Nollendorfplatz 5", first.venueName)
    }

    @Test
    fun venueNameWinsOverAddress() {
        val third = parseFixture("events-berlin-music.json").toEvents()[2]
        assertEquals("Columbia Theater", third.venueName)
    }

    @Test
    fun noVenueAtAllLeavesVenueNull() {
        assertNull(events(eventJson()).single().venueName)
    }

    @Test // AC-7
    fun eventsWithoutIdOrWithBlankNameAreDropped() {
        val result = events(eventJson(id = null), eventJson(id = "B", name = " "), eventJson(id = "OK"))
        assertEquals(listOf(EventId("OK")), result.map { it.id })
    }

    @Test // AC-8
    fun eventWithoutAnyDateIsDropped() {
        val noDate = eventJson(dates = """"dates":{"timezone":"Europe/Berlin"}""")
        assertTrue(events(noDate).isEmpty())
        assertTrue(events(eventJson(dates = null)).isEmpty())
    }

    @Test // AC-9
    fun startDerivedFromLocalDateAndTime() {
        val dates = """"dates":{"start":{"localDate":"2026-10-20","localTime":"20:00:00"},"timezone":"Europe/Berlin"}"""
        val event = events(eventJson(dates = dates)).single()
        assertEquals(Instant.parse("2026-10-20T18:00:00Z"), event.startsAt)
        assertTrue(event.hasSpecificTime)
    }

    @Test
    fun localDateOnlyUsesStartOfDayWithoutSpecificTime() {
        val dates = """"dates":{"start":{"localDate":"2026-10-20"},"timezone":"Europe/Berlin"}"""
        val event = events(eventJson(dates = dates)).single()
        assertEquals(Instant.parse("2026-10-19T22:00:00Z"), event.startsAt)
        assertFalse(event.hasSpecificTime)
    }

    @Test
    fun localFallbackWithoutTimezoneIsDropped() {
        val dates = """"dates":{"start":{"localDate":"2026-10-20","localTime":"20:00:00"}}"""
        assertTrue(events(eventJson(dates = dates)).isEmpty())
    }

    @Test
    fun noSpecificTimeFlagHidesTime() {
        val dates = """"dates":{"start":{"dateTime":"2026-10-20T18:00:00Z","noSpecificTime":true},"timezone":"Europe/Berlin"}"""
        assertFalse(events(eventJson(dates = dates)).single().hasSpecificTime)
    }

    @Test // AC-10
    fun bothCancelledSpellingsAreCancelled() {
        fun status(code: String) = eventJson(
            dates = """"dates":{"start":{"dateTime":"2026-10-20T18:00:00Z"},"status":{"code":"$code"}}""",
        )
        assertEquals(
            listOf(EventStatus.Cancelled, EventStatus.Cancelled),
            events(status("cancelled"), status("canceled")).map { it.status },
        )
        assertEquals(EventStatus.Postponed, events(status("postponed")).single().status)
        assertEquals(EventStatus.OffSale, events(status("offsale")).single().status)
        assertEquals(EventStatus.Unknown, events(status("somethingnew")).single().status)
    }

    @Test
    fun missingStatusIsUnknown() {
        val dates = """"dates":{"start":{"dateTime":"2026-10-20T18:00:00Z"}}"""
        assertEquals(EventStatus.Unknown, events(eventJson(dates = dates)).single().status)
    }

    @Test // AC-11
    fun heroIsSmallestQualifying16x9AndRatiolessImageIsIgnored() {
        val first = parseFixture("events-berlin-music.json").toEvents().first()
        assertTrue(first.heroImageUrl!!.endsWith("TABLET_LANDSCAPE_16_9.jpg"), first.heroImageUrl)
        assertTrue(first.thumbnailUrl!!.endsWith("ARTIST_PAGE_3_2.jpg"), first.thumbnailUrl)
    }

    @Test
    fun noImagesLeavesUrlsNull() {
        val event = events(eventJson()).single()
        assertNull(event.heroImageUrl)
        assertNull(event.thumbnailUrl)
    }

    @Test
    fun thumbnailFallsBackToHeroWhenNoSquareishImage() {
        val images = """"images":[{"ratio":"16_9","url":"https://x/hero.jpg","width":1024,"height":576}]"""
        val event = events(eventJson(extra = images)).single()
        assertEquals("https://x/hero.jpg", event.heroImageUrl)
        assertEquals("https://x/hero.jpg", event.thumbnailUrl)
    }

    @Test // AC-12
    fun primaryClassificationWinsOverOrder() {
        val classifications = """"classifications":[
            {"primary":false,"segment":{"name":"Music"},"genre":{"name":"Rock"}},
            {"primary":true,"segment":{"name":"Arts & Theatre"},"genre":{"name":"Folk"}}]"""
        val dto = parse(responseOf(eventJson(extra = classifications))).embedded!!.events!!.single()
        assertEquals("Arts & Theatre", dto.primaryClassificationName())
    }

    @Test
    fun noClassificationsGivesNull() {
        val dto = parse(responseOf(eventJson())).embedded!!.events!!.single()
        assertNull(dto.primaryClassificationName())
    }

    @Test // AC-13
    fun noPriceRangesMeansNoPrice() {
        assertTrue(parseFixture("events-berlin-music.json").toEvents().all { it.priceFrom == null })
    }

    @Test
    fun priceIsCheapestRangeInMinorUnits() {
        val prices = """"priceRanges":[{"currency":"EUR","min":39.5,"max":80},{"currency":"EUR","min":25,"max":60},{"min":1}]"""
        assertEquals(Money(2500, "EUR"), events(eventJson(extra = prices)).single().priceFrom)
    }

    @Test
    fun attractionKeyUsesAttractionIdThenEventName() {
        val withAttraction = eventJson(extra = """"_embedded":{"attractions":[{"id":"A9","name":"Act"}]}""")
        assertEquals("A9", events(withAttraction).single().attractionKey)
        assertEquals("Name", events(eventJson()).single().attractionKey)
    }
}
