package me.nomanfaisal.eventhub

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
