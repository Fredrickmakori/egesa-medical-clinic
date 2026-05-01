package com.egesa.clinic.shared

/**
 * Platform-agnostic gateway for M-Pesa STK payment operations.
 * Implementations provide platform/network specific behavior.
 */
interface MpesaGateway {
    suspend fun initiateStkPush(request: MpesaPaymentRequest): MpesaPaymentResult
    suspend fun queryStkStatus(checkoutRequestId: String): MpesaPaymentResult
}

/**
 * Contract for persisting and retrieving payment records independently of storage backend.
 */
interface PaymentRepository {
    suspend fun savePayment(record: PaymentRecord)
    suspend fun updatePaymentStatus(
        checkoutRequestId: String,
        status: PaymentStatus,
        receiptNumber: String? = null,
        resultCode: String? = null,
        resultDescription: String? = null
    )

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
