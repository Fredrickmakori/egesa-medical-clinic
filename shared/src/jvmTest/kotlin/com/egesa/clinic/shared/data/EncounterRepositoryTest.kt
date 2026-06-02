package com.egesa.clinic.shared.data

import com.egesa.clinic.shared.db.ClinicDatabase
import com.egesa.clinic.shared.db.DatabaseDriverFactory
import com.egesa.clinic.shared.domain.EncounterDiagnosis
import com.egesa.clinic.shared.domain.EncounterPlan
import com.egesa.clinic.shared.domain.ImagingOrder
import com.egesa.clinic.shared.domain.Prescription
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class EncounterRepositoryTest {
    @Test
    fun createsAndLoadsEncounter() = runBlocking {
        val db = ClinicDatabase(DatabaseDriverFactory().createDriver())
        val repository = LocalEncounterRepository(db)

        val created = repository.createEncounter(
            patientId = "PT-100",
            providerId = "DR-100",
            facilityId = "EGESA-CLINIC",
        )
        val loaded = repository.getEncounterById(created.encounterId)

        assertNotNull(loaded)
        assertEquals("PT-100", loaded.encounter.patientId)
    }

    @Test
    fun savesPlanOrdersAndPrescription() = runBlocking {
        val db = ClinicDatabase(DatabaseDriverFactory().createDriver())
        val repository = LocalEncounterRepository(db)
        val now = Clock.System.now().toString()
        val encounter = repository.createEncounter(
            patientId = "PT-101",
            providerId = "DR-200",
            facilityId = "EGESA-CLINIC",
        )
        val updated = repository.updateEncounter(
            bundle = com.egesa.clinic.shared.domain.OpdEncounterBundle(
                encounter = encounter,
                diagnoses = listOf(
                    EncounterDiagnosis(
                        diagnosisId = "DX-001",
                        encounterId = encounter.encounterId,
                        diagnosisText = "Malaria",
                        createdAt = now,
                        updatedAt = now,
                    )
                ),
                plan = EncounterPlan(
                    encounterId = encounter.encounterId,
                    clinicalAdvice = "Hydrate and rest",
                    createdAt = now,
                    updatedAt = now,
                ),
                imagingOrders = listOf(
                    ImagingOrder(
                        orderId = "IMG-001",
                        encounterId = encounter.encounterId,
                        studyName = "CXR",
                        createdAt = now,
                        updatedAt = now,
                    )
                ),
                prescriptions = listOf(
                    Prescription(
                        prescriptionId = "RX-001",
                        encounterId = encounter.encounterId,
                        medicationName = "Artemether",
                        dose = "20mg",
                        createdAt = now,
                        updatedAt = now,
                    )
                )
            ),
            finalize = true,
        )

        assertEquals(1, updated.prescriptions.size)
        assertEquals(1, updated.imagingOrders.size)
        assertEquals("Hydrate and rest", updated.plan?.clinicalAdvice)
    }
}
