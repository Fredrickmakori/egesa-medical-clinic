package com.egesa.clinic.shared.data

import com.egesa.clinic.shared.Disposition
import com.egesa.clinic.shared.VisitType
import com.egesa.clinic.shared.domain.ClinicalOrderType
import com.egesa.clinic.shared.domain.Encounter
import com.egesa.clinic.shared.domain.EncounterDiagnosis
import com.egesa.clinic.shared.domain.EncounterExam
import com.egesa.clinic.shared.domain.EncounterHistory
import com.egesa.clinic.shared.domain.EncounterPlan
import com.egesa.clinic.shared.domain.EncounterSourceType
import com.egesa.clinic.shared.domain.EncounterStatus
import com.egesa.clinic.shared.domain.ImagingOrder
import com.egesa.clinic.shared.domain.LabOrderItem
import com.egesa.clinic.shared.domain.LabOrderStatus
import com.egesa.clinic.shared.domain.LabOrder
import com.egesa.clinic.shared.domain.OpdEncounterBundle
import com.egesa.clinic.shared.domain.Prescription
import com.egesa.clinic.shared.db.ClinicDatabase
import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import kotlinx.datetime.Clock

interface EncounterRepository {
    suspend fun createEncounter(
        patientId: String,
        providerId: String?,
        facilityId: String,
        locationId: String? = null,
        sourceType: EncounterSourceType? = null,
        sourceId: String? = null,
        department: String = "OPD",
    ): Encounter

    suspend fun updateEncounter(bundle: OpdEncounterBundle, finalize: Boolean): OpdEncounterBundle
    suspend fun getEncounterById(encounterId: String): OpdEncounterBundle?
    suspend fun getEncountersForPatient(patientId: String): List<OpdEncounterBundle>
    suspend fun saveOrdersAndPrescriptions(
        encounterId: String,
        labOrders: List<LabOrder>,
        imagingOrders: List<ImagingOrder>,
        prescriptions: List<Prescription>,
    )
}

/**
 * OPD consultation persistence: local SQLDelight with sync queue integration.
 */
class LocalEncounterRepository(private val database: ClinicDatabase) : EncounterRepository {

    private val queries = database.clinicDatabaseQueries
    private val localRepository = LocalRepository(database)

    override suspend fun createEncounter(
        patientId: String,
        providerId: String?,
        facilityId: String,
        locationId: String? = null,
        sourceType: EncounterSourceType? = null,
        sourceId: String? = null,
        department: String = "OPD",
    ): Encounter {
        val now = Clock.System.now().toString()
        val encounterId = "ENC-${Clock.System.now().toEpochMilliseconds()}"
        val encounter = Encounter(
            encounterId = encounterId,
            localId = encounterId,
            patientId = patientId,
            providerId = providerId,
            facilityId = facilityId,
            locationId = locationId,
            encounterDatetime = now,
            department = department,
            visitType = VisitType.OUTPATIENT.code,
            sourceType = sourceType,
            sourceId = sourceId,
            status = EncounterStatus.DRAFT,
            version = 1,
            createdAt = now,
            updatedAt = now,
        )
        persistEncounterHeader(encounter, syncState = "LOCAL_ONLY")
        localRepository.queueSync("EncounterEntity", encounterId, "UPSERT", "{}")
        return encounter
    }

    override suspend fun updateEncounter(bundle: OpdEncounterBundle, finalize: Boolean): OpdEncounterBundle {
        val status = if (finalize) EncounterStatus.FINAL else EncounterStatus.DRAFT
        val updated = bundle.encounter.copy(
            status = status,
            version = bundle.encounter.version + 1,
            updatedAt = Clock.System.now().toString(),
            nursingNotes = bundle.encounter.nursingNotes,
        )
        persistEncounterHeader(updated, syncState = if (finalize) "PENDING_SYNC" else "LOCAL_ONLY")
        bundle.history?.let { persistHistory(it) }
        bundle.exam?.let { persistExamAndVitals(it, updated.encounterId) }
        replaceDiagnoses(updated.encounterId, bundle.diagnoses)
        bundle.plan?.let { persistPlan(it) }
        replaceClinicalOrders(updated.encounterId, bundle.labOrders, bundle.imagingOrders)
        replacePrescriptions(updated.encounterId, bundle.prescriptions)
        if (finalize) {
            persistOutcome(updated.encounterId, bundle)
            queueBillingAndRoutingEvents(updated, bundle)
        }
        localRepository.queueSync("EncounterEntity", updated.encounterId, "UPSERT", "{}")
        return loadEncounterBundle(updated.encounterId) ?: bundle.copy(encounter = updated)
    }

    override suspend fun getEncounterById(encounterId: String): OpdEncounterBundle? =
        loadEncounterBundle(encounterId)

    override suspend fun getEncountersForPatient(patientId: String): List<OpdEncounterBundle> =
        queries.selectEncountersByPatient(patient_id = patientId).awaitAsList()
            .mapNotNull { loadEncounterBundle(it.encounter_id) }

    override suspend fun saveOrdersAndPrescriptions(
        encounterId: String,
        labOrders: List<LabOrder>,
        imagingOrders: List<ImagingOrder>,
        prescriptions: List<Prescription>,
    ) {
        replaceClinicalOrders(encounterId, labOrders, imagingOrders)
        replacePrescriptions(encounterId, prescriptions)
        labOrders.forEach { localRepository.queueSync("ClinicalOrderEntity", it.id, "UPSERT", "{}") }
        imagingOrders.forEach { localRepository.queueSync("ClinicalOrderEntity", it.orderId, "UPSERT", "{}") }
        prescriptions.forEach {
            localRepository.queueSync("MedicationOrderEntity", it.prescriptionId, "UPSERT", "{}")
        }
    }

    private suspend fun persistEncounterHeader(encounter: Encounter, syncState: String) {
        queries.insertEncounter(
            encounter_id = encounter.encounterId,
            server_id = encounter.serverId,
            patient_id = encounter.patientId,
            encounter_datetime = encounter.encounterDatetime,
            department = encounter.department,
            visit_type = encounter.visitType,
            provider_id = encounter.providerId,
            facility_id = encounter.facilityId,
            location_id = encounter.locationId,
            source_type = encounter.sourceType?.name,
            source_id = encounter.sourceId,
            status = encounter.status.name,
            version = encounter.version.toLong(),
            created_at = encounter.createdAt,
            updated_at = encounter.updatedAt,
            deleted_at = encounter.deletedAt,
            nursing_notes = encounter.nursingNotes,
            sync_state = syncState,
        )
    }

    private suspend fun persistHistory(history: EncounterHistory) {
        queries.upsertEncounterHistory(
            encounter_id = history.encounterId,
            server_id = history.serverId,
            chief_complaint = history.chiefComplaint,
            hpi = history.hpi,
            pmh = history.pmh,
            medication_history = history.medicationHistory,
            allergies = history.allergies,
            family_history = history.familyHistory,
            social_history = history.socialHistory,
            version = history.version.toLong(),
            created_at = history.createdAt,
            updated_at = history.updatedAt,
            deleted_at = history.deletedAt,
        )
    }

    private suspend fun persistExamAndVitals(exam: EncounterExam, encounterId: String) {
        queries.upsertEncounterExam(
            encounter_id = encounterId,
            server_id = exam.serverId,
            system_exam_notes = exam.systemExamNotes,
            version = exam.version.toLong(),
            created_at = exam.createdAt,
            updated_at = exam.updatedAt,
            deleted_at = exam.deletedAt,
        )
        val hasVitals = listOf(
            exam.weightKg, exam.heightCm, exam.temperatureC,
            exam.systolicBp, exam.diastolicBp, exam.pulseBpm, exam.respiratoryRate, exam.spo2Percent,
        ).any { it != null }
        if (hasVitals) {
            val vitalId = exam.vitalSignsId ?: "VS-$encounterId"
            localRepository.upsertVitalSigns(
                VitalSignsInput(
                    vitalSignsId = vitalId,
                    encounterId = encounterId,
                    weightKg = exam.weightKg,
                    heightCm = exam.heightCm,
                    bmi = exam.bmi,
                    temperatureC = exam.temperatureC,
                    systolicBp = exam.systolicBp?.toLong(),
                    diastolicBp = exam.diastolicBp?.toLong(),
                    pulseBpm = exam.pulseBpm?.toLong(),
                    respiratoryRate = exam.respiratoryRate?.toLong(),
                    spo2Percent = exam.spo2Percent,
                    recordedAt = exam.recordedAt ?: Clock.System.now().toString(),
                )
            )
        }
    }

    private suspend fun persistPlan(plan: EncounterPlan) {
        queries.upsertEncounterPlan(
            encounter_id = plan.encounterId,
            server_id = plan.serverId,
            clinical_advice = plan.clinicalAdvice,
            follow_up_date = plan.followUpDate,
            version = plan.version.toLong(),
            created_at = plan.createdAt,
            updated_at = plan.updatedAt,
            deleted_at = plan.deletedAt,
        )
    }

    private suspend fun replaceDiagnoses(encounterId: String, diagnoses: List<EncounterDiagnosis>) {
        queries.deleteDiagnosisByEncounter(encounter_id = encounterId)
        diagnoses.forEach { dx ->
            localRepository.upsertDiagnosis(
                DiagnosisInput(
                    diagnosisId = dx.diagnosisId,
                    encounterId = encounterId,
                    diagnosisText = dx.diagnosisText,
                    isPrimary = dx.isPrimary,
                    codeSystem = dx.codeSystem,
                    diagnosisCode = dx.diagnosisCode,
                )
            )
        }
    }

    private suspend fun replaceClinicalOrders(
        encounterId: String,
        labOrders: List<LabOrder>,
        imagingOrders: List<ImagingOrder>,
    ) {
        queries.deleteClinicalOrdersByEncounter(encounter_id = encounterId)
        labOrders.forEach { order ->
            queries.insertClinicalOrder(
                order_id = order.id,
                server_id = order.serverId,
                encounter_id = encounterId,
                order_type = ClinicalOrderType.LAB.name,
                order_name = order.testName,
                instructions = order.clinicalNotes,
                status = order.status.name,
                sync_state = "LOCAL_ONLY",
                version = order.version.toLong(),
                created_at = order.createdAt,
                updated_at = order.updatedAt,
                deleted_at = order.deletedAt,
            )
        }
        imagingOrders.forEach { order ->
            queries.insertClinicalOrder(
                order_id = order.orderId,
                server_id = order.serverId,
                encounter_id = encounterId,
                order_type = ClinicalOrderType.IMAGING.name,
                order_name = order.studyName,
                instructions = order.instructions,
                status = order.status,
                sync_state = "LOCAL_ONLY",
                version = order.version.toLong(),
                created_at = order.createdAt,
                updated_at = order.updatedAt,
                deleted_at = order.deletedAt,
            )
        }
    }

    private suspend fun replacePrescriptions(encounterId: String, prescriptions: List<Prescription>) {
        queries.deleteMedicationOrdersByEncounter(encounter_id = encounterId)
        prescriptions.forEach { rx ->
            localRepository.upsertMedicationOrder(
                MedicationOrderInput(
                    medicationOrderId = rx.prescriptionId,
                    serverId = rx.serverId,
                    encounterId = encounterId,
                    medicationName = rx.medicationName,
                    dose = rx.dose,
                    route = rx.route,
                    frequency = rx.frequency,
                    duration = rx.duration,
                    instructions = rx.instructions,
                    version = rx.version.toLong(),
                    createdAt = rx.createdAt,
                    updatedAt = rx.updatedAt,
                    deletedAt = rx.deletedAt,
                )
            )
        }
    }

    private suspend fun persistOutcome(encounterId: String, bundle: OpdEncounterBundle) {
        val disposition = bundle.disposition?.let { raw ->
            runCatching { Disposition.valueOf(raw.uppercase()) }.getOrNull()
        } ?: Disposition.DISCHARGED
        localRepository.upsertEncounterOutcome(
            EncounterOutcomeInput(
                outcomeId = "OUT-$encounterId",
                encounterId = encounterId,
                disposition = disposition,
                referralTo = bundle.referralTo,
                admitted = disposition == Disposition.ADMITTED,
                dischargeNotes = bundle.dischargeNotes,
            )
        )
    }

    private suspend fun queueBillingAndRoutingEvents(encounter: Encounter, bundle: OpdEncounterBundle) {
        val now = Clock.System.now().toString()
        bundle.labOrders.forEach { order ->
            localRepository.upsertServiceEvent(
                ServiceEventInput(
                    serviceEventId = "LAB-${order.id}",
                    encounterId = encounter.encounterId,
                    program = "OPD",
                    indicatorCategory = "LAB_ORDER",
                    serviceCode = order.testName,
                    valueText = order.clinicalNotes,
                    eventDatetime = now,
                )
            )
        }
        bundle.imagingOrders.forEach { order ->
            localRepository.upsertServiceEvent(
                ServiceEventInput(
                    serviceEventId = "IMG-${order.orderId}",
                    encounterId = encounter.encounterId,
                    program = "OPD",
                    indicatorCategory = "IMAGING_ORDER",
                    serviceCode = order.studyName,
                    valueText = order.instructions,
                    eventDatetime = now,
                )
            )
        }
        bundle.prescriptions.forEach { rx ->
            localRepository.upsertServiceEvent(
                ServiceEventInput(
                    serviceEventId = "RX-${rx.prescriptionId}",
                    encounterId = encounter.encounterId,
                    program = "OPD",
                    indicatorCategory = "PHARMACY_ORDER",
                    serviceCode = rx.medicationName,
                    valueText = listOfNotNull(rx.dose, rx.frequency, rx.duration).joinToString(" "),
                    eventDatetime = now,
                )
            )
        }
        if (bundle.diagnoses.isNotEmpty() || bundle.plan != null) {
            localRepository.upsertServiceEvent(
                ServiceEventInput(
                    serviceEventId = "OPD-${encounter.encounterId}",
                    encounterId = encounter.encounterId,
                    program = "OPD",
                    indicatorCategory = "OPD_CONSULTATION",
                    serviceCode = bundle.diagnoses.firstOrNull()?.diagnosisCode ?: "CONSULT",
                    valueText = bundle.diagnoses.firstOrNull()?.diagnosisText,
                    eventDatetime = now,
                )
            )
        }
    }

    private suspend fun loadEncounterBundle(encounterId: String): OpdEncounterBundle? {
        val row = queries.selectEncounterById(encounter_id = encounterId).awaitAsOneOrNull() ?: return null
        val historyRow = queries.selectEncounterHistory(encounter_id = encounterId).awaitAsOneOrNull()
        val examRow = queries.selectEncounterExam(encounter_id = encounterId).awaitAsOneOrNull()
        val planRow = queries.selectEncounterPlan(encounter_id = encounterId).awaitAsOneOrNull()
        val vitals = queries.selectVitalSignsByEncounter(encounter_id = encounterId).awaitAsList().firstOrNull()
        val sourceType = row.source_type?.let { runCatching { EncounterSourceType.valueOf(it) }.getOrNull() }
        val status = runCatching { EncounterStatus.valueOf(row.status) }.getOrDefault(EncounterStatus.DRAFT)

        val encounter = Encounter(
            encounterId = row.encounter_id,
            localId = row.encounter_id,
            serverId = row.server_id,
            patientId = row.patient_id,
            providerId = row.provider_id,
            facilityId = row.facility_id,
            locationId = row.location_id,
            encounterDatetime = row.encounter_datetime,
            department = row.department,
            visitType = row.visit_type,
            sourceType = sourceType,
            sourceId = row.source_id,
            status = status,
            version = row.version.toInt(),
            createdAt = row.created_at,
            updatedAt = row.updated_at,
            deletedAt = row.deleted_at,
            nursingNotes = row.nursing_notes,
            syncState = row.sync_state,
        )

        val history = historyRow?.let {
            EncounterHistory(
                encounterId = it.encounter_id,
                localId = "HIST-${it.encounter_id}",
                serverId = it.server_id,
                chiefComplaint = it.chief_complaint,
                hpi = it.hpi,
                pmh = it.pmh,
                medicationHistory = it.medication_history,
                allergies = it.allergies,
                familyHistory = it.family_history,
                socialHistory = it.social_history,
                version = it.version.toInt(),
                createdAt = it.created_at,
                updatedAt = it.updated_at,
                deletedAt = it.deleted_at,
            )
        }

        val exam = EncounterExam(
            encounterId = encounterId,
            localId = "EXAM-$encounterId",
            serverId = examRow?.server_id,
            systemExamNotes = examRow?.system_exam_notes ?: "",
            vitalSignsId = vitals?.vital_signs_id,
            weightKg = vitals?.weight_kg,
            heightCm = vitals?.height_cm,
            bmi = vitals?.bmi,
            temperatureC = vitals?.temperature_c,
            systolicBp = vitals?.systolic_bp?.toInt(),
            diastolicBp = vitals?.diastolic_bp?.toInt(),
            pulseBpm = vitals?.pulse_bpm?.toInt(),
            respiratoryRate = vitals?.respiratory_rate?.toInt(),
            spo2Percent = vitals?.spo2_percent,
            recordedAt = vitals?.recorded_at,
            version = examRow?.version?.toInt() ?: 1,
            createdAt = examRow?.created_at ?: row.created_at,
            updatedAt = examRow?.updated_at ?: row.updated_at,
            deletedAt = examRow?.deleted_at,
        )

        val diagnoses = queries.selectDiagnosisByEncounter(encounter_id = encounterId).awaitAsList().map {
            EncounterDiagnosis(
                diagnosisId = it.diagnosis_id,
                localId = it.diagnosis_id,
                serverId = it.server_id,
                encounterId = it.encounter_id,
                diagnosisText = it.diagnosis_text,
                isPrimary = it.is_primary == 1L,
                codeSystem = it.code_system,
                diagnosisCode = it.diagnosis_code,
                version = it.version.toInt(),
                createdAt = it.created_at,
                updatedAt = it.updated_at,
                deletedAt = it.deleted_at,
            )
        }

        val plan = planRow?.let {
            EncounterPlan(
                encounterId = it.encounter_id,
                localId = "PLAN-${it.encounter_id}",
                serverId = it.server_id,
                clinicalAdvice = it.clinical_advice,
                followUpDate = it.follow_up_date,
                version = it.version.toInt(),
                createdAt = it.created_at,
                updatedAt = it.updated_at,
                deletedAt = it.deleted_at,
            )
        }

        val orders = queries.selectClinicalOrdersByEncounter(encounter_id = encounterId).awaitAsList()
        val labOrders = orders.filter { it.order_type == ClinicalOrderType.LAB.name }.map {
            val status = runCatching { LabOrderStatus.valueOf(it.status) }.getOrDefault(LabOrderStatus.ORDERED)
            LabOrder(
                id = it.order_id,
                localId = it.order_id,
                serverId = it.server_id,
                patientId = row.patient_id,
                encounterId = encounterId,
                orderedBy = row.provider_id ?: "system",
                department = row.department,
                status = status,
                clinicalNotes = it.instructions,
                items = listOf(
                    LabOrderItem(
                        id = "ITEM-${it.order_id}",
                        orderId = it.order_id,
                        testId = it.order_name,
                        testCode = it.order_name,
                        testName = it.order_name,
                        status = status,
                        billingCode = "LAB-${it.order_name}",
                        price = 0.0,
                        instructions = it.instructions,
                        orderedAt = row.encounter_datetime,
                        updatedAt = row.updated_at,
                    )
                ),
                createdAt = row.encounter_datetime,
                updatedAt = row.updated_at,
                version = it.version.toInt(),
                deletedAt = it.deleted_at,
            )
        }
        val imagingOrders = orders.filter { it.order_type == ClinicalOrderType.IMAGING.name }.map {
            ImagingOrder(
                orderId = it.order_id,
                localId = it.order_id,
                serverId = it.server_id,
                encounterId = encounterId,
                studyName = it.order_name,
                instructions = it.instructions,
                status = it.status,
                version = it.version.toInt(),
                createdAt = it.created_at,
                updatedAt = it.updated_at,
                deletedAt = it.deleted_at,
            )
        }

        val prescriptions = queries.selectMedicationOrdersByEncounter(encounter_id = encounterId).awaitAsList().map {
            Prescription(
                prescriptionId = it.medication_order_id,
                localId = it.medication_order_id,
                serverId = it.server_id,
                encounterId = it.encounter_id,
                medicationName = it.medication_name,
                dose = it.dose,
                frequency = it.frequency,
                duration = it.duration,
                route = it.route,
                instructions = it.instructions,
                version = it.version.toInt(),
                createdAt = it.created_at,
                updatedAt = it.updated_at,
                deletedAt = it.deleted_at,
            )
        }

        val outcome = queries.selectEncounterOutcome(encounter_id = encounterId).awaitAsOneOrNull()

        return OpdEncounterBundle(
            encounter = encounter,
            history = history,
            exam = exam,
            diagnoses = diagnoses,
            plan = plan,
            labOrders = labOrders,
            imagingOrders = imagingOrders,
            prescriptions = prescriptions,
            disposition = outcome?.disposition,
            referralTo = outcome?.referral_to,
            dischargeNotes = outcome?.discharge_notes,
        )
    }
}
