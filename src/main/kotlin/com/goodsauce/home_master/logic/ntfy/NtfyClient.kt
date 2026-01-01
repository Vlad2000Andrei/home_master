package com.goodsauce.home_master.logic.ntfy

import com.goodsauce.home_master.support.LOG
import io.ktor.http.*
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

@Component
class NtfyClient(
    @Value("\${ntfy.url}") private val ntfyUrl: String, @Value("\${ntfy.topic}") private val ntfyTopic: String
) {
    private val client = HttpClient.newHttpClient()

    fun notify(message: String, subtopic: String = "") {
        val fullUrl = buildUrl(subtopic)
        notifyAsync(message, fullUrl)
    }

    private fun buildUrl(subtopic: String = ""): String {
        val urlBuilder = URLBuilder()

        urlBuilder.set {
            host = ntfyUrl
            pathSegments = if (subtopic.isNotBlank()) listOf("${ntfyTopic}-${subtopic}") else listOf(ntfyTopic)
        }
        return urlBuilder.build().toString()
    }

    private fun notifyAsync(message: String, url: String) {
        val requestBuilder =
            HttpRequest.newBuilder().uri(URI.create(url)).POST(HttpRequest.BodyPublishers.ofString(message))
        client.sendAsync(requestBuilder.build(), HttpResponse.BodyHandlers.ofString()).thenAccept { response ->
                LOG.info { "Notified $message on channel $url: $response" }
            }
    }

}