package me.nomanfaisal.eventhub.feature.events.ui

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runSkikoComposeUiTest
import androidx.compose.ui.unit.Density

actual abstract class UiTest actual constructor()

@OptIn(ExperimentalTestApi::class)
actual fun runTallUiTest(block: ComposeUiTest.() -> Unit) =
    runSkikoComposeUiTest(size = Size(412f, 2400f), density = Density(1f)) { block() }
