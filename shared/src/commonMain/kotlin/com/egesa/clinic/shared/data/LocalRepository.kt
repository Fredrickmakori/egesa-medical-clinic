package com.egesa.clinic.shared.data

import com.egesa.clinic.shared.*
import com.egesa.clinic.shared.db.ClinicDatabase
import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import app.cash.sqldelight.async.coroutines.awaitAsOne
import kotlinx.datetime.Clock

data class EncounterInput(
    val encounterId: String,
    val patientId: String,
    val encounterDatetime: String,
    val department: String,
    val visitType: VisitType,
    val providerId: String? = null,
    val facilityId: String
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
    val encounterId: String,
    val diagnosisText: String,
    val isPrimary: Boolean,
    val codeSystem: String? = null,
    val diagnosisCode: String? = null
)

data class MedicationOrderInput(
    val medicationOrderId: String,
    val encounterId: String,
    val medicationName: String,
    val dose: String? = null,
    val route: String? = null,
    val frequency: String? = null,
    val duration: String? = null,
    val instructions: String? = null
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
    val encounters: List<EncounterDto> = emptyList(),
    val documents: List<PatientDocumentDto> = emptyList()
)

data class MohHtsTally(
    val gender: Sex,
    val ageGroup: String,
    val result: HivStatus,
    val count: Int
)

class LocalRepository(database: ClinicDatabase) {

    private val dbQueries = database.clinicDatabaseQueries

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
                EncounterDto(
                    encounterId = it.encounter_id,
                    patientId = it.patient_id,
                    encounterDatetime = it.encounter_datetime,
                    department = it.department,
                    visitType = it.visit_type,
                    providerId = it.provider_id,
                    facilityId = it.facility_id,
                    syncState = it.sync_state
                )
            },
            documents = dbQueries.selectPatientDocuments(patient_id = patientId).awaitAsList().map {
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

    suspend fun createEncounter(input: EncounterInput) {
        dbQueries.insertEncounter(
            encounter_id = input.encounterId,
            patient_id = input.patientId,
            encounter_datetime = input.encounterDatetime,
            department = input.department,
            visit_type = input.visitType.code,
            provider_id = input.providerId,
            facility_id = input.facilityId
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
            encounter_id = input.encounterId,
            diagnosis_text = input.diagnosisText,
            is_primary = if (input.isPrimary) 1L else 0L,
            code_system = input.codeSystem,
            diagnosis_code = input.diagnosisCode
        )
    }

    suspend fun getDiagnosisByEncounter(encounterId: String) =
        dbQueries.selectDiagnosisByEncounter(encounter_id = encounterId).awaitAsList()

    suspend fun upsertMedicationOrder(input: MedicationOrderInput) {
        dbQueries.insertMedicationOrder(
            medication_order_id = input.medicationOrderId,
            encounter_id = input.encounterId,
            medication_name = input.medicationName,
            dose = input.dose,
            route = input.route,
            frequency = input.frequency,
            duration = input.duration,
            instructions = input.instructions
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
}

