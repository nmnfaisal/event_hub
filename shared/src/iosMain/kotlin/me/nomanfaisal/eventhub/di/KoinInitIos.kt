package me.nomanfaisal.eventhub.di

import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.Platform

/**
 * iOS entry point for Koin. Named distinctly (not an `initKoin` overload) so the
 * Objective-C/Swift export is an unambiguous `doInitKoinIos()` with no arguments —
 * Swift cannot pass the `KoinApplication.() -> Unit` receiver the common `initKoin` takes.
 */
@OptIn(ExperimentalNativeApi::class)
fun initKoinIos() = initKoin(isDebug = Platform.isDebugBinary) {}
