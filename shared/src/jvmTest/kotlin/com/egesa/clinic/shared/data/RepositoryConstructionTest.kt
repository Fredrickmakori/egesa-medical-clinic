package com.egesa.clinic.shared.data

import com.egesa.clinic.shared.Sex
import com.egesa.clinic.shared.VisitType
import com.egesa.clinic.shared.db.ClinicDatabase
import com.egesa.clinic.shared.db.DatabaseDriverFactory
import com.egesa.clinic.shared.domain.LabOrder
import com.egesa.clinic.shared.domain.LabOrderItem
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class RepositoryConstructionTest {
    @Test
    fun repositoriesConstructWithoutRecursiveDependencies() {
        runBlocking {
            val db = ClinicDatabase(DatabaseDriverFactory().createDriver())

            val localRepository = LocalRepository(db)
            val labRepository = LocalLabRepository(db)
            val encounterRepository = LocalEncounterRepository(db)

            assertNotNull(localRepository)
            assertNotNull(labRepository)
            assertNotNull(encounterRepository)
        }
    }

    @Test
    fun labRepositoryCreatesAndLoadsOrder() {
        runBlocking {
            val db = ClinicDatabase(DatabaseDriverFactory().createDriver())
            val localRepository = LocalRepository(db)
            val labRepository: LabRepository = LocalLabRepository(db)
            val now = Clock.System.now().toString()

            localRepository.upsertPatient(
                PatientRegistrationInput(
                    id = "PT-LAB-001",
                    fullName = "Lab Test Patient",
                    age = 34,
                    sex = Sex.FEMALE,
                )
            )
            localRepository.createEncounter(
                EncounterInput(
                    encounterId = "ENC-LAB-001",
                    patientId = "PT-LAB-001",
                    encounterDatetime = now,
                    department = "OPD",
                    visitType = VisitType.OUTPATIENT,
                    providerId = "DR-LAB-001",
                    facilityId = "EGESA-CLINIC",
                )
            )

            val created = labRepository.createLabOrder(
                LabOrder(
                    id = "LAB-001",
                    patientId = "PT-LAB-001",
                    encounterId = "ENC-LAB-001",
                    orderedBy = "DR-LAB-001",
                    department = "LAB",
                    clinicalNotes = "Rule out malaria",
                    items = listOf(
                        LabOrderItem(
                            id = "LAB-ITEM-001",
                            orderId = "LAB-001",
                            testId = "MALARIA-RDT",
                            testCode = "MRDT",
                            testName = "Malaria RDT",
                            billingCode = "LAB-MRDT",
                            price = 250.0,
                            orderedAt = now,
                            updatedAt = now,
                        )
                    ),
                    createdAt = now,
                    updatedAt = now,
                )
            )
            val loaded = labRepository.getLabOrder(created.id)

            assertNotNull(loaded)
            assertEquals("PT-LAB-001", loaded.patientId)
            assertEquals("ENC-LAB-001", loaded.encounterId)
            assertEquals(1, loaded.items.size)
            assertEquals("Malaria RDT", loaded.items.first().testName)
        }
    }
}
