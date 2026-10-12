package me.nomanfaisal.eventhub.feature.events.data.db

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Relation

/** One cached event under one category: the same event can sit under All and Music. */
@Entity(tableName = "events", primaryKeys = ["category", "eventId"])
internal data class EventEntity(
    val category: String,
    val eventId: String,
    val name: String,
    val startsAtEpochMs: Long,
    val timeZoneId: String?,
    val hasSpecificTime: Boolean,
    val venueName: String?,
    val heroImageUrl: String?,
    val thumbnailUrl: String?,
    val status: String,
    val attractionKey: String,
    val relevanceRank: Int,
    val priceMinMinor: Long?,
    val priceCurrency: String?,
)

/**
 * Exists once a category has been loaded successfully. Its presence is what separates
 * "never loaded" from "loaded and empty" (Rule 10).
 */
@Entity(tableName = "category_fetch")
internal data class CategoryFetchEntity(
    @PrimaryKey val category: String,
    val fetchedAtEpochMs: Long,
)

/** A category's fetch row together with every event cached under it. */
internal data class CategorySnapshot(
    @Embedded val fetch: CategoryFetchEntity,
    @Relation(parentColumn = "category", entityColumn = "category")
    val events: List<EventEntity>,
) {
    /** Past events are dropped here, on read, not when the data was fetched. */
    fun visibleEvents(nowEpochMs: Long): List<EventEntity> =
        events.filter { it.startsAtEpochMs >= nowEpochMs }.sortedBy { it.relevanceRank }
}
