package com.egesa.clinic.shared.ui.screens.registration

import com.egesa.clinic.shared.Sex
import com.egesa.clinic.shared.data.PatientDocumentInput
import com.egesa.clinic.shared.data.PatientRegistrationInput
import com.egesa.clinic.shared.data.RegistrationClinicalInput
import kotlinx.datetime.Clock

enum class RegistrationStep(val title: String, val subtitle: String) {
    IDENTITY("Identity", "Demographics and triage level"),
    VITALS("Vitals", "Optional measurements at registration"),
    QUEUE("Queue", "Routing and priority"),
    DOCUMENTS("Documents", "Capture and verify ID"),
    REVIEW("Review", "Confirm before registering"),
}

fun String.toReceptionSex(): Sex = when (trim().lowercase()) {
    "m", "male" -> Sex.MALE
    "f", "female" -> Sex.FEMALE
    "intersex" -> Sex.INTERSEX
    else -> Sex.UNKNOWN
}

fun String.digitsOnly(): String = filter { it.isDigit() }

fun String.decimalInput(): String = filterIndexed { index, ch ->
    ch.isDigit() || (ch == '.' && indexOf('.') == index)
}

fun RegistrationClinicalInput.hasVitals(): Boolean =
    listOf(weightKg, heightCm, temperatureC, systolicBp, diastolicBp, pulseBpm, respiratoryRate, spo2Percent, muacCm)
        .any { it != null }

fun calculateBmi(weightKg: Double?, heightCm: Double?): Double? {
    if (weightKg == null || heightCm == null || heightCm <= 0.0) return null
    val meters = heightCm / 100.0
    return kotlin.math.round((weightKg / (meters * meters)) * 10.0) / 10.0
}

data class RegistrationSubmitPayload(
    val patient: PatientRegistrationInput,
    val triageLevel: Int,
    val clinical: RegistrationClinicalInput,
    val document: PatientDocumentInput?,
)

fun buildRegistrationPayload(state: RegistrationFormState): RegistrationSubmitPayload? {
    val parsedAge = state.age.toIntOrNull() ?: return null
    val parsedTriage = state.triageLevel.toIntOrNull()?.coerceIn(1, 5) ?: 3
    val cleanId = state.id.trim()
    val clinical = RegistrationClinicalInput(
        weightKg = state.weightKg.toDoubleOrNull(),
        heightCm = state.heightCm.toDoubleOrNull(),
        temperatureC = state.temperatureC.toDoubleOrNull(),
        systolicBp = state.systolicBp.toLongOrNull(),
        diastolicBp = state.diastolicBp.toLongOrNull(),
        pulseBpm = state.pulseBpm.toLongOrNull(),
        respiratoryRate = state.respiratoryRate.toLongOrNull(),
        spo2Percent = state.spo2Percent.toDoubleOrNull(),
        muacCm = state.muacCm.toDoubleOrNull(),
        queueDestination = state.queueDestination.ifBlank { "Triage" },
        queuePriority = state.queuePriority.ifBlank { "Routine" },
        queueNote = state.queueNote.ifBlank { null },
    )
    val document = if (state.documentPhotoUri.isBlank()) {
        null
    } else {
        PatientDocumentInput(
            documentId = "DOC-${Clock.System.now().toEpochMilliseconds()}-$cleanId",
            patientId = cleanId,
            documentType = state.documentType.ifBlank { "Unknown" },
            imageUri = state.documentPhotoUri.trim(),
            verificationStatus = state.verificationStatus.ifBlank { "PENDING_REVIEW" },
            extractedFullName = state.extractedFullName.ifBlank { null },
            extractedIdentifier = state.extractedIdentifier.ifBlank { null },
            extractedBirthDate = state.extractedBirthDate.ifBlank { null },
            extractedSex = state.extractedSex.ifBlank { null },
            extractedGuardianName = state.extractedGuardianName.ifBlank { null },
            notes = state.documentNotes.ifBlank { null },
        )
    }
    return RegistrationSubmitPayload(
        patient = PatientRegistrationInput(
            id = cleanId,
            fullName = state.fullName.trim(),
            age = parsedAge,
            sex = state.sex.toReceptionSex(),
            assignedWard = state.assignedWard.ifBlank { null },
            roomBed = state.roomBed.ifBlank { null },
            acuity = state.acuity.ifBlank { "Moderate" },
            isolation = state.isolation.ifBlank { null },
        ),
        triageLevel = parsedTriage,
        clinical = clinical,
        document = document,
    )
}

fun validateRegistrationStep(step: RegistrationStep, state: RegistrationFormState): String? = when (step) {
    RegistrationStep.IDENTITY -> when {
        state.id.isBlank() -> "Patient ID is required."
        state.fullName.isBlank() -> "Full name is required."
        state.age.toIntOrNull() == null -> "Age is required."
        else -> null
    }
    else -> null
}
