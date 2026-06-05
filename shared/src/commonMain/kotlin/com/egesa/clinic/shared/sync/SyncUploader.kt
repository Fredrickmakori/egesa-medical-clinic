package com.egesa.clinic.shared.sync

import com.egesa.clinic.shared.data.ClinicAuth
import com.egesa.clinic.shared.data.FakeRepository
import com.egesa.clinic.shared.data.LocalRepository

object SyncUploader {
    suspend fun upload(
        localRepository: LocalRepository,
        entityType: String,
        entityId: String,
        payload: String,
    ): Boolean {
        if (!ClinicAuth.hasToken()) return false

        return when (entityType) {
            "PatientEntity" -> uploadPatient(localRepository, entityId)
            "EncounterEntity",
            "VitalSignsEntity",
            "ServiceEventEntity",
            "PatientDocumentEntity",
            "HtsRegisterEntity" -> uploadClinical(localRepository, entityType, entityId)
            "LabOrderEntity", "LabSampleEntity", "LabResultEntity" -> {
                // Local-first until lab sync payload contract is finalized.
                payload.isNotBlank()
            }
            "ScheduleEntity", "SlotEntity", "AppointmentEntity" -> {
                // Local-only scheduling data until server routes exist.
                payload.isNotBlank()
            }
            else -> false
        }
    }

    private suspend fun uploadPatient(localRepository: LocalRepository, patientId: String): Boolean {
        val patient = localRepository.getPatientById(patientId) ?: return false
        return FakeRepository.uploadPatientChanges(listOf(patient)).isSuccess
    }

    private suspend fun uploadClinical(
        localRepository: LocalRepository,
        entityType: String,
        entityId: String,
    ): Boolean {
        val batch = localRepository.buildClinicalSyncBatch(entityType, entityId) ?: return false
        if (batch.encounters.isEmpty() &&
            batch.vitalSigns.isEmpty() &&
            batch.serviceEvents.isEmpty() &&
            batch.patientDocuments.isEmpty() &&
            batch.htsEntries.isEmpty()
        ) {
            return false
        }
        return FakeRepository.uploadClinicalChanges(batch).isSuccess
    }
}
