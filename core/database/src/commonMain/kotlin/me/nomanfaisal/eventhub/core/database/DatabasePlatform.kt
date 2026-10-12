package me.nomanfaisal.eventhub.core.database

import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers

/**
 * What a feature's `Room.databaseBuilder` needs from the platform: an Android `Context` or
 * the iOS documents directory. Feature `:data` modules call `Room.databaseBuilder` themselves
 * (it is reified over their own `@Database`) and pass the result through [withAppDefaults].
 */
expect class DatabasePlatform {
    /** Absolute path of the database file called [name]. */
    fun filePath(name: String): String
}

/** The one driver and dispatcher choice for every feature database. */
fun <T : RoomDatabase> RoomDatabase.Builder<T>.withAppDefaults(): RoomDatabase.Builder<T> =
    setDriver(BundledSQLiteDriver()).setQueryCoroutineContext(Dispatchers.Default)
