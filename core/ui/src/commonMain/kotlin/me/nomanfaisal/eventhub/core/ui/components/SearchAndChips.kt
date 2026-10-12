package me.nomanfaisal.eventhub.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import org.jetbrains.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import me.nomanfaisal.eventhub.core.ui.theme.AppIcon
import me.nomanfaisal.eventhub.core.ui.theme.EventHubTheme

/** Looks like a text field but is a single tap target; typing happens on the Search tab. */
@Composable
fun SearchBarButton(
    placeholder: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, EventHubTheme.extras.outline),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Icon(AppIcon.Search, tint = EventHubTheme.extras.mutedText, size = 22.dp)
            Text(
                text = placeholder,
                style = MaterialTheme.typography.bodyLarge,
                color = EventHubTheme.extras.mutedText,
            )
        }
    }
}

@Composable
fun CategoryChipRow(
    labels: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        itemsIndexed(labels) { index, label ->
            val selected = index == selectedIndex
            Surface(
                onClick = { onSelect(index) },
                shape = RoundedCornerShape(percent = 50),
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                border = if (selected) null else BorderStroke(1.dp, EventHubTheme.extras.outline),
                modifier = Modifier.semantics { this.selected = selected; this.role = Role.Tab },
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
                )
            }
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: () -> Unit = {},
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = title, style = MaterialTheme.typography.headlineMedium)
        if (actionLabel != null) {
            Surface(onClick = onAction, color = androidx.compose.ui.graphics.Color.Transparent) {
                Text(
                    text = actionLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = EventHubTheme.extras.accent,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }
        }
    }
}

@Preview
@Composable
private fun SearchAndChipsPreview() {
    EventHubTheme {
        androidx.compose.foundation.layout.Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SearchBarButton(placeholder = "Search artists, venues, events", onClick = {})
            CategoryChipRow(listOf("All", "Music", "Sports", "Arts & Theatre", "Film"), selectedIndex = 0, onSelect = {})
            SectionHeader(title = "Trending near you", actionLabel = "See all", onAction = {})
        }
    }
}
