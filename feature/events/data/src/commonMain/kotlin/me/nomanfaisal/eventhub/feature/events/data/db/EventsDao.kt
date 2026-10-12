package me.nomanfaisal.eventhub.feature.events.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
internal interface EventsDao {
    /**
     * The fetch row and its events in one query, so a reader never sees the new fetch row
     * with the previous (possibly empty) events. Null until the category is first loaded.
     */
    @Transaction
    @Query("SELECT * FROM category_fetch WHERE category = :category")
    fun observeSnapshot(category: String): Flow<CategorySnapshot?>

    @Query("SELECT * FROM category_fetch WHERE category = :category")
    suspend fun fetchRow(category: String): CategoryFetchEntity?

    @Query("DELETE FROM events WHERE category = :category")
    suspend fun deleteEvents(category: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvents(events: List<EventEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFetch(row: CategoryFetchEntity)

    /** Replaces one category only; other categories and their fetch times are untouched. */
    @Transaction
    suspend fun replaceCategory(category: String, events: List<EventEntity>, fetchedAtEpochMs: Long) {
        deleteEvents(category)
        insertEvents(events)
        upsertFetch(CategoryFetchEntity(category, fetchedAtEpochMs))
    }
}
