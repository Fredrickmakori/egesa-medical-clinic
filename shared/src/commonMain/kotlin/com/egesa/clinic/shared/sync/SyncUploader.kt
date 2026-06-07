package com.egesa.clinic.shared.sync

import com.egesa.clinic.shared.data.ClinicAuth
import com.egesa.clinic.shared.data.ClinicApiProvider
import com.egesa.clinic.shared.data.LocalRepository
import com.egesa.clinic.shared.data.toDto

object SyncUploader {
    suspend fun upload(
        localRepository: LocalRepository,
        entityType: String,
        entityId: String,
        payload: String,
    ): Boolean {
        if (!ClinicAuth.hasToken()) return false
        val api = ClinicApiProvider.api ?: return false

        return when (entityType) {
            "PatientEntity" -> uploadPatient(api, localRepository, entityId)
            "EncounterEntity",
            "VitalSignsEntity",
            "ServiceEventEntity",
            "PatientDocumentEntity",
            "HtsRegisterEntity" -> uploadClinical(api, localRepository, entityType, entityId)
            "LabOrderEntity" -> uploadLabOrder(api, localRepository, entityId)
            else -> false
        }
    }

    private suspend fun uploadPatient(
        api: com.egesa.clinic.shared.data.ClinicApi,
        localRepository: LocalRepository,
        patientId: String
    ): Boolean {
        val patient = localRepository.getPatientById(patientId) ?: return false
        return runCatching { api.uploadPatientChanges(listOf(patient.toDto())) }.isSuccess
    }

    private suspend fun uploadClinical(
        api: com.egesa.clinic.shared.data.ClinicApi,
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
        return runCatching { api.uploadClinicalChanges(batch) }.isSuccess
    }

    private suspend fun uploadLabOrder(
        api: com.egesa.clinic.shared.data.ClinicApi,
        localRepository: LocalRepository,
        orderId: String
    ): Boolean {
        val order = localRepository.getLabOrder(orderId) ?: return false
        return runCatching { api.createLabOrder(order.toDto()) }.isSuccess
    }
}
