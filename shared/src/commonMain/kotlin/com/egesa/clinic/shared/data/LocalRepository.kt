package com.egesa.clinic.shared.data

import com.egesa.clinic.shared.StaffMember
import com.egesa.clinic.shared.UserRole
import com.egesa.clinic.shared.db.ClinicDatabase
import com.egesa.clinic.shared.db.DatabaseDriverFactory
import kotlinx.datetime.Clock

data class EncounterInput(
    val encounterId: String,
    val patientId: String,
    val encounterDatetime: String,
    val department: String,
    val visitType: String,
    val providerId: String? = null,
    val facilityId: String
)

data class VitalSignsInput(
    val vitalSignsId: String,
    val encounterId: String,
    val weightKg: Double? = null,
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
    val disposition: String,
    val referralTo: String? = null,
    val admitted: Boolean = false,
    val dischargeNotes: String? = null
)

class LocalRepository(databaseDriverFactory: DatabaseDriverFactory) {

    private val database = ClinicDatabase(
        driver = databaseDriverFactory.createDriver()
    )

    private val dbQueries = database.clinicDatabaseQueries

    fun getAllStaff(): List<StaffMember> {
        return dbQueries.selectAllStaff().executeAsList().map {
            StaffMember(
                id = it.id,
                fullName = it.fullName,
                role = UserRole.valueOf(it.role),
                department = it.department ?: ""
            )
        }
    }

    fun insertStaff(staff: StaffMember, pin: String? = null) {
        dbQueries.insertStaff(
            id = staff.id,
            fullName = staff.fullName,
            role = staff.role.name,
            department = staff.department,
            pin = pin,
            active =1,
            lastUpdated = Clock.System.now().toString()
        )
    }

    fun createEncounter(input: EncounterInput) {
        dbQueries.insertEncounter(
            encounter_id = input.encounterId,
            patient_id = input.patientId,
            encounter_datetime = input.encounterDatetime,
            department = input.department,
            visit_type = input.visitType,
            provider_id = input.providerId,
            facility_id = input.facilityId
        )
        queueSync("EncounterEntity", input.encounterId, "UPSERT", "{}")
    }

    fun getEncountersByPatient(patientId: String) =
        dbQueries.selectEncountersByPatient(patient_id = patientId).executeAsList()

    fun deleteEncounter(encounterId: String) {
        dbQueries.deleteEncounter(encounter_id = encounterId)
    }

    fun upsertVitalSigns(input: VitalSignsInput) {
        dbQueries.insertVitalSigns(
            vital_signs_id = input.vitalSignsId,
            encounter_id = input.encounterId,
            weight_kg = input.weightKg,
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

    fun getVitalSignsByEncounter(encounterId: String) =
        dbQueries.selectVitalSignsByEncounter(encounter_id = encounterId).executeAsList()

    fun upsertDiagnosis(input: DiagnosisInput) {
        dbQueries.insertDiagnosis(
            diagnosis_id = input.diagnosisId,
            encounter_id = input.encounterId,
            diagnosis_text = input.diagnosisText,
            is_primary = if (input.isPrimary) 1 else 0,
            code_system = input.codeSystem,
            diagnosis_code = input.diagnosisCode
        )
    }

    fun getDiagnosisByEncounter(encounterId: String) =
        dbQueries.selectDiagnosisByEncounter(encounter_id = encounterId).executeAsList()

    fun upsertMedicationOrder(input: MedicationOrderInput) {
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

    fun getMedicationOrdersByEncounter(encounterId: String) =
        dbQueries.selectMedicationOrdersByEncounter(encounter_id = encounterId).executeAsList()

    fun upsertEncounterOutcome(input: EncounterOutcomeInput) {
        dbQueries.upsertEncounterOutcome(
            outcome_id = input.outcomeId,
            encounter_id = input.encounterId,
            disposition = input.disposition,
            referral_to = input.referralTo,
            admitted = if (input.admitted) 1 else 0,
            discharge_notes = input.dischargeNotes
        )
    }

    fun getEncounterOutcome(encounterId: String) =
        dbQueries.selectEncounterOutcome(encounter_id = encounterId).executeAsOneOrNull()


    fun queueSync(entityType: String, entityId: String, operation: String, payload: String) {
        dbQueries.insertSyncItem(
            id = "SYNC-${'$'}{Clock.System.now().toEpochMilliseconds()}-${'$'}entityId",
            entityType = entityType,
            entityId = entityId,
            operation = operation,
            payload = payload,
            createdAt = Clock.System.now().toString()
        )
    }

    fun getPendingSync() = dbQueries.selectPendingSync().executeAsList()

    fun deleteSyncItem(id: String) {
        dbQueries.deleteSyncItem(id = id)
    }

    fun seedAdminIfEmpty() {
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
