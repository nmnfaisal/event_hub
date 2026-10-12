package me.nomanfaisal.eventhub.di

import androidx.test.ext.junit.runners.AndroidJUnit4
import me.nomanfaisal.eventhub.core.database.databaseModule
import me.nomanfaisal.eventhub.core.network.networkModule
import me.nomanfaisal.eventhub.feature.events.data.eventsDataModule
import me.nomanfaisal.eventhub.feature.events.domain.EventRepository
import me.nomanfaisal.eventhub.feature.events.domain.GetEventsUseCase
import me.nomanfaisal.eventhub.feature.events.ui.EventsViewModel
import me.nomanfaisal.eventhub.feature.events.ui.eventsUiModule
import org.junit.runner.RunWith
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.koinApplication
import org.robolectric.RuntimeEnvironment
import kotlin.test.Test
import kotlin.test.assertNotNull

/** The assembled graph resolves everything Home needs, with no missing bindings. */
@RunWith(AndroidJUnit4::class)
class KoinGraphTest {
    @Test
    fun homeGraphResolves() {
        val koin = koinApplication {
            androidContext(RuntimeEnvironment.getApplication())
            modules(appModule, networkModule(isDebug = false), databaseModule, eventsDataModule, eventsUiModule)
        }.koin

        assertNotNull(koin.get<EventRepository>())
        assertNotNull(koin.get<GetEventsUseCase>())
        assertNotNull(koin.get<EventsViewModel>())
    }
}
