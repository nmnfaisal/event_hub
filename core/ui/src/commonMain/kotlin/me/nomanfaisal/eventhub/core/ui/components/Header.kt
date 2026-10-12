package me.nomanfaisal.eventhub.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.ui.tooling.preview.Preview
import me.nomanfaisal.eventhub.core.ui.theme.AppIcon
import me.nomanfaisal.eventhub.core.ui.theme.EventHubTheme

@Composable
fun AppHeader(
    title: String,
    locationText: String,
    onLocationClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = title, style = MaterialTheme.typography.titleLarge)
        LocationPill(text = locationText, onClick = onLocationClick)
    }
}

@Composable
fun LocationPill(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(percent = 50),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, EventHubTheme.extras.outline),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(AppIcon.Pin)
            Text(text = text, style = MaterialTheme.typography.labelLarge)
            Icon(AppIcon.ChevronDown)
        }
    }
}

@Composable
internal fun Icon(
    icon: AppIcon,
    modifier: Modifier = Modifier,
    tint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
    size: androidx.compose.ui.unit.Dp = 20.dp,
) {
    Image(
        imageVector = icon.vector,
        contentDescription = null,
        colorFilter = ColorFilter.tint(tint),
        modifier = modifier.size(size),
    )
}

@Preview
@Composable
private fun AppHeaderPreview() {
    EventHubTheme {
        AppHeader(title = "EventHub", locationText = "Berlin, DE", onLocationClick = {}, modifier = Modifier.padding(16.dp))
    }
}
