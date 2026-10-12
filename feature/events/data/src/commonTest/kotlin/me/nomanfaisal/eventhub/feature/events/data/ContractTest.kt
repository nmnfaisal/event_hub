package me.nomanfaisal.eventhub.feature.events.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/** Rule 6: every captured response shape deserialises, and an empty one is a success. */
class ContractTest {
    @Test
    fun berlinMusicSampleDeserialises() {
        val response = parseFixture("events-berlin-music.json")
        assertEquals(5, response.embedded?.events?.size)
        assertEquals(335, response.page?.totalElements)
    }

    @Test
    fun unfilteredSampleDeserialises() {
        assertEquals(20, parseFixture("events-us-unfiltered.json").embedded?.events?.size)
    }

    @Test
    fun responseWithoutEmbeddedDeserialisesAsNullNotError() {
        val response = parse("""{"page":{"size":50,"totalElements":0,"totalPages":0,"number":0}}""")
        assertNull(response.embedded)
        assertEquals(0, response.page?.totalElements)
    }

    @Test
    fun absentFieldsStayNull() {
        val event = assertNotNull(parse(responseOf("""{"id":"E1"}""")).embedded?.events?.single())
        assertNull(event.name)
        assertNull(event.dates)
        assertNull(event.images)
        assertNull(event.priceRanges)
        assertNull(event.embedded)
    }

    @Test
    fun capturedEmptyResponseIsAnErrorBodyNotAnEventList() {
        // events-empty.json is currently a 404 error body (task 1.2: re-capture a real empty 200).
        // It must still deserialise without throwing and yield no events.
        val response = parseFixture("events-empty.json")
        assertNull(response.embedded)
    }

    @Test
    fun invalidKeyFaultBodyDeserialisesWithoutThrowing() {
        assertNull(parseFixture("error-invalid-key.json").embedded)
    }
}
