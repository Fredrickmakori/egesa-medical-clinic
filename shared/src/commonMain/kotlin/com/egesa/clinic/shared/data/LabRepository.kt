package com.egesa.clinic.shared.data

import com.egesa.clinic.shared.domain.LabOrder
import com.egesa.clinic.shared.domain.LabOrderStatus
import com.egesa.clinic.shared.domain.LabResult
import com.egesa.clinic.shared.domain.LabSample
import com.egesa.clinic.shared.db.ClinicDatabase
import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import kotlinx.datetime.Clock

data class LabWorklistQuery(
    val department: String,
    val status: LabOrderStatus? = null,
    val fromDateIso: String? = null,
    val toDateIso: String? = null,
)

interface LabRepository {
    suspend fun createLabOrder(order: LabOrder): LabOrder
    suspend fun getLabOrder(id: String): LabOrder?
    suspend fun getLabOrdersForPatient(patientId: String): List<LabOrder>
    suspend fun getWorklist(query: LabWorklistQuery): List<LabOrder>
    suspend fun updateOrderStatus(orderId: String, status: LabOrderStatus, actorId: String): LabOrder?
    suspend fun saveSample(sample: LabSample): LabSample
    suspend fun saveResults(orderId: String, results: List<LabResult>, actorId: String): List<LabResult>
}

class LocalLabRepository(private val database: ClinicDatabase) : LabRepository {
    private val queries = database.clinicDatabaseQueries
    private val localRepository = LocalRepository(database)

    override suspend fun createLabOrder(order: LabOrder): LabOrder {
        val now = Clock.System.now().toString()
        queries.insertLabOrder(
            id = order.id,
            patient_id = order.patientId,
            encounter_id = order.encounterId,
            ordered_by = order.orderedBy,
            department = order.department,
            status = order.status.name,
            priority = order.priority.name,
            diagnosis_hint = order.diagnosisHint,
            clinical_notes = order.clinicalNotes,
            sample_id = order.sampleId,
            billable_group_id = order.billableGroupId,
            verified_by = order.verifiedBy,
            verified_at = order.verifiedAt,
            reported_by = order.reportedBy,
            reported_at = order.reportedAt,
            created_at = order.createdAt,
            updated_at = order.updatedAt,
        )
        order.items.forEach { item ->
            queries.insertLabOrderItem(
                id = item.id,
                order_id = order.id,
                test_id = item.testId,
                test_code = item.testCode,
                test_name = item.testName,
                status = item.status.name,
                priority = item.priority.name,
                instructions = item.instructions,
                billing_code = item.billingCode,
                price = item.price,
                ordered_at = item.orderedAt,
                updated_at = item.updatedAt,
            )
        }
        // Hook: queue billable event for billing module reconciliation.
        localRepository.upsertServiceEvent(
            ServiceEventInput(
                serviceEventId = "LAB-BILL-${order.id}",
                encounterId = order.encounterId ?: "LAB-${order.id}",
                program = "LAB",
                indicatorCategory = "LAB_ORDER_BILLABLE",
                serviceCode = order.items.firstOrNull()?.billingCode ?: "LAB",
                valueText = "Lab order ${order.id} created",
                eventDatetime = now,
            )
        )
        localRepository.queueSync("LabOrderEntity", order.id, "UPSERT", "{}")
        return getLabOrder(order.id) ?: order
    }

    override suspend fun getLabOrder(id: String): LabOrder? {
        val row = queries.selectLabOrderById(id).awaitAsOneOrNull() ?: return null
        val items = queries.selectLabOrderItemsByOrder(id).awaitAsList().map {
            com.egesa.clinic.shared.domain.LabOrderItem(
                id = it.id,
                orderId = it.order_id,
                testId = it.test_id,
                testCode = it.test_code,
                testName = it.test_name,
                status = enumValueOfOrDefault(it.status, LabOrderStatus.ORDERED),
                priority = enumValueOfOrDefault(it.priority, com.egesa.clinic.shared.domain.LabPriority.ROUTINE),
                instructions = it.instructions,
                billingCode = it.billing_code,
                price = it.price,
                orderedAt = it.ordered_at,
                updatedAt = it.updated_at,
            )
        }
        return LabOrder(
            id = row.id,
            patientId = row.patient_id,
            encounterId = row.encounter_id,
            orderedBy = row.ordered_by,
            department = row.department,
            status = enumValueOfOrDefault(row.status, LabOrderStatus.ORDERED),
            priority = enumValueOfOrDefault(row.priority, com.egesa.clinic.shared.domain.LabPriority.ROUTINE),
            diagnosisHint = row.diagnosis_hint,
            clinicalNotes = row.clinical_notes,
            items = items,
            sampleId = row.sample_id,
            billableGroupId = row.billable_group_id,
            createdAt = row.created_at,
            updatedAt = row.updated_at,
            verifiedBy = row.verified_by,
            verifiedAt = row.verified_at,
            reportedBy = row.reported_by,
            reportedAt = row.reported_at,
        )
    }

    override suspend fun getLabOrdersForPatient(patientId: String): List<LabOrder> =
        queries.selectLabOrdersByPatient(patientId).awaitAsList().mapNotNull { getLabOrder(it.id) }

    override suspend fun getWorklist(query: LabWorklistQuery): List<LabOrder> {
        val rows = if (query.status == null) {
            queries.selectLabWorklistByDepartment(query.department).awaitAsList()
        } else {
            queries.selectLabWorklistByDepartmentAndStatus(query.department, query.status.name).awaitAsList()
        }
        return rows.mapNotNull { row ->
            if (query.fromDateIso != null && row.created_at < query.fromDateIso) return@mapNotNull null
            if (query.toDateIso != null && row.created_at > query.toDateIso) return@mapNotNull null
            getLabOrder(row.id)
        }
    }

    override suspend fun updateOrderStatus(orderId: String, status: LabOrderStatus, actorId: String): LabOrder? {
        val existing = queries.selectLabOrderById(orderId).awaitAsOneOrNull() ?: return null
        val currentStatus = enumValueOfOrDefault(existing.status, LabOrderStatus.ORDERED)
        if (!currentStatus.canTransitionTo(status) && currentStatus != status) return null
        val now = Clock.System.now().toString()
        queries.updateLabOrderStatus(
            status = status.name,
            verified_by = if (status == LabOrderStatus.VERIFIED) actorId else existing.verified_by,
            verified_at = if (status == LabOrderStatus.VERIFIED) now else existing.verified_at,
            reported_by = if (status == LabOrderStatus.REPORTED) actorId else existing.reported_by,
            reported_at = if (status == LabOrderStatus.REPORTED) now else existing.reported_at,
            updated_at = now,
            id = orderId,
        )
        localRepository.queueSync("LabOrderEntity", orderId, "UPDATE_STATUS", """{"status":"${status.name}"}""")
        return getLabOrder(orderId)
    }

    override suspend fun saveSample(sample: LabSample): LabSample {
        queries.insertLabSample(
            id = sample.id,
            order_id = sample.orderId,
            patient_id = sample.patientId,
            specimen_type = sample.specimenType,
            accession_number = sample.accessionNumber,
            collected_by = sample.collectedBy,
            collected_at = sample.collectedAt,
            received_by = sample.receivedBy,
            received_at = sample.receivedAt,
            rejected_reason = sample.rejectedReason,
            status = sample.status.name,
            created_at = sample.createdAt,
            updated_at = sample.updatedAt,
        )
        localRepository.queueSync("LabSampleEntity", sample.id, "UPSERT", "{}")
        return sample
    }

    override suspend fun saveResults(orderId: String, results: List<LabResult>, actorId: String): List<LabResult> {
        val now = Clock.System.now().toString()
        results.forEach { result ->
            queries.insertLabResult(
                id = result.id,
                order_id = orderId,
                order_item_id = result.orderItemId,
                patient_id = result.patientId,
                test_id = result.testId,
                test_code = result.testCode,
                test_name = result.testName,
                value = result.value,
                value_numeric = result.valueNumeric,
                unit = result.unit,
                reference_range = result.referenceRange,
                flag = result.flag?.name,
                comment = result.comment,
                entered_by = result.enteredBy,
                entered_at = result.enteredAt,
                verified_by = result.verifiedBy,
                verified_at = result.verifiedAt,
                reported_by = result.reportedBy,
                reported_at = result.reportedAt,
                created_at = result.createdAt,
                updated_at = result.updatedAt,
            )
            localRepository.queueSync("LabResultEntity", result.id, "UPSERT", "{}")
        }
        // Placeholder: async notifier can fan out result-ready events (SMS/app/webhook).
        localRepository.queueSync("LabNotification", orderId, "RESULTS_READY", """{"actorId":"$actorId","timestamp":"$now"}""")
        return queries.selectLabResultsByOrder(orderId).awaitAsList().map {
            LabResult(
                id = it.id,
                orderId = it.order_id,
                orderItemId = it.order_item_id,
                patientId = it.patient_id,
                testId = it.test_id,
                testCode = it.test_code,
                testName = it.test_name,
                value = it.value,
                valueNumeric = it.value_numeric,
                unit = it.unit,
                referenceRange = it.reference_range,
                flag = it.flag?.let { raw -> enumValueOfOrDefault(raw, com.egesa.clinic.shared.domain.LabResultFlag.NORMAL) },
                comment = it.comment,
                enteredBy = it.entered_by,
                enteredAt = it.entered_at,
                verifiedBy = it.verified_by,
                verifiedAt = it.verified_at,
                reportedBy = it.reported_by,
                reportedAt = it.reported_at,
                createdAt = it.created_at,
                updatedAt = it.updated_at,
            )
        }
    }
}

private inline fun <reified T : Enum<T>> enumValueOfOrDefault(value: String, default: T): T =
    runCatching { enumValueOf<T>(value) }.getOrDefault(default)
