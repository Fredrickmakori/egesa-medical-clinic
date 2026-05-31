package com.egesa.clinic.server

import io.ktor.client.HttpClient
import io.ktor.client.engine.java.Java
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.header
import io.ktor.client.statement.HttpResponse
import io.ktor.http.parameters
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class NotificationService(
    private val username: String? = System.getenv("AT_USERNAME")?.takeIf { it.isNotBlank() },
    private val apiKey: String? = System.getenv("AT_API_KEY")?.takeIf { it.isNotBlank() },
    private val httpClient: HttpClient = HttpClient(Java) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }
) {
    suspend fun sendSms(to: String, message: String): Boolean = withContext(Dispatchers.IO) {
        if (username == null || apiKey == null) {
            // Mock Fallback mode
            println("[NotificationService MOCK SMS] To: $to | Msg: $message")
            return@withContext true
        }

        try {
            val response: HttpResponse = httpClient.submitForm(
                url = "https://api.africastalking.com/version1/messaging",
                formParameters = parameters {
                    append("username", username)
                    append("to", to)
                    append("message", message)
                }
            ) {
                header("apikey", apiKey)
                header("Accept", "application/json")
            }

            val status = response.status.value
            if (status in 200..299) {
                println("[NotificationService AT SMS Success] SMS sent to $to via Africa's Talking API.")
                true
            } else {
                println("[NotificationService AT SMS Error] Failed to send SMS. Status: $status")
                false
            }
        } catch (e: Exception) {
            println("[NotificationService AT SMS Exception] Error: ${e.message}")
            false
        }
    }
}
