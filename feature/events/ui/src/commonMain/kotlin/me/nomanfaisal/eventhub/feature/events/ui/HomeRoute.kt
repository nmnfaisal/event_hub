package me.nomanfaisal.eventhub.feature.events.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.nomanfaisal.eventhub.core.model.EventId
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val eventsUiModule: Module = module {
    viewModel { EventsViewModel(get()) }
}

@Composable
fun HomeRoute(
    onSearchClick: () -> Unit,
    onSeeAllClick: () -> Unit,
    onLocationClick: () -> Unit,
    onEventClick: (EventId) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EventsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    HomeScreen(
        state = state,
        onAction = viewModel::onAction,
        onSearchClick = onSearchClick,
        onSeeAllClick = onSeeAllClick,
        onLocationClick = onLocationClick,
        onEventClick = onEventClick,
        modifier = modifier,
    )
}
