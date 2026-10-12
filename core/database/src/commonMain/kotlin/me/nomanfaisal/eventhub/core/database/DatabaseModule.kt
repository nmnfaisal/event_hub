package me.nomanfaisal.eventhub.core.database

import org.koin.core.module.Module
import org.koin.core.scope.Scope
import org.koin.dsl.module

internal expect fun Scope.createDatabasePlatform(): DatabasePlatform

val databaseModule: Module = module {
    single { createDatabasePlatform() }
}
