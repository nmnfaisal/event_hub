package me.nomanfaisal.eventhub.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** Colours that Material's scheme has no slot for. */
data class EventHubExtras(
    val accent: Color,
    val mutedText: Color,
    val outline: Color,
    val skeleton: Color,
    val imagePlaceholder: Color,
    val bannerBackground: Color,
    val onBanner: Color,
)

private val Ink = Color(0xFF17171B)
private val Paper = Color(0xFFF4F1EA)
private val Accent = Color(0xFFB83A1E)

private val LightExtras = EventHubExtras(
    accent = Accent,
    mutedText = Color(0xFF5E5B57),
    outline = Color(0xFFE3DFD6),
    skeleton = Color(0xFFE1DDD6),
    imagePlaceholder = Color(0xFF3B3049),
    bannerBackground = Ink,
    onBanner = Color.White,
)

private val LightColors = lightColorScheme(
    primary = Ink,
    onPrimary = Color.White,
    secondary = Accent,
    onSecondary = Color.White,
    background = Paper,
    onBackground = Ink,
    surface = Color.White,
    onSurface = Ink,
    onSurfaceVariant = Color(0xFF5E5B57),
    outline = Color(0xFFE3DFD6),
)

private val EventHubTypography = Typography(
    displayLarge = TextStyle(fontSize = 40.sp, lineHeight = 44.sp, fontWeight = FontWeight.ExtraBold),
    headlineMedium = TextStyle(fontSize = 24.sp, lineHeight = 30.sp, fontWeight = FontWeight.Bold),
    titleLarge = TextStyle(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold),
    titleMedium = TextStyle(fontSize = 17.sp, lineHeight = 22.sp, fontWeight = FontWeight.Bold),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold),
    labelMedium = TextStyle(fontSize = 13.sp, lineHeight = 16.sp, fontWeight = FontWeight.SemiBold),
)

private val LocalExtras = staticCompositionLocalOf { LightExtras }

object EventHubTheme {
    val extras: EventHubExtras
        @Composable get() = LocalExtras.current
}

/** Light theme only in v1. */
@Composable
fun EventHubTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalExtras provides LightExtras) {
        MaterialTheme(
            colorScheme = LightColors,
            typography = EventHubTypography,
            content = content,
        )
    }
}
