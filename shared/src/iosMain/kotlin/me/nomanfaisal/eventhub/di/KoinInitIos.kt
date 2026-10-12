package me.nomanfaisal.eventhub.di

import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.Platform

/** For Swift, which cannot pass a Kotlin receiver lambda. */
@OptIn(ExperimentalNativeApi::class)
fun initKoin() = initKoin(isDebug = Platform.isDebugBinary) {}
