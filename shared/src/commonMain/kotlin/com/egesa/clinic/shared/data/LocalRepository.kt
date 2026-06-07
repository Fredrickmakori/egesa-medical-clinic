package com.egesa.clinic.shared.data

import com.egesa.clinic.shared.*
import com.egesa.clinic.shared.domain.LabOrder
import com.egesa.clinic.shared.domain.LabOrderStatus
import com.egesa.clinic.shared.domain.LabResult
import com.egesa.clinic.shared.domain.LabSample
import com.egesa.clinic.shared.sync.SyncNotifier
import com.egesa.clinic.shared.db.ClinicDatabase
import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import app.cash.sqldelight.async.coroutines.awaitAsOne
import kotlinx.datetime.Clock

data class EncounterInput(
    val encounterId: String,
    val serverId: String? = null,
    val patientId: String,
    val encounterDatetime: String,
    val department: String,
    val visitType: VisitType,
    val providerId: String? = null,
    val facilityId: String,
    val locationId: String? = null,
    val sourceType: String? = null,
    val sourceId: String? = null,
    val status: String = "DRAFT",
    val version: Long = 1,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val deletedAt: String? = null,
    val nursingNotes: String? = null,
)

data class VitalSignsInput(
    val vitalSignsId: String,
    val encounterId: String,
    val weightKg: Double? = null,
    val heightCm: Double? = null,
    val bmi: Double? = null,
    val temperatureC: Double? = null,
    val systolicBp: Long? = null,
    val diastolicBp: Long? = null,
    val pulseBpm: Long? = null,
    val respiratoryRate: Long? = null,
    val spo2Percent: Double? = null,
    val muacCm: Double? = null,
    val recordedAt: String = Clock.System.now().toString()
)

data class DiagnosisInput(
    val diagnosisId: String,
    val serverId: String? = null,
    val encounterId: String,
    val diagnosisText: String,
    val isPrimary: Boolean,
    val codeSystem: String? = null,
    val diagnosisCode: String? = null,
    val version: Long = 1,
    val createdAt: String = Clock.System.now().toString(),
    val updatedAt: String = Clock.System.now().toString(),
    val deletedAt: String? = null,
)

data class MedicationOrderInput(
    val medicationOrderId: String,
    val serverId: String? = null,
    val encounterId: String,
    val medicationName: String,
    val dose: String? = null,
    val route: String? = null,
    val frequency: String? = null,
    val duration: String? = null,
    val instructions: String? = null,
    val version: Long = 1,
    val createdAt: String = Clock.System.now().toString(),
    val updatedAt: String = Clock.System.now().toString(),
    val deletedAt: String? = null,
)

data class EncounterOutcomeInput(
    val outcomeId: String,
    val encounterId: String,
    val disposition: Disposition,
    val referralTo: String? = null,
    val admitted: Boolean = false,
    val dischargeNotes: String? = null
)

data class HtsRegisterInput(
    val htsId: String,
    val encounterId: String,
    val serialNumber: String? = null,
    val htsNumber: String? = null,
    val populationType: String,
    val testingPoint: String,
    val test1Result: HivStatus? = null,
    val test2Result: HivStatus? = null,
    val finalResult: HivStatus,
    val coupleTesting: String? = null,
    val recencyTestResult: String? = null,
    val referredTo: String? = null,
    val linkedToCare: Boolean = false,
    val remarks: String? = null
)

data class PatientRegistrationInput(
    val id: String,
    val fullName: String,
    val age: Int,
    val sex: Sex,
    val status: String = "Checked in",
    val assignedWard: String? = null,
    val roomBed: String? = null,
    val acuity: String = "Moderate",
    val isolation: String? = null
)

data class RegistrationClinicalInput(
    val weightKg: Double? = null,
    val heightCm: Double? = null,
    val bmi: Double? = null,
    val temperatureC: Double? = null,
    val systolicBp: Long? = null,
    val diastolicBp: Long? = null,
    val pulseBpm: Long? = null,
    val respiratoryRate: Long? = null,
    val spo2Percent: Double? = null,
    val muacCm: Double? = null,
    val queueDestination: String? = null,
    val queuePriority: String? = null,
    val queueNote: String? = null
)

data class PatientDocumentInput(
    val documentId: String,
    val patientId: String,
    val documentType: String,
    val imageUri: String,
    val verificationStatus: String = "PENDING",
    val extractedFullName: String? = null,
    val extractedIdentifier: String? = null,
    val extractedBirthDate: String? = null,
    val extractedSex: String? = null,
    val extractedGuardianName: String? = null,
    val notes: String? = null,
    val capturedAt: String = Clock.System.now().toString()
)

data class ServiceEventInput(
    val serviceEventId: String,
    val encounterId: String,
    val program: String,
    val indicatorCategory: String,
    val serviceCode: String? = null,
    val valueText: String? = null,
    val quantity: Long = 1,
    val eventDatetime: String = Clock.System.now().toString()
)

data class PatientChart(
    val patient: Patient,
    val encounters: List<PatientChartEncounter> = emptyList(),
    val documents: List<PatientDocumentDto> = emptyList()
)

data class PatientChartEncounter(
    val encounterId: String,
    val patientId: String,
    val encounterDatetime: String,
    val department: String,
    val visitType: VisitType,
    val providerId: String? = null,
    val facilityId: String,
    val syncState: String = "LOCAL_ONLY",
    val vitals: List<String> = emptyList(),
    val diagnoses: List<String> = emptyList(),
    val medications: List<String> = emptyList(),
    val serviceEvents: List<String> = emptyList(),
    val outcome: String? = null,
    val referralTo: String? = null
)

data class MohHtsTally(
    val gender: Sex,
    val ageGroup: String,
    val result: HivStatus,
    val count: Int
)

class LocalRepository(val database: ClinicDatabase) {

    private val dbQueries = database.clinicDatabaseQueries
    private val labRepository: LabRepository = LocalLabRepository(database)

    // ── Hospital Profile ──────────────────────────────────────────────────

    suspend fun getHospitalProfile(): HospitalProfile? {
        return dbQueries.selectHospitalProfile().awaitAsOneOrNull()?.let {
            HospitalProfile(
                id = it.id,
                name = it.name,
                address = it.address,
                logoUri = it.logoUri,
                phone = it.phone,
                email = it.email,
                registeredAt = it.registeredAt,
                isOnboarded = it.isOnboarded == 1L,
            )
        }
    }

    suspend fun saveHospitalProfile(profile: HospitalProfile) {
        dbQueries.insertHospitalProfile(
            id = profile.id,
            name = profile.name,
            address = profile.address,
            logoUri = profile.logoUri,
            phone = profile.phone,
            email = profile.email,
            registeredAt = profile.registeredAt,
            isOnboarded = if (profile.isOnboarded) 1L else 0L,
        )
    }

    suspend fun isOnboarded(): Boolean {
        return getHospitalProfile()?.isOnboarded == true
    }

    // ── Staff ─────────────────────────────────────────────────────────────

    suspend fun getAllStaff(): List<StaffMember> {
        return dbQueries.selectAllStaff().awaitAsList().map {
            StaffMember(
                id = it.id,
                fullName = it.fullName,
                role = UserRole.valueOf(it.role),
                department = it.department ?: ""
            )
        }
    }

    suspend fun insertStaff(staff: StaffMember, pin: String? = null) {
        dbQueries.insertStaff(
            id = staff.id,
            fullName = staff.fullName,
            role = staff.role.name,
            department = staff.department,
            pin = pin,
            active = 1,
            lastUpdated = Clock.System.now().toString()
        )
    }

    suspend fun deleteStaff(staffId: String) {
        dbQueries.deleteStaffById(staffId)
        queueSync("StaffMemberEntity", staffId, "DELETE", """{"id":"$staffId"}""")
    }

    suspend fun getAllPatients(): List<Patient> =
        dbQueries.selectAllPatients().awaitAsList().map {
            Patient(
                id = it.id,
                fullName = it.fullName,
                age = it.age.toInt(),
                sex = LegacyCodeMapper.sex(it.sex),
                status = it.status,
                assignedWard = it.assignedWard,
                roomBed = it.roomBed,
                acuity = it.acuity,
                isolation = it.isolation
            )
        }

    suspend fun getPatientById(patientId: String): Patient? =
        dbQueries.selectPatientById(patientId).awaitAsOneOrNull()?.let {
            Patient(
                id = it.id,
                fullName = it.fullName,
                age = it.age.toInt(),
                sex = LegacyCodeMapper.sex(it.sex),
                status = it.status,
                assignedWard = it.assignedWard,
                roomBed = it.roomBed,
                acuity = it.acuity,
                isolation = it.isolation
            )
        }

    suspend fun buildClinicalSyncBatch(entityType: String, entityId: String): ClinicalSyncBatchDto? =
        when (entityType) {
            "EncounterEntity" -> {
                val row = dbQueries.selectEncounterById(entityId).awaitAsOneOrNull() ?: return null
                ClinicalSyncBatchDto(
                    encounters = listOf(
                        EncounterDto(
                            encounterId = row.encounter_id,
                            patientId = row.patient_id,
                            encounterDatetime = row.encounter_datetime,
                            department = row.department,
                            visitType = row.visit_type,
                            providerId = row.provider_id,
                            facilityId = row.facility_id,
                            locationId = row.location_id,
                            sourceType = row.source_type,
                            sourceId = row.source_id,
                            status = row.status,
                            version = row.version.toInt(),
                            updatedAt = row.updated_at,
                            nursingNotes = row.nursing_notes,
                            syncState = row.sync_state,
                        )
                    )
                )
            }
            "VitalSignsEntity" -> {
                val row = dbQueries.selectVitalSignsById(entityId).awaitAsOneOrNull() ?: return null
                ClinicalSyncBatchDto(
                    vitalSigns = listOf(
                        VitalSignsDto(
                            vitalSignsId = row.vital_signs_id,
                            encounterId = row.encounter_id,
                            weightKg = row.weight_kg,
                            heightCm = row.height_cm,
                            bmi = row.bmi,
                            temperatureC = row.temperature_c,
                            systolicBp = row.systolic_bp,
                            diastolicBp = row.diastolic_bp,
                            pulseBpm = row.pulse_bpm,
                            respiratoryRate = row.respiratory_rate,
                            spo2Percent = row.spo2_percent,
                            muacCm = row.muac_cm,
                            recordedAt = row.recorded_at
                        )
                    )
                )
            }
            "ServiceEventEntity" -> {
                val row = dbQueries.selectServiceEventById(entityId).awaitAsOneOrNull() ?: return null
                ClinicalSyncBatchDto(
                    serviceEvents = listOf(
                        ServiceEventDto(
                            serviceEventId = row.service_event_id,
                            encounterId = row.encounter_id,
                            program = row.program,
                            indicatorCategory = row.indicator_category,
                            serviceCode = row.service_code,
                            valueText = row.value_text,
                            quantity = row.quantity ?: 1L,
                            eventDatetime = row.event_datetime,
                            syncState = row.sync_state
                        )
                    )
                )
            }
            "PatientDocumentEntity" -> {
                val row = dbQueries.selectPatientDocumentById(entityId).awaitAsOneOrNull() ?: return null
                ClinicalSyncBatchDto(
                    patientDocuments = listOf(
                        PatientDocumentDto(
                            documentId = row.document_id,
                            patientId = row.patient_id,
                            documentType = row.document_type,
                            imageUri = row.image_uri,
                            verificationStatus = row.verification_status,
                            extractedFullName = row.extracted_full_name,
                            extractedIdentifier = row.extracted_identifier,
                            extractedBirthDate = row.extracted_birth_date,
                            extractedSex = row.extracted_sex,
                            extractedGuardianName = row.extracted_guardian_name,
                            notes = row.notes,
                            capturedAt = row.captured_at
                        )
                    )
                )
            }
            "HtsRegisterEntity" -> {
                val row = dbQueries.selectHtsById(entityId).awaitAsOneOrNull() ?: return null
                ClinicalSyncBatchDto(
                    htsEntries = listOf(
                        HtsRegisterDto(
                            htsId = row.hts_id,
                            encounterId = row.encounter_id,
                            serialNumber = row.serial_number,
                            htsNumber = row.hts_number,
                            populationType = row.population_type,
                            testingPoint = row.testing_point,
                            test1Result = row.test_1_result,
                            test2Result = row.test_2_result,
                            finalResult = row.final_result,
                            coupleTesting = row.couple_testing,
                            recencyTestResult = row.recency_test_result,
                            referredTo = row.referred_to,
                            linkedToCare = row.linked_to_care == 1L,
                            remarks = row.remarks
                        )
                    )
                )
            }
            else -> null
        }

    suspend fun upsertPatient(input: PatientRegistrationInput) {
        dbQueries.upsertPatient(
            id = input.id,
            fullName = input.fullName,
            age = input.age.toLong(),
            sex = input.sex.code,
            status = input.status,
            assignedWard = input.assignedWard,
            roomBed = input.roomBed,
            acuity = input.acuity,
            isolation = input.isolation,
            version = 1,
            syncedAt = null
        )
        queueSync("PatientEntity", input.id, "UPSERT", "{}")
    }

    suspend fun getPatientChart(patientId: String): PatientChart? {
        val row = dbQueries.selectPatientById(patientId).awaitAsOneOrNull() ?: return null
        val patient = Patient(
            id = row.id,
            fullName = row.fullName,
            age = row.age.toInt(),
            sex = LegacyCodeMapper.sex(row.sex),
            status = row.status,
            assignedWard = row.assignedWard,
            roomBed = row.roomBed,
            acuity = row.acuity,
            isolation = row.isolation
        )
        return PatientChart(
            patient = patient,
            encounters = dbQueries.selectEncountersByPatient(patient_id = patientId).awaitAsList().map {
                val vitals = dbQueries.selectVitalSignsByEncounter(encounter_id = it.encounter_id).awaitAsList().flatMap { vital ->
                    listOfNotNull(
                        vital.weight_kg?.let { value -> "Weight ${value}kg" },
                        vital.height_cm?.let { value -> "Height ${value}cm" },
                        vital.bmi?.let { value -> "BMI $value" },
                        vital.temperature_c?.let { value -> "Temp ${value}C" },
                        vital.systolic_bp?.let { systolic -> "BP $systolic/${vital.diastolic_bp ?: "-"}" },
                        vital.pulse_bpm?.let { value -> "Pulse $value bpm" },
                        vital.respiratory_rate?.let { value -> "RR $value" },
                        vital.spo2_percent?.let { value -> "SpO2 $value%" },
                        vital.muac_cm?.let { value -> "MUAC ${value}cm" }
                    )
                }
                val outcome = dbQueries.selectEncounterOutcome(encounter_id = it.encounter_id).awaitAsOneOrNull()
                PatientChartEncounter(
                    encounterId = it.encounter_id,
                    patientId = it.patient_id,
                    encounterDatetime = it.encounter_datetime,
                    department = it.department,
                    visitType = LegacyCodeMapper.visitType(it.visit_type),
                    providerId = it.provider_id,
                    facilityId = it.facility_id,
                    syncState = it.sync_state,
                    vitals = vitals,
                    diagnoses = dbQueries.selectDiagnosisByEncounter(encounter_id = it.encounter_id).awaitAsList()
                        .map { diagnosis -> diagnosis.diagnosis_text },
                    medications = dbQueries.selectMedicationOrdersByEncounter(encounter_id = it.encounter_id).awaitAsList()
                        .map { medication -> listOfNotNull(medication.medication_name, medication.dose, medication.frequency).joinToString(" ") },
                    serviceEvents = dbQueries.selectServiceEventsByEncounter(encounter_id = it.encounter_id).awaitAsList()
                        .map { event -> listOfNotNull(event.program, event.indicator_category, event.value_text).joinToString(" - ") },
                    outcome = outcome?.disposition,
                    referralTo = outcome?.referral_to
                )
            },
            documents = dbQueries.selectDocumentsByPatient(patient_id = patientId).awaitAsList().map {
                PatientDocumentDto(
                    documentId = it.document_id,
                    patientId = it.patient_id,
                    documentType = it.document_type,
                    imageUri = it.image_uri,
                    verificationStatus = it.verification_status,
                    extractedFullName = it.extracted_full_name,
                    extractedIdentifier = it.extracted_identifier,
                    extractedBirthDate = it.extracted_birth_date,
                    extractedSex = it.extracted_sex,
                    extractedGuardianName = it.extracted_guardian_name,
                    notes = it.notes,
                    capturedAt = it.captured_at
                )
            }
        )
    }

    suspend fun getActiveVisitSummaries(): List<PatientVisitSummary> =
        dbQueries.selectActiveVisitSummaries().awaitAsList().map {
            PatientVisitSummary(
                patientId = it.patient_id,
                fullName = it.full_name,
                age = it.age.toInt(),
                sex = LegacyCodeMapper.sex(it.sex),
                encounterId = it.encounter_id,
                department = it.department,
                visitType = LegacyCodeMapper.visitType(it.visit_type),
                encounterDatetime = it.encounter_datetime,
                syncState = it.sync_state
            )
        }

    suspend fun getServiceIndicatorSummary(startDate: String, endDate: String): List<ServiceIndicatorSummary> =
        dbQueries.getServiceIndicatorSummary(startDate, endDate).awaitAsList().map {
            ServiceIndicatorSummary(
                program = it.program,
                indicatorCategory = it.indicator_category,
                count = it.count,
                quantity = it.quantity ?: 0L
            )
        }

    suspend fun createEncounter(input: EncounterInput) {
        val updatedAt = input.updatedAt ?: input.encounterDatetime
        val createdAt = input.createdAt ?: input.encounterDatetime
        dbQueries.insertEncounter(
            encounter_id = input.encounterId,
            server_id = input.serverId,
            patient_id = input.patientId,
            encounter_datetime = input.encounterDatetime,
            department = input.department,
            visit_type = input.visitType.code,
            provider_id = input.providerId,
            facility_id = input.facilityId,
            location_id = input.locationId,
            source_type = input.sourceType,
            source_id = input.sourceId,
            status = input.status,
            version = input.version,
            created_at = createdAt,
            updated_at = updatedAt,
            deleted_at = input.deletedAt,
            nursing_notes = input.nursingNotes,
            sync_state = "LOCAL_ONLY",
        )
        queueSync("EncounterEntity", input.encounterId, "UPSERT", "{}")
    }

    suspend fun getEncountersByPatient(patientId: String) =
        dbQueries.selectEncountersByPatient(patient_id = patientId).awaitAsList()

    suspend fun deleteEncounter(encounterId: String) {
        dbQueries.deleteEncounter(encounter_id = encounterId)
    }

    suspend fun upsertVitalSigns(input: VitalSignsInput) {
        dbQueries.insertVitalSigns(
            vital_signs_id = input.vitalSignsId,
            encounter_id = input.encounterId,
            weight_kg = input.weightKg,
            height_cm = input.heightCm,
            bmi = input.bmi,
            temperature_c = input.temperatureC,
            systolic_bp = input.systolicBp,
            diastolic_bp = input.diastolicBp,
            pulse_bpm = input.pulseBpm,
            respiratory_rate = input.respiratoryRate,
            spo2_percent = input.spo2Percent,
            muac_cm = input.muacCm,
            recorded_at = input.recordedAt
        )
        queueSync("VitalSignsEntity", input.vitalSignsId, "UPSERT", "{}")
    }

    suspend fun insertPatientDocument(input: PatientDocumentInput) {
        dbQueries.insertPatientDocument(
            document_id = input.documentId,
            patient_id = input.patientId,
            document_type = input.documentType,
            image_uri = input.imageUri,
            verification_status = input.verificationStatus,
            extracted_full_name = input.extractedFullName,
            extracted_identifier = input.extractedIdentifier,
            extracted_birth_date = input.extractedBirthDate,
            extracted_sex = input.extractedSex,
            extracted_guardian_name = input.extractedGuardianName,
            notes = input.notes,
            captured_at = input.capturedAt
        )
        queueSync("PatientDocumentEntity", input.documentId, "UPSERT", "{}")
    }

    suspend fun upsertServiceEvent(input: ServiceEventInput) {
        dbQueries.insertServiceEvent(
            service_event_id = input.serviceEventId,
            encounter_id = input.encounterId,
            program = input.program,
            indicator_category = input.indicatorCategory,
            service_code = input.serviceCode,
            value_text = input.valueText,
            quantity = input.quantity,
            event_datetime = input.eventDatetime,
            sync_state = "LOCAL_ONLY"
        )
        queueSync("ServiceEventEntity", input.serviceEventId, "UPSERT", "{}")
    }

    suspend fun getVitalSignsByEncounter(encounterId: String) =
        dbQueries.selectVitalSignsByEncounter(encounter_id = encounterId).awaitAsList()

    suspend fun upsertDiagnosis(input: DiagnosisInput) {
        dbQueries.insertDiagnosis(
            diagnosis_id = input.diagnosisId,
            server_id = input.serverId,
            encounter_id = input.encounterId,
            diagnosis_text = input.diagnosisText,
            is_primary = if (input.isPrimary) 1L else 0L,
            code_system = input.codeSystem,
            diagnosis_code = input.diagnosisCode,
            version = input.version,
            created_at = input.createdAt,
            updated_at = input.updatedAt,
            deleted_at = input.deletedAt,
        )
    }

    suspend fun getDiagnosisByEncounter(encounterId: String) =
        dbQueries.selectDiagnosisByEncounter(encounter_id = encounterId).awaitAsList()

    suspend fun upsertMedicationOrder(input: MedicationOrderInput) {
        dbQueries.insertMedicationOrder(
            medication_order_id = input.medicationOrderId,
            server_id = input.serverId,
            encounter_id = input.encounterId,
            medication_name = input.medicationName,
            dose = input.dose,
            route = input.route,
            frequency = input.frequency,
            duration = input.duration,
            instructions = input.instructions,
            version = input.version,
            created_at = input.createdAt,
            updated_at = input.updatedAt,
            deleted_at = input.deletedAt,
        )
    }

    suspend fun getMedicationOrdersByEncounter(encounterId: String) =
        dbQueries.selectMedicationOrdersByEncounter(encounter_id = encounterId).awaitAsList()

    suspend fun upsertEncounterOutcome(input: EncounterOutcomeInput) {
        dbQueries.upsertEncounterOutcome(
            outcome_id = input.outcomeId,
            encounter_id = input.encounterId,
            disposition = input.disposition.code,
            referral_to = input.referralTo,
            admitted = if (input.admitted) 1L else 0L,
            discharge_notes = input.dischargeNotes
        )
    }

    suspend fun getEncounterOutcome(encounterId: String) =
        dbQueries.selectEncounterOutcome(encounter_id = encounterId).awaitAsOneOrNull()

    suspend fun upsertHtsEntry(input: HtsRegisterInput) {
        dbQueries.insertHtsEntry(
            hts_id = input.htsId,
            encounter_id = input.encounterId,
            serial_number = input.serialNumber,
            hts_number = input.htsNumber,
            population_type = input.populationType,
            testing_point = input.testingPoint,
            test_1_result = input.test1Result?.code,
            test_2_result = input.test2Result?.code,
            final_result = input.finalResult.code,
            couple_testing = input.coupleTesting,
            recency_test_result = input.recencyTestResult,
            referred_to = input.referredTo,
            linked_to_care = if (input.linkedToCare) 1L else 0L,
            remarks = input.remarks
        )
        queueSync("HtsRegisterEntity", input.htsId, "UPSERT", "{}")
    }

    suspend fun getHtsReportSummary(startDate: String, endDate: String): List<MohHtsTally> {
        val rawData = dbQueries.getHtsSummary(startDate, endDate).awaitAsList()
        
        // Group and tally in Kotlin for maximum flexibility and to avoid complex SQLDialect issues
        return rawData.map { item ->
            val ageGroup = when {
                item.age < 1 -> "0-1"
                item.age in 1L..9L -> "1-9"
                item.age in 10L..14L -> "10-14"
                item.age in 15L..19L -> "15-19"
                item.age in 20L..24L -> "20-24"
                else -> "25+"
            }
            MohHtsTally(
                gender = LegacyCodeMapper.sex(item.sex),
                ageGroup = ageGroup,
                result = LegacyCodeMapper.hivStatus(item.final_result),
                count = 1
            )
        }.groupBy { "${it.gender.code}|${it.ageGroup}|${it.result.code}" }
            .map { (key, list) ->
                val parts = key.split("|")
                MohHtsTally(
                    gender = LegacyCodeMapper.sex(parts[0]),
                    ageGroup = parts[1],
                    result = LegacyCodeMapper.hivStatus(parts[2]),
                    count = list.size
                )
            }
    }

    suspend fun queueSync(entityType: String, entityId: String, operation: String, payload: String) {
        dbQueries.insertSyncItem(
            id = "SYNC-${Clock.System.now().toEpochMilliseconds()}-$entityId",
            entityType = entityType,
            entityId = entityId,
            operation = operation,
            payload = payload,
            createdAt = Clock.System.now().toString()
        )
        SyncNotifier.requestSync()
    }

    suspend fun getPendingSync() = dbQueries.selectPendingSync().awaitAsList()

    suspend fun deleteSyncItem(id: String) {
        dbQueries.deleteSyncItem(id = id)
    }

    suspend fun getAllSchedules(): List<Schedule> {
        return dbQueries.selectAllSchedules().awaitAsList().map {
            Schedule(
                id = it.id,
                actorType = it.actor_type,
                actorId = it.actor_id,
                name = it.name,
                active = it.active == 1L
            )
        }
    }

    suspend fun insertSchedule(schedule: Schedule) {
        dbQueries.insertSchedule(
            id = schedule.id,
            actor_type = schedule.actorType,
            actor_id = schedule.actorId,
            name = schedule.name,
            active = if (schedule.active) 1L else 0L
        )
        val payload = """{"id":"${schedule.id}","actor_type":"${schedule.actorType}","actor_id":"${schedule.actorId}","name":"${schedule.name}","active":${schedule.active}}"""
        queueSync("ScheduleEntity", schedule.id, "UPSERT", payload)
    }

    suspend fun getSlotsBySchedule(scheduleId: String): List<Slot> {
        return dbQueries.selectSlotsBySchedule(scheduleId).awaitAsList().map {
            Slot(
                id = it.id,
                scheduleId = it.schedule_id,
                startTime = it.start_time,
                endTime = it.end_time,
                status = it.status
            )
        }
    }

    suspend fun insertSlot(slot: Slot) {
        dbQueries.insertSlot(
            id = slot.id,
            schedule_id = slot.scheduleId,
            start_time = slot.startTime,
            end_time = slot.endTime,
            status = slot.status
        )
        val payload = """{"id":"${slot.id}","schedule_id":"${slot.scheduleId}","start_time":"${slot.startTime}","end_time":"${slot.endTime}","status":"${slot.status}"}"""
        queueSync("SlotEntity", slot.id, "UPSERT", payload)
    }

    suspend fun updateSlotStatus(slotId: String, status: String) {
        dbQueries.updateSlotStatus(status = status, id = slotId)
        val payload = """{"status":"$status"}"""
        queueSync("SlotEntity", slotId, "UPDATE_STATUS", payload)
    }

    suspend fun getAppointmentsByPatient(patientId: String): List<Appointment> {
        return dbQueries.selectAppointmentsByPatient(patientId).awaitAsList().map {
            Appointment(
                id = it.id,
                patientId = it.patient_id,
                scheduleId = it.schedule_id,
                slotId = it.slot_id,
                status = it.status,
                appointmentType = it.appointment_type,
                reason = it.reason,
                startTime = it.start_time,
                endTime = it.end_time,
                createdAt = it.created_at,
                updatedAt = it.updated_at
            )
        }
    }

    suspend fun getAppointmentsBySchedule(scheduleId: String): List<Appointment> {
        return dbQueries.selectAppointmentsBySchedule(scheduleId).awaitAsList().map {
            Appointment(
                id = it.id,
                patientId = it.patient_id,
                scheduleId = it.schedule_id,
                slotId = it.slot_id,
                status = it.status,
                appointmentType = it.appointment_type,
                reason = it.reason,
                startTime = it.start_time,
                endTime = it.end_time,
                createdAt = it.created_at,
                updatedAt = it.updated_at
            )
        }
    }

    suspend fun insertAppointment(appointment: Appointment) {
        dbQueries.insertAppointment(
            id = appointment.id,
            patient_id = appointment.patientId,
            schedule_id = appointment.scheduleId,
            slot_id = appointment.slotId,
            status = appointment.status,
            appointment_type = appointment.appointmentType,
            reason = appointment.reason,
            start_time = appointment.startTime,
            end_time = appointment.endTime,
            created_at = appointment.createdAt,
            updated_at = appointment.updatedAt
        )
        val payload = """{"id":"${appointment.id}","patient_id":"${appointment.patientId}","schedule_id":"${appointment.scheduleId}","slot_id":${appointment.slotId?.let { "\"$it\"" } ?: "null"},"status":"${appointment.status}","appointment_type":"${appointment.appointmentType}","reason":${appointment.reason?.let { "\"$it\"" } ?: "null"},"start_time":"${appointment.startTime}","end_time":"${appointment.endTime}","created_at":"${appointment.createdAt}","updated_at":"${appointment.updatedAt}"}"""
        queueSync("AppointmentEntity", appointment.id, "UPSERT", payload)
    }

    suspend fun updateAppointmentStatus(appointmentId: String, status: String) {
        val existing = dbQueries.selectAppointmentById(appointmentId).awaitAsOneOrNull() ?: return
        insertAppointment(
            Appointment(
                id = existing.id,
                patientId = existing.patient_id,
                scheduleId = existing.schedule_id,
                slotId = existing.slot_id,
                status = status,
                appointmentType = existing.appointment_type,
                reason = existing.reason,
                startTime = existing.start_time,
                endTime = existing.end_time,
                createdAt = existing.created_at,
                updatedAt = Clock.System.now().toString(),
            )
        )
    }

    suspend fun deleteAppointment(appointmentId: String) {
        dbQueries.deleteAppointmentById(appointmentId)
        queueSync("AppointmentEntity", appointmentId, "DELETE", """{"id":"$appointmentId"}""")
    }

    suspend fun checkOverlappingAppointments(scheduleId: String, startTime: String, endTime: String): Boolean {
        return dbQueries.checkOverlappingAppointments(
            schedule_id = scheduleId,
            start_time = endTime,
            end_time = startTime
        ).awaitAsOne() > 0L
    }

    suspend fun seedAdminIfEmpty() {
        if (getAllStaff().none { it.role == UserRole.ADMIN }) {
            insertStaff(
                StaffMember(
                    id = "ADMIN-001",
                    fullName = "System Admin",
                    role = UserRole.ADMIN,
                    department = "Administration"
                ),
                pin = "1234"
            )
        }
    }

    suspend fun createLabOrder(order: LabOrder): LabOrder = labRepository.createLabOrder(order)

    suspend fun getLabOrder(id: String): LabOrder? = labRepository.getLabOrder(id)

    suspend fun getLabOrdersForPatient(patientId: String): List<LabOrder> =
        labRepository.getLabOrdersForPatient(patientId)

    suspend fun getLabWorklist(query: LabWorklistQuery): List<LabOrder> =
        labRepository.getWorklist(query)

    suspend fun updateLabOrderStatus(orderId: String, status: LabOrderStatus, actorId: String): LabOrder? =
        labRepository.updateOrderStatus(orderId, status, actorId)

    suspend fun saveLabSample(sample: LabSample): LabSample =
        labRepository.saveSample(sample)

    suspend fun saveLabResults(orderId: String, results: List<LabResult>, actorId: String): List<LabResult> =
        labRepository.saveResults(orderId, results, actorId)
}

