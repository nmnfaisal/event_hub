package me.nomanfaisal.eventhub.di

import me.nomanfaisal.eventhub.feature.events.domain.GetEventsUseCase
import org.koin.core.module.Module
import org.koin.dsl.module

/** Cross-feature wiring. Domain stays free of Koin, so its use cases are bound here. */
val appModule: Module = module {
    factory { GetEventsUseCase(get()) }
}
