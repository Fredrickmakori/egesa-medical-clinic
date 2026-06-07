package com.egesa.clinic.shared.domain

import kotlinx.serialization.Serializable
import kotlinx.datetime.Clock

@Serializable
enum class EncounterStatus { DRAFT, FINAL }

@Serializable
enum class EncounterSourceType { WALK_IN, APPOINTMENT }

@Serializable
enum class ClinicalOrderType { LAB, IMAGING, PROCEDURE }

@Serializable
data class Encounter(
    val encounterId: String,
    val localId: String = encounterId,
    val serverId: String? = null,
    val patientId: String,
    val providerId: String?,
    val facilityId: String,
    val locationId: String? = null,
    val encounterDatetime: String,
    val department: String = "OPD",
    val visitType: String = "outpatient",
    val sourceType: EncounterSourceType? = null,
    val sourceId: String? = null,
    val status: EncounterStatus = EncounterStatus.DRAFT,
    val version: Int = 1,
    val createdAt: String = Clock.System.now().toString(),
    val updatedAt: String = Clock.System.now().toString(),
    val deletedAt: String? = null,
    val nursingNotes: String? = null,
    val syncState: String = "LOCAL_ONLY",
)

@Serializable
data class EncounterHistory(
    val encounterId: String,
    val localId: String = "HIST-$encounterId",
    val serverId: String? = null,
    val chiefComplaint: String = "",
    val hpi: String = "",
    val pmh: String = "",
    val medicationHistory: String = "",
    val allergies: String = "",
    val familyHistory: String = "",
    val socialHistory: String = "",
    val version: Int = 1,
    val createdAt: String = Clock.System.now().toString(),
    val updatedAt: String = Clock.System.now().toString(),
    val deletedAt: String? = null,
)

@Serializable
data class EncounterExam(
    val encounterId: String,
    val localId: String = "EXAM-$encounterId",
    val serverId: String? = null,
    val systemExamNotes: String = "",
    val vitalSignsId: String? = null,
    val weightKg: Double? = null,
    val heightCm: Double? = null,
    val bmi: Double? = null,
    val temperatureC: Double? = null,
    val systolicBp: Int? = null,
    val diastolicBp: Int? = null,
    val pulseBpm: Int? = null,
    val respiratoryRate: Int? = null,
    val spo2Percent: Double? = null,
    val recordedAt: String? = null,
    val version: Int = 1,
    val createdAt: String = Clock.System.now().toString(),
    val updatedAt: String = Clock.System.now().toString(),
    val deletedAt: String? = null,
)

@Serializable
data class EncounterDiagnosis(
    val diagnosisId: String,
    val localId: String = diagnosisId,
    val serverId: String? = null,
    val encounterId: String,
    val diagnosisText: String,
    val isPrimary: Boolean = false,
    val codeSystem: String? = null,
    val diagnosisCode: String? = null,
    val version: Int = 1,
    val createdAt: String = Clock.System.now().toString(),
    val updatedAt: String = Clock.System.now().toString(),
    val deletedAt: String? = null,
)

@Serializable
data class EncounterPlan(
    val encounterId: String,
    val localId: String = "PLAN-$encounterId",
    val serverId: String? = null,
    val clinicalAdvice: String = "",
    val followUpDate: String? = null,
    val version: Int = 1,
    val createdAt: String = Clock.System.now().toString(),
    val updatedAt: String = Clock.System.now().toString(),
    val deletedAt: String? = null,
)

@Serializable
data class ImagingOrder(
    val orderId: String,
    val localId: String = orderId,
    val serverId: String? = null,
    val encounterId: String,
    val studyName: String,
    val instructions: String? = null,
    val status: String = "ORDERED",
    val version: Int = 1,
    val createdAt: String = Clock.System.now().toString(),
    val updatedAt: String = Clock.System.now().toString(),
    val deletedAt: String? = null,
)

@Serializable
data class Prescription(
    val prescriptionId: String,
    val localId: String = prescriptionId,
    val serverId: String? = null,
    val encounterId: String,
    val medicationName: String,
    val dose: String? = null,
    val frequency: String? = null,
    val duration: String? = null,
    val route: String? = null,
    val instructions: String? = null,
    val status: String = "ACTIVE",
    val externalPurchaseMarked: Boolean = false,
    val version: Int = 1,
    val createdAt: String = Clock.System.now().toString(),
    val updatedAt: String = Clock.System.now().toString(),
    val deletedAt: String? = null,
)

/** Full OPD consultation payload for create/update and sync. */
@Serializable
data class OpdEncounterBundle(
    val encounter: Encounter,
    val history: EncounterHistory? = null,
    val exam: EncounterExam? = null,
    val diagnoses: List<EncounterDiagnosis> = emptyList(),
    val plan: EncounterPlan? = null,
    val labOrders: List<LabOrder> = emptyList(),
    val imagingOrders: List<ImagingOrder> = emptyList(),
    val prescriptions: List<Prescription> = emptyList(),
    val disposition: String? = null,
    val referralTo: String? = null,
    val dischargeNotes: String? = null,
)
