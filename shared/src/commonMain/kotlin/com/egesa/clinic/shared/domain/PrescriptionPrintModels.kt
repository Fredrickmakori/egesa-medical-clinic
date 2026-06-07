package com.egesa.clinic.shared.domain

import kotlinx.serialization.Serializable

/**
 * Represents a single medication item in a prescription, optimized for printing/export.
 * Aggregates all necessary information for patient-facing documents.
 */
@Serializable
data class MedicationPrintItem(
    val medicationName: String,           // Brand name
    val genericName: String? = null,       // Generic/chemical name
    val strength: String? = null,          // e.g., "500mg", "10%"
    val form: String? = null,              // e.g., "tablet", "capsule", "injection"
    val dose: String,                      // e.g., "1 tablet"
    val route: String,                     // e.g., "oral", "IM", "IV"
    val frequency: String,                 // e.g., "twice daily", "every 8 hours"
    val duration: String,                  // e.g., "7 days", "2 weeks"
    val quantity: Int? = null,             // Total number of units to dispense
    val patientInstructions: String? = null, // Plain language instructions for patient
    val refills: Int = 0,                  // Number of times prescription can be refilled
    val notes: String? = null              // Additional clinical notes (not shown to patient)
)

/**
 * Facility/Provider information needed on the prescription document.
 */
@Serializable
data class FacilityPrintInfo(
    val facilityName: String,
    val address: String? = null,
    val phoneNumber: String? = null,
    val licenseNumber: String? = null,
    val logoUrl: String? = null            // Placeholder for facility logo
)

/**
 * Provider/Doctor information needed on the prescription document.
 */
@Serializable
data class ProviderPrintInfo(
    val providerName: String,
    val specialty: String? = null,
    val registrationNumber: String? = null,
    val licenseNumber: String? = null,
    val signatureBlockPlaceholder: String = "[Doctor Signature]"
)

/**
 * Patient demographic information for the prescription.
 */
@Serializable
data class PatientPrintInfo(
    val patientName: String,
    val age: Int? = null,
    val sex: String? = null,               // "M", "F", "Other"
    val patientId: String? = null,
    val dateOfBirth: String? = null
)

/**
 * Represents the complete prescription document model.
 * This is independent of UI toolkits and can be converted to HTML, PDF, or other formats.
 */
@Serializable
data class PrescriptionPrintModel(
    val prescriptionId: String,
    val encounterId: String,
    val dateIssued: String,                // ISO 8601 date
    val facility: FacilityPrintInfo,
    val provider: ProviderPrintInfo,
    val patient: PatientPrintInfo,
    val medications: List<MedicationPrintItem>,
    val diagnosis: String? = null,         // Primary diagnosis (if allowed by policy)
    val indication: String? = null,        // Clinical indication
    val status: PrescriptionPrintStatus = PrescriptionPrintStatus.ACTIVE,
    val externalPurchaseMarked: Boolean = false, // True if patient buying outside facility
    val notes: String? = null,             // General prescription notes
    val disclaimers: String? = null        // Legal/clinical disclaimers
)

@Serializable
enum class PrescriptionPrintStatus {
    ACTIVE,
    DISPENSED,
    EXPIRED,
    CANCELLED,
    EXTERNAL_PURCHASE    // Patient chose to buy outside
}

/**
 * Extension function to convert a regular Prescription to PrescriptionPrintModel.
 * This aggregates prescription data with encounter and facility context.
 */
fun Prescription.toPrintModel(
    encounterId: String,
    facilityInfo: FacilityPrintInfo,
    providerInfo: ProviderPrintInfo,
    patientInfo: PatientPrintInfo,
    diagnosis: String? = null,
    externalPurchase: Boolean = false
): PrescriptionPrintModel {
    return PrescriptionPrintModel(
        prescriptionId = this.prescriptionId,
        encounterId = encounterId,
        dateIssued = this.createdAt,
        facility = facilityInfo,
        provider = providerInfo,
        patient = patientInfo,
        medications = listOf(
            MedicationPrintItem(
                medicationName = this.medicationName,
                strength = null,
                form = null,
                dose = this.dose ?: "As directed",
                route = this.route ?: "Oral",
                frequency = this.frequency ?: "As directed",
                duration = this.duration ?: "As advised",
                patientInstructions = this.instructions
            )
        ),
        diagnosis = diagnosis,
        status = if (externalPurchase) PrescriptionPrintStatus.EXTERNAL_PURCHASE else PrescriptionPrintStatus.ACTIVE,
        externalPurchaseMarked = externalPurchase
    )
}

