package com.egesa.clinic.shared.ui.screens.registration

import com.egesa.clinic.shared.data.DocumentCaptureResult
import kotlinx.datetime.Clock

data class RegistrationFormState(
    val id: String = "PT-${Clock.System.now().toEpochMilliseconds()}",
    val fullName: String = "",
    val age: String = "",
    val sex: String = "female",
    val assignedWard: String = "",
    val roomBed: String = "",
    val acuity: String = "Moderate",
    val isolation: String = "",
    val triageLevel: String = "3",
    val documentType: String = "National ID",
    val documentPhotoUri: String = "",
    val verificationStatus: String = "PENDING_REVIEW",
    val extractedFullName: String = "",
    val extractedIdentifier: String = "",
    val extractedBirthDate: String = "",
    val extractedSex: String = "",
    val extractedGuardianName: String = "",
    val documentNotes: String = "",
    val weightKg: String = "",
    val heightCm: String = "",
    val temperatureC: String = "",
    val systolicBp: String = "",
    val diastolicBp: String = "",
    val pulseBpm: String = "",
    val respiratoryRate: String = "",
    val spo2Percent: String = "",
    val muacCm: String = "",
    val queueDestination: String = "Triage",
    val queuePriority: String = "Routine",
    val queueNote: String = "",
    val documentActionMessage: String? = null,
    val documentActionBusy: Boolean = false,
) {
    fun applyCaptureResult(result: DocumentCaptureResult): RegistrationFormState {
        var next = this
        result.imageUri?.let { next = next.copy(documentPhotoUri = it) }
        result.extractedData.fullName?.let { next = next.copy(extractedFullName = it) }
        result.extractedData.identifier?.let { next = next.copy(extractedIdentifier = it) }
        result.extractedData.birthDate?.let { next = next.copy(extractedBirthDate = it) }
        result.extractedData.sex?.let { next = next.copy(extractedSex = it) }
        result.extractedData.guardianName?.let { next = next.copy(extractedGuardianName = it) }
        return next.copy(documentActionMessage = result.message)
    }

    fun applyExtractedToDemographics(): RegistrationFormState {
        var next = this
        if (extractedIdentifier.isNotBlank()) next = next.copy(id = extractedIdentifier.trim())
        if (extractedFullName.isNotBlank()) next = next.copy(fullName = extractedFullName.trim())
        if (extractedSex.isNotBlank()) next = next.copy(sex = extractedSex.trim())
        return next
    }
}
