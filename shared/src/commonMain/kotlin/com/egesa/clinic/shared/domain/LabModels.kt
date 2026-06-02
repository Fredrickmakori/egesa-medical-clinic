package com.egesa.clinic.shared.domain

import kotlinx.serialization.Serializable

@Serializable
enum class LabOrderStatus {
    ORDERED,
    SAMPLE_COLLECTED,
    IN_PROCESS,
    VERIFIED,
    REPORTED,
}

@Serializable
enum class LabPriority {
    ROUTINE,
    URGENT,
    STAT,
}

@Serializable
enum class LabResultFlag {
    LOW,
    HIGH,
    CRITICAL,
    ABNORMAL,
    NORMAL,
}

@Serializable
data class LabTest(
    val id: String,
    val code: String,
    val name: String,
    val category: String,
    val specimenType: String,
    val department: String,
    val loincCode: String? = null,
    val defaultUnit: String? = null,
    val defaultReferenceRange: String? = null,
    val billingCode: String,
    val price: Double,
    val active: Boolean = true,
    val createdAt: String,
    val updatedAt: String,
)

@Serializable
data class LabPanel(
    val id: String,
    val code: String,
    val name: String,
    val department: String,
    val testIds: List<String>,
    val billingCode: String,
    val price: Double,
    val active: Boolean = true,
    val createdAt: String,
    val updatedAt: String,
)

@Serializable
data class LabOrderItem(
    val id: String,
    val orderId: String,
    val testId: String,
    val testCode: String,
    val testName: String,
    val status: LabOrderStatus = LabOrderStatus.ORDERED,
    val priority: LabPriority = LabPriority.ROUTINE,
    val instructions: String? = null,
    val billingCode: String,
    val price: Double = 0.0,
    val orderedAt: String,
    val updatedAt: String,
)

@Serializable
data class LabOrder(
    val id: String,
    val localId: String = id,
    val serverId: String? = null,
    val patientId: String,
    val encounterId: String? = null,
    val orderedBy: String,
    val department: String,
    val status: LabOrderStatus = LabOrderStatus.ORDERED,
    val priority: LabPriority = LabPriority.ROUTINE,
    val diagnosisHint: String? = null,
    val clinicalNotes: String? = null,
    val items: List<LabOrderItem> = emptyList(),
    val sampleId: String? = null,
    val billableGroupId: String? = null,
    val createdAt: String,
    val updatedAt: String,
    val verifiedBy: String? = null,
    val verifiedAt: String? = null,
    val reportedBy: String? = null,
    val reportedAt: String? = null,
    val version: Int = 1,
    val deletedAt: String? = null,
) {
    val testName: String
        get() = items.firstOrNull()?.testName ?: id
}

@Serializable
data class LabSample(
    val id: String,
    val orderId: String,
    val patientId: String,
    val specimenType: String,
    val accessionNumber: String? = null,
    val collectedBy: String? = null,
    val collectedAt: String? = null,
    val receivedBy: String? = null,
    val receivedAt: String? = null,
    val rejectedReason: String? = null,
    val status: LabOrderStatus = LabOrderStatus.ORDERED,
    val createdAt: String,
    val updatedAt: String,
)

@Serializable
data class LabResult(
    val id: String,
    val orderId: String,
    val orderItemId: String,
    val patientId: String,
    val testId: String,
    val testCode: String,
    val testName: String,
    val value: String,
    val valueNumeric: Double? = null,
    val unit: String? = null,
    val referenceRange: String? = null,
    val flag: LabResultFlag? = null,
    val comment: String? = null,
    val enteredBy: String,
    val enteredAt: String,
    val verifiedBy: String? = null,
    val verifiedAt: String? = null,
    val reportedBy: String? = null,
    val reportedAt: String? = null,
    val createdAt: String,
    val updatedAt: String,
)

fun LabOrderStatus.canTransitionTo(next: LabOrderStatus): Boolean = when (this) {
    LabOrderStatus.ORDERED -> next == LabOrderStatus.SAMPLE_COLLECTED
    LabOrderStatus.SAMPLE_COLLECTED -> next == LabOrderStatus.IN_PROCESS
    LabOrderStatus.IN_PROCESS -> next == LabOrderStatus.VERIFIED
    LabOrderStatus.VERIFIED -> next == LabOrderStatus.REPORTED
    LabOrderStatus.REPORTED -> false
}
