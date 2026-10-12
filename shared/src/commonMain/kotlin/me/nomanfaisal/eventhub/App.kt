package me.nomanfaisal.eventhub

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import me.nomanfaisal.eventhub.core.ui.components.BottomNavBar
import me.nomanfaisal.eventhub.core.ui.components.SetUpImageLoading
import me.nomanfaisal.eventhub.core.ui.theme.AppIcon
import me.nomanfaisal.eventhub.core.ui.theme.EventHubTheme
import me.nomanfaisal.eventhub.feature.events.ui.HomeRoute

private val TAB_LABELS = listOf("Home", "Search", "Saved", "Tickets")
private val TAB_ICONS = listOf(AppIcon.Home, AppIcon.Search, AppIcon.Saved, AppIcon.Tickets)
private const val HOME_TAB = 0
private const val SEARCH_TAB = 1

/** Owns the tab shell; features know nothing about each other (ADR-001). */
@Composable
fun App() {
    SetUpImageLoading()
    EventHubTheme {
        var selectedTab by rememberSaveable { mutableIntStateOf(HOME_TAB) }
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                BottomNavBar(
                    labels = TAB_LABELS,
                    icons = TAB_ICONS,
                    selectedIndex = selectedTab,
                    onSelect = { selectedTab = it },
                )
            },
        ) { padding ->
            when (selectedTab) {
                HOME_TAB -> HomeRoute(
                    onSearchClick = { selectedTab = SEARCH_TAB },
                    onSeeAllClick = { selectedTab = SEARCH_TAB },
                    onLocationClick = {},
                    onEventClick = {},
                    modifier = Modifier.padding(padding),
                )
                else -> PlaceholderTab(TAB_LABELS[selectedTab], Modifier.padding(padding))
            }
        }
    }
}

@Composable
private fun PlaceholderTab(name: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = "$name is coming soon", style = MaterialTheme.typography.bodyLarge)
    }
}
