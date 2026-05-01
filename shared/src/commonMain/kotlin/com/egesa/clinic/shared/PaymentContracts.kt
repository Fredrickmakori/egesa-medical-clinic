package com.egesa.clinic.shared

/**
 * Platform-agnostic gateway for M-Pesa STK payment operations.
 *
 * Shared code defines only the contract; platform/server modules provide concrete implementations.
 */
interface MpesaGateway {
    suspend fun initiateStkPush(request: MpesaPaymentRequest): MpesaPaymentResult
    suspend fun queryStkStatus(checkoutRequestId: String): MpesaPaymentResult
}

/**
 * Platform-agnostic persistence contract for payment records.
 *
 * This contract intentionally avoids coupling to any specific database, ORM, or transport.
 */
interface PaymentRepository {
    suspend fun savePayment(record: PaymentRecord)
    suspend fun updatePayment(record: PaymentRecord)

    suspend fun getPaymentById(id: String): PaymentRecord?
    suspend fun getPaymentByCheckoutRequestId(checkoutRequestId: String): PaymentRecord?
    suspend fun getPaymentByMerchantRequestId(merchantRequestId: String): PaymentRecord?
    suspend fun listPaymentsByPatientId(patientId: String): List<PaymentRecord>
}

data class MpesaPaymentRequest(
    val amount: Long,
    val phoneNumber: String,
    val accountReference: String,
    val transactionDescription: String,
    val patientId: String? = null,
    val visitId: String? = null
)

data class MpesaPaymentResult(
    val checkoutRequestId: String? = null,
    val merchantRequestId: String? = null,
    val status: PaymentStatus,
    val resultCode: String? = null,
    val resultDescription: String? = null,
    val receiptNumber: String? = null,
    val transactionDate: String? = null,
    val phoneNumber: String? = null
)

data class PaymentRecord(
    val id: String,
    val patientId: String,
    val amount: Long,
    val phoneNumber: String,
    val accountReference: String,
    val transactionDescription: String,
    val checkoutRequestId: String,
    val merchantRequestId: String? = null,
    val status: PaymentStatus,
    val receiptNumber: String? = null,
    val resultCode: String? = null,
    val resultDescription: String? = null,
    val createdAtIso: String,
    val updatedAtIso: String
)

enum class PaymentStatus {
    PENDING,
    SUCCESS,
    FAILED,
    CANCELLED,
    TIMEOUT
}
