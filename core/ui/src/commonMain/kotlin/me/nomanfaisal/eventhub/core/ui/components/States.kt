package me.nomanfaisal.eventhub.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import org.jetbrains.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import me.nomanfaisal.eventhub.core.ui.theme.AppIcon
import me.nomanfaisal.eventhub.core.ui.theme.EventHubTheme

@Composable
fun SkeletonBox(modifier: Modifier = Modifier, cornerRadius: androidx.compose.ui.unit.Dp = 12.dp) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(EventHubTheme.extras.skeleton),
    )
}

/** Empty result. [actionLabel] and [onAction] are optional; no button without a label. */
@Composable
fun EmptyView(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    icon: AppIcon = AppIcon.Calendar,
    actionLabel: String? = null,
    onAction: () -> Unit = {},
) {
    StateView(title, message, icon, actionLabel, onAction, filledAction = false, modifier = modifier)
}

@Composable
fun ErrorView(
    title: String,
    message: String,
    actionLabel: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    icon: AppIcon = AppIcon.WifiOff,
) {
    StateView(title, message, icon, actionLabel, onRetry, filledAction = true, modifier = modifier)
}

@Composable
private fun StateView(
    title: String,
    message: String,
    icon: AppIcon,
    actionLabel: String?,
    onAction: () -> Unit,
    filledAction: Boolean,
    modifier: Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically),
    ) {
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surface, modifier = Modifier.size(64.dp)) {
            Box(contentAlignment = Alignment.Center) { Icon(icon, size = 26.dp) }
        }
        Text(text = title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = EventHubTheme.extras.mutedText,
            textAlign = TextAlign.Center,
        )
        if (actionLabel != null) {
            Surface(
                onClick = onAction,
                shape = RoundedCornerShape(14.dp),
                color = if (filledAction) EventHubTheme.extras.accent else Color.Transparent,
                contentColor = if (filledAction) Color.White else MaterialTheme.colorScheme.onSurface,
                border = if (filledAction) null else BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface),
                modifier = Modifier.padding(top = 8.dp),
            ) {
                Text(
                    text = actionLabel,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 14.dp),
                )
            }
        }
    }
}

@Composable
fun OfflineBanner(
    text: String,
    actionLabel: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(EventHubTheme.extras.bannerBackground)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = EventHubTheme.extras.onBanner,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = actionLabel,
            style = MaterialTheme.typography.labelLarge,
            color = EventHubTheme.extras.onBanner,
            textDecoration = TextDecoration.Underline,
            modifier = Modifier.clickable(onClick = onAction),
        )
    }
}

@Composable
fun BottomNavBar(
    labels: List<String>,
    icons: List<AppIcon>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(color = MaterialTheme.colorScheme.background, modifier = modifier) {
        Column {
            Box(Modifier.fillMaxWidth().size(width = 0.dp, height = 1.dp).background(EventHubTheme.extras.outline))
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
                labels.forEachIndexed { index, label ->
                    val selected = index == selectedIndex
                    val tint = if (selected) EventHubTheme.extras.accent else EventHubTheme.extras.mutedText
                    Column(
                        modifier = Modifier.weight(1f).clickable { onSelect(index) }.padding(vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(icons[index], tint = tint, size = 26.dp)
                        Text(text = label, style = MaterialTheme.typography.labelMedium, color = tint)
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun StatesPreview() {
    EventHubTheme {
        Column(
            modifier = Modifier.background(MaterialTheme.colorScheme.background).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OfflineBanner("Offline. Showing events from 14:32.", "Retry", {})
            SkeletonBox(Modifier.fillMaxWidth().size(width = 0.dp, height = 60.dp))
            ErrorView("Couldn't load events", "Check your connection and try again.", "Retry", {}, Modifier.size(width = 300.dp, height = 280.dp))
            EmptyView("Nothing on for these filters", "Try another category.", Modifier.size(width = 300.dp, height = 280.dp), actionLabel = "Clear filters")
            BottomNavBar(listOf("Home", "Search", "Saved", "Tickets"), listOf(AppIcon.Home, AppIcon.Search, AppIcon.Saved, AppIcon.Tickets), 0, {})
        }
    }
}
