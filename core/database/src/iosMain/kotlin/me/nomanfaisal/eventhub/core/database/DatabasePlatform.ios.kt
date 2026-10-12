package me.nomanfaisal.eventhub.core.database

import kotlinx.cinterop.ExperimentalForeignApi
import org.koin.core.scope.Scope
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

actual class DatabasePlatform {
    @OptIn(ExperimentalForeignApi::class)
    actual fun filePath(name: String): String {
        val documents = NSFileManager.defaultManager.URLForDirectory(
            directory = NSDocumentDirectory,
            inDomain = NSUserDomainMask,
            appropriateForURL = null,
            create = false,
            error = null,
        )
        return requireNotNull(documents?.path) + "/" + name
    }
}

internal actual fun Scope.createDatabasePlatform(): DatabasePlatform = DatabasePlatform()
