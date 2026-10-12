package me.nomanfaisal.eventhub.core.network

internal fun requireApiKey(): String {
    check(TICKETMASTER_API_KEY.isNotBlank()) {
        "TICKETMASTER_API_KEY is not set. Add TICKETMASTER_API_KEY=<your key> to local.properties."
    }
    return TICKETMASTER_API_KEY
}
