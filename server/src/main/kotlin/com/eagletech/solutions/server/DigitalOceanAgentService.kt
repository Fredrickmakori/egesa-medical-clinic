package com.eagletech.solutions.server

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.java.Java
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class AgentSupportRequest(
    val prompt: String
)

@Serializable
data class AgentSupportResponse(
    val answer: String,
    val source: String,
    val warning: String? = null
)

private val obviousPhiPatterns = listOf(
    Regex("""\b\d{3}[-.\s]?\d{3}[-.\s]?\d{4}\b"""),
    Regex("""\b[A-Z]{2,5}-\d{3,}\b"""),
    Regex("""\b[\w.+-]+@[\w.-]+\.[A-Za-z]{2,}\b"""),
    Regex("""\b(?:patient|name|phone|email|address|national id|passport)\s*[:=]""", RegexOption.IGNORE_CASE)
)

class DigitalOceanAgentService(
    private val endpointUrl: String = System.getenv("EGESA_DO_AGENT_URL")
        ?: "http://127.0.0.1:8080/run",
    private val bearerToken: String? = System.getenv("DIGITALOCEAN_API_TOKEN")?.takeIf { it.isNotBlank() },
    private val httpClient: HttpClient = HttpClient(Java) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }
) {
    suspend fun ask(prompt: String): AgentSupportResponse {
        val cleanPrompt = prompt.trim()
        if (cleanPrompt.isBlank()) {
            return AgentSupportResponse(
                answer = "Please provide a non-empty support question.",
                source = endpointUrl,
                warning = "empty_prompt"
            )
        }

        if (containsObviousPhi(cleanPrompt)) {
            return AgentSupportResponse(
                answer = "I cannot send this prompt to the support agent because it may contain PHI. Remove identifiers and retry with de-identified workflow context only.",
                source = endpointUrl,
                warning = "possible_phi_detected"
            )
        }

        return runCatching {
            val response = httpClient.post(endpointUrl) {
                contentType(ContentType.Application.Json)
                bearerToken?.let { header(HttpHeaders.Authorization, "Bearer $it") }
                setBody(mapOf("prompt" to cleanPrompt))
            }.body<String>()

            AgentSupportResponse(
                answer = response.trim('"'),
                source = endpointUrl
            )
        }.getOrElse { error ->
            AgentSupportResponse(
                answer = "DigitalOcean agent request failed: ${error::class.simpleName}: ${error.message}",
                source = endpointUrl,
                warning = "agent_unavailable"
            )
        }
    }

    private fun containsObviousPhi(prompt: String): Boolean =
        obviousPhiPatterns.any { it.containsMatchIn(prompt) }
}

