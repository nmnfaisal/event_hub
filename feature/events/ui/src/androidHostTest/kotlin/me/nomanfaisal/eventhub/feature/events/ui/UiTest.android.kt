package me.nomanfaisal.eventhub.feature.events.ui

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
actual abstract class UiTest actual constructor()

// Window size comes from robolectric.properties (w412dp-h2400dp-mdpi).
@OptIn(ExperimentalTestApi::class)
actual fun runTallUiTest(block: ComposeUiTest.() -> Unit) = runComposeUiTest { block() }
