package me.nomanfaisal.eventhub.di

import org.koin.core.context.startKoin
import org.koin.core.module.Module

fun initKoin(platformModules: List<Module> = emptyList()) {
    startKoin {
        modules(appModule)
        modules(platformModules)
    }
}
