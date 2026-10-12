package me.nomanfaisal.eventhub.core.model

/** An amount in minor units (cents) so no floating point is involved. */
data class Money(val amountMinor: Long, val currencyCode: String)
