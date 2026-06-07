package com.egesa.clinic.shared.domain

import kotlinx.datetime.Clock

/**
 * Extension functions for Prescription repository operations.
 * These support marking prescriptions for external pharmacy purchase
 * and tracking their dispensing status.
 */

/**
 * Marks a prescription as being for external pharmacy purchase.
 * This means the patient will buy the medications outside the facility,
 * so no internal stock movement occurs.
 *
 * The prescription status changes to EXTERNAL_PURCHASE and the
 * externalPurchaseMarked flag is set to true.
 */
fun Prescription.markForExternalPurchase(): Prescription {
    return this.copy(
        status = "EXTERNAL_PURCHASE",
        externalPurchaseMarked = true,
        updatedAt = Clock.System.now().toString()
    )
}

/**
 * Marks a prescription as dispensed (patient received at facility pharmacy).
 * This occurs when the internal pharmacy stocks the medication.
 */
fun Prescription.markAsDispensed(): Prescription {
    return this.copy(
        status = "DISPENSED",
        externalPurchaseMarked = false,
        updatedAt = kotlinx.datetime.Clock.System.now().toString()
    )
}

/**
 * Checks if a prescription is ready to be printed and given to patient.
 * Can be printed regardless of dispensing status.
 */
fun Prescription.isPrintable(): Boolean {
    return status in listOf("ACTIVE", "DISPENSED", "EXTERNAL_PURCHASE")
}

/**
 * Checks if a prescription is still valid (not expired or cancelled).
 */
fun Prescription.isValid(): Boolean {
    return status in listOf("ACTIVE", "DISPENSED", "EXTERNAL_PURCHASE")
}

/**
 * Gets the patient-facing status description.
 */
fun Prescription.getPatientFacingStatus(): String {
    return when (status) {
        "ACTIVE" -> "Active Prescription"
        "DISPENSED" -> "Dispensed - Picked Up"
        "EXTERNAL_PURCHASE" -> "For External Pharmacy"
        "EXPIRED" -> "Expired - Contact Doctor"
        "CANCELLED" -> "Cancelled - Contact Doctor"
        else -> "Unknown Status"
    }
}

/**
 * Gets a user-friendly description of what the prescription status means.
 */
fun Prescription.getStatusExplanation(): String {
    return when (status) {
        "ACTIVE" -> "This prescription is active and can be dispensed at the facility pharmacy."
        "DISPENSED" -> "You have already picked up this prescription from the facility pharmacy."
        "EXTERNAL_PURCHASE" -> "This prescription is for you to take to an outside pharmacy of your choice."
        "EXPIRED" -> "This prescription expired. Please contact your doctor to issue a new one."
        "CANCELLED" -> "This prescription was cancelled. Please contact your doctor if you need medications."
        else -> "Status unknown. Please contact your healthcare provider."
    }
}

/**
 * Data class to track prescription printing and distribution history.
 * Useful for audit trails and compliance reporting.
 */
data class PrescriptionPrintAudit(
    val prescriptionId: String,
    val patientId: String,
    val encounterId: String,
    val printedAt: String,
    val printedBy: String,  // User ID/name who printed
    val format: String,      // "HTML", "TEXT", "MARKDOWN", "PDF"
    val method: String,      // "PRINT", "DOWNLOAD", "EMAIL", "SMS", "SHARE"
    val destination: String, // "INTERNAL_PHARMACY", "EXTERNAL_PHARMACY", "PATIENT_EMAIL", etc.
    val notes: String? = null
)

/**
 * Represents prescription print metrics for compliance and monitoring.
 */
data class PrescriptionPrintMetrics(
    val totalPrescriptionsCreated: Int,
    val prescriptionsPrinted: Int,
    val prescriptionsExternalPurchase: Int,
    val prescriptionsDispensedInternally: Int,
    val averagePrintTimeMinutes: Double,
    val mostCommonPrintFormat: String,
    val printSuccessRate: Double
)


