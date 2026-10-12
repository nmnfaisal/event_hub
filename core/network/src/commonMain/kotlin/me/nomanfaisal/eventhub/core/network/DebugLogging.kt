package me.nomanfaisal.eventhub.core.network

import io.ktor.client.HttpClientConfig
import io.ktor.client.call.save
import io.ktor.client.plugins.api.Send
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.statement.bodyAsText
import io.ktor.http.content.TextContent
import kotlinx.serialization.json.Json

private const val REDACTED = "[REDACTED]"
private const val MAX_BODY_CHARS = 4_000
private val apiKeyInUrl = Regex("""([?&]apikey=)[^&\s']*""", RegexOption.IGNORE_CASE)
private val prettyJson = Json { prettyPrint = true }
private val secretHeaders = setOf("authorization", "cookie", "set-cookie")

private class LogConfig {
    var write: (String) -> Unit = {}
}

/**
 * Logs each request as a pasteable curl command and each response as status plus a
 * pretty-printed body. The `apikey` query value is replaced with `[REDACTED]` everywhere.
 *
 * Ktor's own `Logging` plugin only hands its logger pre-formatted text, so it cannot emit
 * curl; this plugin hooks `Send`, after `defaultRequest`, so the logged URL is the final one.
 */
private val DebugLogging = createClientPlugin("DebugLogging", ::LogConfig) {
    val sink = pluginConfig
    on(Send) { request ->
        sink.write(request.toCurl().redacted())
        val call = proceed(request).save()
        val body = call.response.bodyAsText()
        sink.write("<-- ${call.response.status.value} ${request.url.buildString().redacted()}\n${body.pretty()}")
        call
    }
}

internal fun HttpClientConfig<*>.installDebugLogging(write: (String) -> Unit) {
    install(DebugLogging) { this.write = write }
}

private fun HttpRequestBuilder.toCurl(): String = buildString {
    append("curl -X ").append(method.value).append(" '").append(url.buildString()).append("'")
    headers.build().forEach { name, values ->
        val shown = if (name.lowercase() in secretHeaders) REDACTED else values.joinToString(", ")
        append(" \\\n  -H '").append(name).append(": ").append(shown).append("'")
    }
    (body as? TextContent)?.let { append(" \\\n  -d '").append(it.text).append("'") }
}

private fun String.redacted() = replace(apiKeyInUrl) { it.groupValues[1] + REDACTED }

private fun String.pretty(): String {
    val formatted = runCatching { prettyJson.encodeToString(prettyJson.parseToJsonElement(this)) }.getOrDefault(this)
    return if (formatted.length <= MAX_BODY_CHARS) formatted
    else formatted.take(MAX_BODY_CHARS) + "\n… (${formatted.length - MAX_BODY_CHARS} more characters)"
}
