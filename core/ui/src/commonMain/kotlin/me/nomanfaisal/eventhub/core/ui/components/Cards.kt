package me.nomanfaisal.eventhub.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import org.jetbrains.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import me.nomanfaisal.eventhub.core.ui.theme.EventHubTheme

/** Large card for the Trending carousel. The caller sets the width. */
@Composable
fun FeaturedCard(
    title: String,
    dateChip: String,
    subtitle: String?,
    imageUrl: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.clickable(onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(20.dp))) {
            RemoteImage(url = imageUrl, modifier = Modifier.matchParentSize())
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.align(Alignment.TopStart).padding(12.dp),
            ) {
                Text(
                    text = dateChip,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                )
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = EventHubTheme.extras.mutedText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
fun EventRow(
    title: String,
    dateText: String,
    venueText: String?,
    imageUrl: String?,
    badge: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            RemoteImage(
                url = imageUrl,
                modifier = Modifier.size(64.dp).clip(RoundedCornerShape(14.dp)),
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(text = dateText, style = MaterialTheme.typography.bodyMedium, color = EventHubTheme.extras.mutedText)
                if (venueText != null) {
                    Text(
                        text = venueText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = EventHubTheme.extras.mutedText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (badge != null) {
                Text(
                    text = badge,
                    style = MaterialTheme.typography.labelMedium,
                    color = EventHubTheme.extras.accent,
                    modifier = Modifier.padding(end = 6.dp),
                )
            }
        }
    }
}

@Preview
@Composable
private fun CardsPreview() {
    EventHubTheme {
        Column(
            modifier = Modifier.background(MaterialTheme.colorScheme.background).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            FeaturedCard(
                title = "Velvet Static: Autumn Tour",
                dateChip = "Fri 16 Oct",
                subtitle = "Halle Ost",
                imageUrl = null,
                onClick = {},
                modifier = Modifier.size(width = 250.dp, height = 210.dp),
            )
            EventRow("Film Scores Live", "Tue 13 Oct · 19:30", "Konzertsaal Mitte", null, null, {})
            EventRow("Eli – STAGE GIRL", "Wed 14 Oct · 20:00", null, null, "Postponed", {})
        }
    }
}
