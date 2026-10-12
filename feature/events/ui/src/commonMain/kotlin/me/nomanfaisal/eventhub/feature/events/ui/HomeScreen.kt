package me.nomanfaisal.eventhub.feature.events.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import me.nomanfaisal.eventhub.core.model.EventId
import me.nomanfaisal.eventhub.core.ui.components.AppHeader
import me.nomanfaisal.eventhub.core.ui.components.CategoryChipRow
import me.nomanfaisal.eventhub.core.ui.components.EmptyView
import me.nomanfaisal.eventhub.core.ui.components.ErrorView
import me.nomanfaisal.eventhub.core.ui.components.EventRow
import me.nomanfaisal.eventhub.core.ui.components.FeaturedCard
import me.nomanfaisal.eventhub.core.ui.components.OfflineBanner
import me.nomanfaisal.eventhub.core.ui.components.SearchBarButton
import me.nomanfaisal.eventhub.core.ui.components.SectionHeader
import me.nomanfaisal.eventhub.core.ui.components.SkeletonBox
import me.nomanfaisal.eventhub.feature.events.domain.EventCategory

/** Stateless Home screen: header, title, search, chips, then exactly one body. */
@Composable
fun HomeScreen(
    state: HomeUiState,
    onAction: (HomeAction) -> Unit,
    onSearchClick: () -> Unit,
    onSeeAllClick: () -> Unit,
    onLocationClick: () -> Unit,
    onEventClick: (EventId) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            AppHeader(HomeStrings.APP_TITLE, HomeStrings.LOCATION, onLocationClick)
            Text(text = HomeStrings.HEADLINE, style = MaterialTheme.typography.displayLarge)
            SearchBarButton(HomeStrings.SEARCH_PLACEHOLDER, onSearchClick)
            CategoryChipRow(
                labels = state.chips,
                selectedIndex = state.selectedIndex,
                onSelect = { onAction(HomeAction.SelectCategory(EventCategory.entries[it])) },
            )
        }
        when (val body = state.body) {
            HomeBody.Loading -> HomeSkeleton(Modifier.padding(horizontal = 20.dp, vertical = 16.dp))
            is HomeBody.Content -> HomeContent(body, onAction, onSeeAllClick, onEventClick)
            is HomeBody.Empty -> EmptyView(
                title = HomeStrings.EMPTY_TITLE,
                message = HomeStrings.EMPTY_MESSAGE,
                actionLabel = if (body.showClearFilters) HomeStrings.CLEAR_FILTERS else null,
                onAction = { onAction(HomeAction.ClearFilters) },
            )
            is HomeBody.Error -> ErrorView(
                title = HomeStrings.ERROR_TITLE,
                message = HomeStrings.errorMessage(body.kind),
                actionLabel = HomeStrings.RETRY,
                onRetry = { onAction(HomeAction.Retry) },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeContent(
    body: HomeBody.Content,
    onAction: (HomeAction) -> Unit,
    onSeeAllClick: () -> Unit,
    onEventClick: (EventId) -> Unit,
) {
    PullToRefreshBox(
        isRefreshing = body.isRefreshing,
        onRefresh = { onAction(HomeAction.Refresh) },
        modifier = Modifier.fillMaxSize(),
    ) {
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            val banner = body.banner
            if (banner != null) {
                item(key = "banner") {
                    OfflineBanner(
                        text = HomeStrings.bannerText(banner.kind, banner.time),
                        actionLabel = HomeStrings.RETRY,
                        onAction = { onAction(HomeAction.Refresh) },
                    )
                }
            }
            if (body.trending.isNotEmpty()) {
                item(key = "trending-header") {
                    SectionHeader(HomeStrings.TRENDING, actionLabel = HomeStrings.SEE_ALL, onAction = onSeeAllClick)
                }
                item(key = "trending") {
                    val listState = rememberLazyListState()
                    LazyRow(
                        state = listState,
                        flingBehavior = rememberSnapFlingBehavior(listState),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        items(body.trending, key = { it.id.value }) { item ->
                            FeaturedCard(
                                title = item.title,
                                dateChip = item.dateChip,
                                subtitle = item.subtitle,
                                imageUrl = item.imageUrl,
                                onClick = { onEventClick(item.id) },
                                modifier = Modifier.width(250.dp),
                            )
                        }
                    }
                }
            }
            if (body.comingUp.isNotEmpty()) {
                item(key = "coming-up-header") { SectionHeader(HomeStrings.COMING_UP) }
                items(body.comingUp, key = { "row-" + it.id.value }) { row ->
                    EventRow(
                        title = row.title,
                        dateText = row.dateText,
                        venueText = row.venueText,
                        imageUrl = row.imageUrl,
                        badge = row.badge,
                        onClick = { onEventClick(row.id) },
                    )
                }
            }
        }
    }
}

/** Placeholder shaped like the content: one large card and four rows. */
@Composable
fun HomeSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) { contentDescription = HomeStrings.LOADING_DESCRIPTION },
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SkeletonBox(Modifier.fillMaxWidth().height(176.dp), cornerRadius = 20.dp)
        SkeletonBox(Modifier.width(220.dp).height(14.dp), cornerRadius = 7.dp)
        SkeletonBox(Modifier.width(140.dp).height(14.dp), cornerRadius = 7.dp)
        repeat(4) {
            SkeletonBox(Modifier.fillMaxWidth().height(84.dp), cornerRadius = 20.dp)
        }
    }
}
