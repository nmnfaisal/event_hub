package me.nomanfaisal.eventhub.core.database

import android.content.Context
import org.koin.android.ext.koin.androidContext
import org.koin.core.scope.Scope

actual class DatabasePlatform(val context: Context) {
    actual fun filePath(name: String): String = context.getDatabasePath(name).absolutePath
}

internal actual fun Scope.createDatabasePlatform(): DatabasePlatform = DatabasePlatform(androidContext())
