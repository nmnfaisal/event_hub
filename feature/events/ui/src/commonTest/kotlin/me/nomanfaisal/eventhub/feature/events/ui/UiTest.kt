package me.nomanfaisal.eventhub.feature.events.ui

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi

/** Base class that lets the Android host target run Compose tests under Robolectric. */
expect abstract class UiTest()

/** Runs a Compose test in a 412x2400dp window so lazy content below the fold is composed and displayed. */
@OptIn(ExperimentalTestApi::class)
expect fun runTallUiTest(block: ComposeUiTest.() -> Unit)
