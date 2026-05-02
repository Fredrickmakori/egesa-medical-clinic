package com.egesa.clinic.server

import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Base64
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class MpesaService(
    private val consumerKey: String = requiredEnv("MPESA_CONSUMER_KEY"),
    private val consumerSecret: String = requiredEnv("MPESA_CONSUMER_SECRET"),
    private val passkey: String = requiredEnv("MPESA_PASSKEY"),
    private val shortCode: String = requiredEnv("MPESA_SHORTCODE"),
    private val callbackUrl: String = requiredEnv("MPESA_CALLBACK_URL"),
    private val json: Json = Json { ignoreUnknownKeys = true }
) {
    private val oauthUrl = "https://sandbox.safaricom.co.ke/oauth/v1/generate?grant_type=client_credentials"
    private val stkPushUrl = "https://sandbox.safaricom.co.ke/mpesa/stkpush/v1/processrequest"

    fun initiateStkPush(request: StkPushRequest): ApiResponse {
        return runCatching {
            val timestamp = timestamp()
            val password = createPassword(timestamp)
            val token = generateOAuthToken()
            val darajaRequest = DarajaStkPushRequest(
                businessShortCode = shortCode,
                password = password,
                timestamp = timestamp,
                transactionType = "CustomerPayBillOnline",
                amount = request.amount,
                partyA = request.phoneNumber,
                partyB = shortCode,
                phoneNumber = request.phoneNumber,
                callBackURL = callbackUrl,
                accountReference = request.accountReference,
                transactionDesc = request.description
            )

            val darajaResponse = postJson(stkPushUrl, token, json.encodeToString(DarajaStkPushRequest.serializer(), darajaRequest))
            val parsed = json.decodeFromString(DarajaStkPushResponse.serializer(), darajaResponse)
            ApiResponse(
                success = parsed.responseCode == "0",
                message = parsed.customerMessage ?: parsed.responseDescription ?: "STK push request processed",
                data = buildJsonObject {
                    put("checkoutRequestId", parsed.checkoutRequestId ?: "")
                    put("merchantRequestId", parsed.merchantRequestId ?: "")
                    put("responseCode", parsed.responseCode ?: "")
                    put("responseDescription", parsed.responseDescription ?: "")
                    put("customerMessage", parsed.customerMessage ?: "")
                    put("status", if (parsed.responseCode == "0") "PENDING" else "FAILED")
                }
            )
        }.getOrElse {
            ApiResponse(false, it.message ?: "Failed to initiate STK push")
        }
    }

    fun status(checkoutRequestId: String): ApiResponse = ApiResponse(
        success = true,
        message = "Pending callback confirmation",
        data = buildJsonObject {
            put("checkoutRequestId", checkoutRequestId)
            put("status", "PENDING")
            put("source", "daraja-callback")
        }
    )

    fun parseCallback(payload: JsonElement): ApiResponse {
        val callback = json.decodeFromJsonElement(DarajaCallbackEnvelope.serializer(), payload)
        val stkCallback = callback.body.stkCallback
        val metadata = stkCallback.callbackMetadata?.item
            ?.associate { it.name to (it.value?.toString() ?: "") }
            ?: emptyMap()

        val status = if (stkCallback.resultCode == 0) "SUCCESS" else "FAILED"

        return ApiResponse(
            success = true,
            message = stkCallback.resultDesc,
            data = buildJsonObject {
                put("merchantRequestId", stkCallback.merchantRequestID)
                put("checkoutRequestId", stkCallback.checkoutRequestID)
                put("resultCode", stkCallback.resultCode)
                put("resultDesc", stkCallback.resultDesc)
                put("status", status)
                put("amount", metadata["Amount"] ?: "")
                put("mpesaReceiptNumber", metadata["MpesaReceiptNumber"] ?: "")
                put("phoneNumber", metadata["PhoneNumber"] ?: "")
                put("transactionDate", metadata["TransactionDate"] ?: "")
            }
        )
    }

    private fun generateOAuthToken(): String {
        val credentials = Base64.getEncoder().encodeToString("$consumerKey:$consumerSecret".toByteArray(StandardCharsets.UTF_8))
        val connection = URL(oauthUrl).openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.setRequestProperty("Authorization", "Basic $credentials")

        val body = connection.inputStream.bufferedReader().use { it.readText() }
        val tokenResponse = json.decodeFromString(MpesaOauthTokenResponse.serializer(), body)
        return tokenResponse.accessToken
    }

    private fun createPassword(timestamp: String): String {
        val raw = "$shortCode$passkey$timestamp"
        return Base64.getEncoder().encodeToString(raw.toByteArray(StandardCharsets.UTF_8))
    }

    private fun postJson(url: String, bearerToken: String, body: String): String {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.doOutput = true
        connection.setRequestProperty("Authorization", "Bearer $bearerToken")
        connection.setRequestProperty("Content-Type", "application/json")

        connection.outputStream.use { it.write(body.toByteArray(StandardCharsets.UTF_8)) }

        val stream = if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream
        return stream.bufferedReader().use { it.readText() }
    }

    private fun timestamp(): String = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))

    companion object {
        private fun requiredEnv(name: String): String =
            System.getenv(name)?.takeIf { it.isNotBlank() }
                ?: throw IllegalStateException("Missing required environment variable: $name")
    }
}

@Serializable
data class StkPushRequest(
    val phoneNumber: String,
    val amount: Int,
    val accountReference: String,
    val description: String
)

@Serializable
data class ApiResponse(
    val success: Boolean,
    val message: String,
    val data: JsonObject? = null
)

@Serializable
data class MpesaOauthTokenResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("expires_in") val expiresIn: String
)

@Serializable
data class DarajaStkPushRequest(
    @SerialName("BusinessShortCode") val businessShortCode: String,
    @SerialName("Password") val password: String,
    @SerialName("Timestamp") val timestamp: String,
    @SerialName("TransactionType") val transactionType: String,
    @SerialName("Amount") val amount: Int,
    @SerialName("PartyA") val partyA: String,
    @SerialName("PartyB") val partyB: String,
    @SerialName("PhoneNumber") val phoneNumber: String,
    @SerialName("CallBackURL") val callBackURL: String,
    @SerialName("AccountReference") val accountReference: String,
    @SerialName("TransactionDesc") val transactionDesc: String
)

@Serializable
data class DarajaStkPushResponse(
    @SerialName("MerchantRequestID") val merchantRequestId: String? = null,
    @SerialName("CheckoutRequestID") val checkoutRequestId: String? = null,
    @SerialName("ResponseCode") val responseCode: String? = null,
    @SerialName("ResponseDescription") val responseDescription: String? = null,
    @SerialName("CustomerMessage") val customerMessage: String? = null
)

@Serializable
data class DarajaCallbackEnvelope(
    @SerialName("Body") val body: DarajaBody
)

@Serializable
data class DarajaBody(
    @SerialName("stkCallback") val stkCallback: DarajaStkCallback
)

@Serializable
data class DarajaStkCallback(
    @SerialName("MerchantRequestID") val merchantRequestID: String,
    @SerialName("CheckoutRequestID") val checkoutRequestID: String,
    @SerialName("ResultCode") val resultCode: Int,
    @SerialName("ResultDesc") val resultDesc: String,
    @SerialName("CallbackMetadata") val callbackMetadata: DarajaCallbackMetadata? = null
)

@Serializable
data class DarajaCallbackMetadata(
    @SerialName("Item") val item: List<DarajaMetadataItem>
)

@Serializable
data class DarajaMetadataItem(
    @SerialName("Name") val name: String,
    @SerialName("Value") val value: JsonElement? = null
)
