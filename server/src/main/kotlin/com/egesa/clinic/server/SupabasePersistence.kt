package com.egesa.clinic.server

import com.egesa.clinic.shared.AuditEvent
import com.egesa.clinic.shared.Patient
import com.egesa.clinic.shared.Permission
import com.egesa.clinic.shared.QueueItem
import com.egesa.clinic.shared.Sex
import com.egesa.clinic.shared.StaffMember
import com.egesa.clinic.shared.UserRole
import com.egesa.clinic.shared.Schedule
import com.egesa.clinic.shared.Slot
import com.egesa.clinic.shared.Appointment
import com.egesa.clinic.shared.data.ClinicalSyncBatchDto
import com.egesa.clinic.shared.data.DiagnosisDto
import com.egesa.clinic.shared.data.EncounterDto
import com.egesa.clinic.shared.data.EncounterOutcomeDto
import com.egesa.clinic.shared.data.HtsRegisterDto
import com.egesa.clinic.shared.data.MedicationOrderDto
import com.egesa.clinic.shared.data.PatientDocumentDto
import com.egesa.clinic.shared.data.PatientDto
import com.egesa.clinic.shared.data.LabOrderDto
import com.egesa.clinic.shared.data.LabOrderItemDto
import com.egesa.clinic.shared.data.LabResultDto
import com.egesa.clinic.shared.data.SaveLabResultsRequestDto
import com.egesa.clinic.shared.data.ServiceEventDto
import com.egesa.clinic.shared.data.SyncResultItemDto
import com.egesa.clinic.shared.data.UpdateLabOrderStatusRequestDto
import com.egesa.clinic.shared.data.VitalSignsDto
import com.egesa.clinic.shared.domain.Encounter
import com.egesa.clinic.shared.domain.EncounterDiagnosis
import com.egesa.clinic.shared.domain.EncounterExam
import com.egesa.clinic.shared.domain.EncounterHistory
import com.egesa.clinic.shared.domain.EncounterPlan
import com.egesa.clinic.shared.domain.EncounterSourceType
import com.egesa.clinic.shared.domain.EncounterStatus
import com.egesa.clinic.shared.domain.ImagingOrder
import com.egesa.clinic.shared.domain.LabOrder
import com.egesa.clinic.shared.domain.LabOrderItem
import com.egesa.clinic.shared.domain.LabOrderStatus
import com.egesa.clinic.shared.domain.LabPriority
import com.egesa.clinic.shared.domain.OpdEncounterBundle
import com.egesa.clinic.shared.domain.Prescription
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.java.Java
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import kotlin.math.roundToInt

class SupabaseRestClient(
    private val baseUrl: String,
    private val serviceRoleKey: String,
    private val json: Json = Json { ignoreUnknownKeys = true; explicitNulls = false },
    private val httpClient: HttpClient = HttpClient(Java) {
        install(ContentNegotiation) { json(json) }
    }
) {
    suspend fun select(
        table: String,
        filters: Map<String, String> = emptyMap(),
        order: String? = null,
        limit: Int? = null
    ): JsonArray {
        val response = httpClient.get(tableUrl(table)) {
            supabaseHeaders()
            parameter("select", "*")
            filters.forEach { (key, value) -> parameter(key, value) }
            order?.let { parameter("order", it) }
            limit?.let { parameter("limit", it) }
        }
        response.ensureSuccess("select $table")
        return response.body()
    }

    suspend fun upsert(table: String, rows: JsonArray, onConflict: String): JsonArray {
        if (rows.isEmpty()) return JsonArray(emptyList())
        val response = httpClient.post(tableUrl(table)) {
            supabaseHeaders()
            contentType(ContentType.Application.Json)
            header("Prefer", "resolution=merge-duplicates,return=representation")
            parameter("on_conflict", onConflict)
            setBody(rows)
        }
        response.ensureSuccess("upsert $table")
        return response.body()
    }

    suspend fun insert(table: String, row: JsonObject): JsonArray {
        val response = httpClient.post(tableUrl(table)) {
            supabaseHeaders()
            contentType(ContentType.Application.Json)
            header("Prefer", "return=representation")
            setBody(row)
        }
        response.ensureSuccess("insert $table")
        return response.body()
    }

    suspend fun patch(table: String, values: JsonObject, filters: Map<String, String>): JsonArray {
        val response = httpClient.patch(tableUrl(table)) {
            supabaseHeaders()
            contentType(ContentType.Application.Json)
            header("Prefer", "return=representation")
            filters.forEach { (key, value) -> parameter(key, value) }
            setBody(values)
        }
        response.ensureSuccess("patch $table")
        return response.body()
    }

    suspend fun deleteRows(table: String, filters: Map<String, String>) {
        val response = httpClient.delete(tableUrl(table)) {
            supabaseHeaders()
            filters.forEach { (key, value) -> parameter(key, value) }
        }
        response.ensureSuccess("delete $table")
    }

    private fun tableUrl(table: String): String = "${baseUrl.trimEnd('/')}/rest/v1/$table"

    private fun io.ktor.client.request.HttpRequestBuilder.supabaseHeaders() {
        header("apikey", serviceRoleKey)
        header(HttpHeaders.Authorization, "Bearer $serviceRoleKey")
    }

    private suspend fun HttpResponse.ensureSuccess(operation: String) {
        if (status.value !in 200..299) {
            val body = runCatching { body<String>() }.getOrDefault("")
            throw SupabaseException("$operation failed with HTTP ${status.value}: $body")
        }
    }
}

class SupabaseException(message: String) : RuntimeException(message)

class SupabasePersistence(private val client: SupabaseRestClient) {

    suspend fun staffDirectory(facilityId: String = "default"): List<AuthUser> =
        client.select("staff_members", filters = mapOf("active" to "eq.true", "facility_id" to "eq.$facilityId"), order = "full_name.asc")
            .mapNotNull { it.jsonObject.toAuthUserOrNull() }

    suspend fun tenantIdByCode(code: String): String? =
        client.select(
            "hospitals",
            filters = mapOf("tenant_code" to "eq.$code"),
            limit = 1
        ).firstOrNull()?.jsonObject?.string("id")

    suspend fun verifyStaff(
        staffId: String,
        pin: String,
        facilityId: String? = null,
        tenantCode: String? = null
    ): AuthUser? {
        val filters = mutableMapOf(
            "id" to "eq.$staffId",
            "active" to "eq.true"
        )
        facilityId?.takeIf { it.isNotBlank() }?.let {
            filters["facility_id"] = "eq.$it"
        }
        val row = client.select(
            "staff_members",
            filters = filters,
            limit = 1
        ).firstOrNull()?.jsonObject ?: return null
        val storedPin = row.string("pin_hash") ?: return null
        return row.toAuthUserOrNull()
            ?.copy(tenantCode = tenantCode)
            ?.takeIf { storedPin == pin }
    }

    suspend fun allPatients(facilityId: String = "default"): List<Patient> =
        client.select("patients", filters = mapOf("facility_id" to "eq.$facilityId"), order = "full_name.asc").map { it.jsonObject.toPatient() }

    suspend fun patientById(id: String, facilityId: String = "default"): Patient? =
        client.select("patients", filters = mapOf("id" to "eq.$id", "facility_id" to "eq.$facilityId"), limit = 1)
            .firstOrNull()?.jsonObject?.toPatient()

    suspend fun upsertPatient(patient: Patient, triageLevel: Int? = null, facilityId: String = "default"): Patient {
        val saved = client.upsert(
            table = "patients",
            rows = JsonArray(listOf(patient.toSupabasePatientJson(triageLevel, facilityId))),
            onConflict = "id"
        ).firstOrNull()?.jsonObject
        return saved?.toPatient() ?: patient
    }

    suspend fun activeQueue(facilityId: String = "default"): List<QueueItem> =
        client.select(
            "queue_entries",
            filters = mapOf("status" to "neq.CHECKED_OUT", "facility_id" to "eq.$facilityId"),
            order = "checked_in_at.asc"
        ).map { it.jsonObject.toQueueItem() }

    suspend fun checkInPatient(patientId: String, name: String, triageLevel: Int, checkedInAt: String? = null, facilityId: String = "default"): QueueItem {
        val now = checkedInAt ?: Clock.System.now().toString()
        val item = QueueItem(patientId, name, triageLevel.coerceIn(1, 5), 0, "WAITING", now, null)
        client.upsert(
            "queue_entries",
            JsonArray(listOf(item.toSupabaseQueueJson(facilityId))),
            "patient_id"
        )
        return item
    }

    suspend fun checkOutPatient(patientId: String, facilityId: String = "default"): QueueItem? {
        val now = Clock.System.now().toString()
        val patched = client.patch(
            "queue_entries",
            buildJsonObject {
                put("status", "CHECKED_OUT")
                put("checked_out_at", now)
                put("updated_at", now)
            },
            filters = mapOf(
                "patient_id" to "eq.$patientId",
                "status" to "neq.CHECKED_OUT",
                "facility_id" to "eq.$facilityId"
            )
        )
        return patched.firstOrNull()?.jsonObject?.toQueueItem()
    }

    suspend fun auditTrail(limit: Int = 200): List<AuditEvent> =
        client.select("audit_events", order = "timestamp.desc", limit = limit)
            .map { it.jsonObject.toAuditEvent() }

    suspend fun logAudit(event: AuditEvent) {
        client.insert("audit_events", event.toSupabaseAuditJson())
    }

    suspend fun uploadPatients(changes: List<PatientDto>, facilityId: String = "default"): List<SyncResultItemDto> {
        if (changes.isEmpty()) return emptyList()
        return runBatch(changes, "Patient") { dto ->
            client.upsert("patients", JsonArray(listOf(dto.toSupabasePatientJson(facilityId))), "id")
        }
    }

    suspend fun uploadClinical(batch: ClinicalSyncBatchDto, facilityId: String = "default"): List<SyncResultItemDto> {
        val results = mutableListOf<SyncResultItemDto>()
        results += upsertClinical("encounters", "encounter_id", batch.encounters, EncounterDto::encounterId) { it.toSupabaseJson(facilityId) }
        results += upsertClinical("vital_signs", "vital_signs_id", batch.vitalSigns, VitalSignsDto::vitalSignsId) { it.toSupabaseJson() }
        results += upsertClinical("diagnosis", "diagnosis_id", batch.diagnoses, DiagnosisDto::diagnosisId) { it.toSupabaseJson() }
        results += upsertClinical("medication_order", "medication_order_id", batch.medicationOrders, MedicationOrderDto::medicationOrderId) { it.toSupabaseJson() }
        results += upsertClinical("encounter_outcome", "outcome_id", batch.encounterOutcomes, EncounterOutcomeDto::outcomeId) { it.toSupabaseJson() }
        results += upsertClinical("hts_register", "hts_id", batch.htsEntries, HtsRegisterDto::htsId) { it.toSupabaseJson() }
        results += upsertClinical("service_events", "service_event_id", batch.serviceEvents, ServiceEventDto::serviceEventId) { it.toSupabaseJson() }
        results += upsertClinical("patient_documents", "document_id", batch.patientDocuments, PatientDocumentDto::documentId) { it.toSupabaseJson() }
        return results
    }

    suspend fun upsertEncounterBundle(bundle: OpdEncounterBundle, facilityId: String = "default"): OpdEncounterBundle {
        val encounter = bundle.encounter
        client.upsert("encounters", JsonArray(listOf(encounter.toSupabaseJson(facilityId))), "encounter_id")
        bundle.history?.let { client.upsert("encounter_history", JsonArray(listOf(it.toSupabaseJson())), "encounter_id") }
        bundle.exam?.let { exam ->
            client.upsert("encounter_exam", JsonArray(listOf(exam.toExamSupabaseJson())), "encounter_id")
            if (!exam.vitalSignsId.isNullOrBlank()) {
                client.upsert("vital_signs", JsonArray(listOf(exam.toVitalSignsSupabaseJson(encounter.encounterDatetime))), "vital_signs_id")
            }
        }
        upsertRows("diagnosis", "diagnosis_id", bundle.diagnoses.map { it.toSupabaseJson() })
        bundle.plan?.let { client.upsert("encounter_plan", JsonArray(listOf(it.toSupabaseJson())), "encounter_id") }
        bundle.labOrders.forEach { upsertLabOrder(it.toDtoLike(), facilityId) }
        upsertRows("clinical_orders", "order_id", bundle.imagingOrders.map { it.toSupabaseJson() })
        upsertRows("medication_order", "medication_order_id", bundle.prescriptions.map { it.toSupabaseJson() })
        bundle.disposition?.takeIf { it.isNotBlank() }?.let {
            client.upsert("encounter_outcome", JsonArray(listOf(bundle.toOutcomeSupabaseJson())), "outcome_id")
        }
        return encounterBundleById(encounter.encounterId, facilityId) ?: bundle
    }

    suspend fun encounterBundleById(id: String, facilityId: String = "default"): OpdEncounterBundle? {
        val row = client.select(
            "encounters",
            filters = mapOf("encounter_id" to "eq.$id", "facility_id" to "eq.$facilityId"),
            limit = 1
        )
            .firstOrNull()?.jsonObject ?: return null
        return hydrateEncounterBundle(row)
    }

    suspend fun encounterBundlesForPatient(patientId: String, facilityId: String = "default"): List<OpdEncounterBundle> =
        client.select(
            "encounters",
            filters = mapOf("patient_id" to "eq.$patientId", "facility_id" to "eq.$facilityId"),
            order = "encounter_datetime.desc"
        ).mapNotNullSuspend { hydrateEncounterBundle(it.jsonObject) }

    suspend fun upsertLabOrder(order: LabOrderDto, facilityId: String = "default"): LabOrderDto {
        client.upsert("lab_orders", JsonArray(listOf(order.toSupabaseJson(facilityId))), "id")
        upsertRows("lab_order_items", "id", order.items.map { it.toSupabaseJson() })
        return labOrderById(order.id, facilityId) ?: order
    }

    suspend fun labOrderById(id: String, facilityId: String = "default"): LabOrderDto? {
        val row = client.select(
            "lab_orders",
            filters = mapOf("id" to "eq.$id", "facility_id" to "eq.$facilityId"),
            limit = 1
        )
            .firstOrNull()?.jsonObject ?: return null
        return row.toLabOrderDto(labOrderItems(id))
    }

    suspend fun labOrdersForPatient(patientId: String, facilityId: String = "default"): List<LabOrderDto> =
        client.select(
            "lab_orders",
            filters = mapOf("patient_id" to "eq.$patientId", "facility_id" to "eq.$facilityId"),
            order = "created_at.desc"
        )
            .mapSuspend { row -> row.jsonObject.toLabOrderDto(labOrderItems(row.jsonObject.string("id").orEmpty())) }

    suspend fun labWorklist(
        department: String,
        status: String?,
        fromDate: String?,
        toDate: String?,
        facilityId: String = "default"
    ): List<LabOrderDto> {
        val filters = buildMap {
            put("department", "eq.$department")
            if (!status.isNullOrBlank()) put("status", "eq.$status")
            put("facility_id", "eq.$facilityId")
        }
        return client.select("lab_orders", filters = filters, order = "priority.asc,created_at.asc")
            .mapSuspend { it.jsonObject.toLabOrderDto(labOrderItems(it.jsonObject.string("id").orEmpty())) }
            .filter { fromDate.isNullOrBlank() || it.createdAt >= fromDate }
            .filter { toDate.isNullOrBlank() || it.createdAt <= toDate }
    }

    suspend fun saveLabResults(request: SaveLabResultsRequestDto, facilityId: String = "default"): List<LabResultDto> {
        upsertRows("lab_results", "id", request.results.map { it.toSupabaseJson() })
        val now = Clock.System.now().toString()
        client.patch(
            "lab_orders",
            buildJsonObject {
                put("status", LabOrderStatus.VERIFIED.name)
                put("verified_by", request.actorId)
                put("verified_at", now)
                put("updated_at", now)
            },
            filters = mapOf("id" to "eq.${request.orderId}", "facility_id" to "eq.$facilityId")
        )
        return labResultsForOrder(request.orderId)
    }

    suspend fun updateLabOrderStatus(
        orderId: String,
        request: UpdateLabOrderStatusRequestDto,
        facilityId: String = "default"
    ): LabOrderDto? {
        val now = Clock.System.now().toString()
        val values = buildJsonObject {
            put("status", request.status)
            put("updated_at", now)
            if (request.status == LabOrderStatus.VERIFIED.name) {
                put("verified_by", request.actorId)
                put("verified_at", now)
            }
            if (request.status == LabOrderStatus.REPORTED.name) {
                put("reported_by", request.actorId)
                put("reported_at", now)
            }
        }
        client.patch(
            "lab_orders",
            values,
            filters = mapOf("id" to "eq.$orderId", "facility_id" to "eq.$facilityId")
        )
        return labOrderById(orderId, facilityId)
    }

    suspend fun allSchedules(facilityId: String = "default"): List<Schedule> =
        client.select("schedules", filters = mapOf("facility_id" to "eq.$facilityId"), order = "name.asc").map { it.jsonObject.toSchedule() }

    suspend fun upsertSchedule(schedule: Schedule, facilityId: String = "default"): Schedule {
        val saved = client.upsert("schedules", JsonArray(listOf(schedule.toSupabaseJson(facilityId))), "id").firstOrNull()?.jsonObject
        return saved?.toSchedule() ?: schedule
    }

    suspend fun slotsForSchedule(scheduleId: String): List<Slot> =
        client.select("slots", filters = mapOf("schedule_id" to "eq.$scheduleId"), order = "start_time.asc")
            .map { it.jsonObject.toSlot() }

    suspend fun upsertSlot(slot: Slot): Slot {
        val saved = client.upsert("slots", JsonArray(listOf(slot.toSupabaseJson())), "id").firstOrNull()?.jsonObject
        return saved?.toSlot() ?: slot
    }

    suspend fun allAppointments(facilityId: String = "default"): List<Appointment> =
        client.select("appointments", filters = mapOf("facility_id" to "eq.$facilityId"), order = "start_time.asc").map { it.jsonObject.toAppointment() }

    suspend fun selectAppointmentsBySchedule(scheduleId: String): List<Appointment> =
        client.select("appointments", filters = mapOf("schedule_id" to "eq.$scheduleId", "status" to "eq.booked"))
            .map { it.jsonObject.toAppointment() }

    suspend fun checkOverlappingAppointments(scheduleId: String, startTime: String, endTime: String): Int {
        val booked = client.select("appointments", filters = mapOf("schedule_id" to "eq.$scheduleId", "status" to "eq.booked"))
            .map { it.jsonObject.toAppointment() }
        return booked.count { app ->
            app.startTime < endTime && app.endTime > startTime
        }
    }

    suspend fun upsertAppointment(appointment: Appointment, facilityId: String = "default"): Appointment {
        val saved = client.upsert("appointments", JsonArray(listOf(appointment.toSupabaseJson(facilityId))), "id").firstOrNull()?.jsonObject
        return saved?.toAppointment() ?: appointment
    }

    // --- Tenant Operations ---
    suspend fun registerTenant(id: String, name: String, code: String, email: String, plan: String, status: String): TenantHospital {
        val row = buildJsonObject {
            put("id", id)
            put("name", name)
            put("tenant_code", code)
            put("contact_email", email)
            put("billing_plan", plan)
            put("billing_status", status)
            put("amount_billed", if (plan == "premium") 49.00 else if (plan == "enterprise") 150.00 else 0.00)
        }
        val saved = client.insert("hospitals", row).first().jsonObject
        return saved.toTenantHospital()
    }

    suspend fun seedTenantAdmin(tenantId: String, name: String, username: String, pin: String) {
        val row = buildJsonObject {
            put("id", username)
            put("full_name", name)
            put("role", UserRole.ADMIN.name)
            put("department", "Administration")
            put("pin_hash", pin)
            put("facility_id", tenantId)
            put("active", true)
        }
        client.insert("staff_members", row)
    }

    suspend fun createTenantStaff(facilityId: String, name: String, role: String, pin: String, dept: String): AuthUser {
        val nextIdNum = (100..999).random()
        val rolePrefix = when (role) {
            "ADMIN" -> "AD"
            "DOCTOR" -> "DR"
            "NURSE" -> "NR"
            "PHARMACIST" -> "PH"
            else -> "RC"
        }
        val staffId = "$rolePrefix-$nextIdNum"
        val row = buildJsonObject {
            put("id", staffId)
            put("full_name", name)
            put("role", role)
            put("department", dept)
            put("pin_hash", pin)
            put("facility_id", facilityId)
            put("active", true)
        }
        val saved = client.insert("staff_members", row).first().jsonObject
        return saved.toAuthUserOrNull()!!
    }

    suspend fun allTenants(): List<TenantHospital> =
        client.select("hospitals", order = "name.asc").map { it.jsonObject.toTenantHospital() }

    suspend fun tenantMetrics(tenantId: String): TenantMetrics {
        val patients = client.select("patients", filters = mapOf("facility_id" to "eq.$tenantId"))
        val staff = client.select("staff_members", filters = mapOf("facility_id" to "eq.$tenantId"))
        val activeQueue = client.select("queue_entries", filters = mapOf("facility_id" to "eq.$tenantId", "status" to "neq.CHECKED_OUT"))
        return TenantMetrics(patients.size, staff.size, activeQueue.size)
    }

    suspend fun updateTenantBilling(tenantId: String, plan: String, status: String, amount: Double): TenantHospital {
        val patched = client.patch(
            "hospitals",
            buildJsonObject {
                put("billing_plan", plan)
                put("billing_status", status)
                put("amount_billed", amount)
                put("updated_at", Clock.System.now().toString())
            },
            filters = mapOf("id" to "eq.$tenantId")
        )
        return patched.first().jsonObject.toTenantHospital()
    }

    private suspend fun upsertRows(table: String, conflict: String, rows: List<JsonObject>) {
        if (rows.isNotEmpty()) client.upsert(table, JsonArray(rows), conflict)
    }

    private suspend fun hydrateEncounterBundle(row: JsonObject): OpdEncounterBundle? {
        val encounter = row.toEncounter()
        val encounterId = encounter.encounterId.ifBlank { return null }
        val history = client.select("encounter_history", filters = mapOf("encounter_id" to "eq.$encounterId"), limit = 1)
            .firstOrNull()?.jsonObject?.toEncounterHistory()
        val examRow = client.select("encounter_exam", filters = mapOf("encounter_id" to "eq.$encounterId"), limit = 1)
            .firstOrNull()?.jsonObject
        val vitalsRow = client.select("vital_signs", filters = mapOf("encounter_id" to "eq.$encounterId"), order = "recorded_at.desc", limit = 1)
            .firstOrNull()?.jsonObject
        val plan = client.select("encounter_plan", filters = mapOf("encounter_id" to "eq.$encounterId"), limit = 1)
            .firstOrNull()?.jsonObject?.toEncounterPlan()
        val diagnoses = client.select("diagnosis", filters = mapOf("encounter_id" to "eq.$encounterId"))
            .map { it.jsonObject.toEncounterDiagnosis() }
        val imaging = client.select("clinical_orders", filters = mapOf("encounter_id" to "eq.$encounterId", "order_type" to "eq.IMAGING"))
            .map { it.jsonObject.toImagingOrder() }
        val prescriptions = client.select("medication_order", filters = mapOf("encounter_id" to "eq.$encounterId"))
            .map { it.jsonObject.toPrescription() }
        val labOrders = client.select("lab_orders", filters = mapOf("encounter_id" to "eq.$encounterId"), order = "created_at.asc")
            .mapSuspend { it.jsonObject.toLabOrderDto(labOrderItems(it.jsonObject.string("id").orEmpty())).toDomainLike() }
        val outcome = client.select("encounter_outcome", filters = mapOf("encounter_id" to "eq.$encounterId"), limit = 1)
            .firstOrNull()?.jsonObject
        return OpdEncounterBundle(
            encounter = encounter,
            history = history,
            exam = (examRow ?: vitalsRow)?.toEncounterExam(vitalsRow),
            diagnoses = diagnoses,
            plan = plan,
            labOrders = labOrders,
            imagingOrders = imaging,
            prescriptions = prescriptions,
            disposition = outcome?.string("disposition"),
            referralTo = outcome?.string("referral_to"),
            dischargeNotes = outcome?.string("discharge_notes")
        )
    }

    private suspend fun labOrderItems(orderId: String): List<LabOrderItemDto> =
        client.select("lab_order_items", filters = mapOf("order_id" to "eq.$orderId"), order = "test_name.asc")
            .map { it.jsonObject.toLabOrderItemDto() }

    private suspend fun labResultsForOrder(orderId: String): List<LabResultDto> =
        client.select("lab_results", filters = mapOf("order_id" to "eq.$orderId"), order = "created_at.asc")
            .map { it.jsonObject.toLabResultDto() }

    private suspend fun <T> runBatch(items: List<T>, entityType: String, block: suspend (T) -> Unit): List<SyncResultItemDto> =
        items.map { item ->
            val id = when (item) {
                is PatientDto -> item.id
                else -> entityType
            }
            runCatching {
                block(item)
                SyncResultItemDto(id = id, status = "synced", version = 1)
            }.getOrElse {
                SyncResultItemDto(id = id, status = "failed:${it.message ?: "unknown"}", version = 0)
            }
        }

    private suspend fun <T> upsertClinical(
        table: String,
        conflict: String,
        items: List<T>,
        idOf: (T) -> String,
        toJson: (T) -> JsonObject
    ): List<SyncResultItemDto> =
        items.map { item ->
            val id = idOf(item)
            runCatching {
                client.upsert(table, JsonArray(listOf(toJson(item))), conflict)
                SyncResultItemDto(id = id, status = "synced", version = 1)
            }.getOrElse {
                SyncResultItemDto(id = id, status = "failed:${it.message ?: "unknown"}", version = 0)
            }
        }

    companion object {
        fun fromEnvironment(): SupabasePersistence? {
            val url = System.getenv("SUPABASE_URL")?.takeIf { it.isNotBlank() } ?: return null
            val key = System.getenv("SUPABASE_SERVICE_ROLE_KEY")?.takeIf { it.isNotBlank() } ?: return null
            return SupabasePersistence(SupabaseRestClient(url, key))
        }
    }
}

private fun JsonObject.toAuthUserOrNull(): AuthUser? {
    val id = string("id") ?: return null
    val name = string("full_name") ?: return null
    val role = runCatching { UserRole.valueOf(string("role") ?: "") }.getOrNull() ?: return null
    val pin = string("pin_hash") ?: ""
    val facilityId = string("facility_id") ?: "default"
    return AuthUser(id, name, role, pin, facilityId, tenantCode = null)
}

private fun JsonObject.toPatient(): Patient = Patient(
    id = string("id").orEmpty(),
    fullName = string("full_name").orEmpty(),
    age = int("age") ?: 0,
    sex = sex("sex"),
    status = string("status").orEmpty(),
    assignedWard = string("assigned_ward"),
    roomBed = string("room_bed"),
    acuity = string("acuity") ?: "Moderate",
    isolation = string("isolation"),
    visits = int("visits") ?: 0,
    activeDiagnosis = string("diagnosis") ?: "",
)

private fun Patient.toSupabasePatientJson(triageLevel: Int? = null, facilityId: String = "default"): JsonObject = buildJsonObject {
    put("id", id)
    put("full_name", fullName)
    put("age", age)
    put("sex", sex.code)
    put("status", status)
    putNullable("assigned_ward", assignedWard)
    putNullable("room_bed", roomBed)
    put("acuity", acuity)
    putNullable("isolation", isolation)
    triageLevel?.let { put("triage_level", it) }
    put("diagnosis", activeDiagnosis)
    put("facility_id", facilityId)
    put("updated_at", Clock.System.now().toString())
}

private fun PatientDto.toSupabasePatientJson(facilityId: String = "default"): JsonObject = buildJsonObject {
    put("id", id)
    put("full_name", fullName)
    put("age", age)
    put("sex", sex.code)
    put("status", status)
    putNullable("assigned_ward", assignedWard)
    putNullable("room_bed", roomBed)
    put("acuity", acuity)
    putNullable("isolation", isolation)
    put("diagnosis", activeDiagnosis)
    put("facility_id", facilityId)
    put("updated_at", Clock.System.now().toString())
}

private fun JsonObject.toQueueItem(): QueueItem {
    val checkedIn = string("checked_in_at")
    return QueueItem(
        patientId = string("patient_id").orEmpty(),
        name = string("name").orEmpty(),
        triageLevel = int("triage_level") ?: 3,
        waitMinutes = waitMinutesSince(checkedIn),
        status = string("status") ?: "WAITING",
        checkedInAt = checkedIn,
        checkedOutAt = string("checked_out_at")
    )
}

private fun QueueItem.toSupabaseQueueJson(facilityId: String = "default"): JsonObject = buildJsonObject {
    put("patient_id", patientId)
    put("name", name)
    put("triage_level", triageLevel)
    put("status", status)
    putNullable("checked_in_at", checkedInAt)
    putNullable("checked_out_at", checkedOutAt)
    put("facility_id", facilityId)
    put("updated_at", Clock.System.now().toString())
}

private fun AuditEvent.toSupabaseAuditJson(): JsonObject = buildJsonObject {
    put("user_name", user)
    put("user_id", userId)
    put("action", action)
    put("module", module)
    put("timestamp", timestamp.ifBlank { Clock.System.now().toString() })
    put("context_reference", contextReference)
    putNullable("permission", permission?.name)
    put("granted", granted)
}

private fun JsonObject.toAuditEvent(): AuditEvent = AuditEvent(
    user = string("user_name").orEmpty(),
    userId = string("user_id").orEmpty(),
    action = string("action").orEmpty(),
    module = string("module").orEmpty(),
    timestamp = string("timestamp").orEmpty(),
    contextReference = string("context_reference").orEmpty(),
    permission = string("permission")?.let { runCatching { Permission.valueOf(it) }.getOrNull() },
    granted = boolean("granted") ?: true
)

private fun EncounterDto.toSupabaseJson(facilityId: String = this.facilityId): JsonObject = buildJsonObject {
    put("encounter_id", encounterId)
    put("patient_id", patientId)
    put("encounter_datetime", encounterDatetime)
    put("department", department)
    put("visit_type", visitType)
    putNullable("provider_id", providerId)
    put("facility_id", facilityId)
    put("updated_at", Clock.System.now().toString())
}

private fun Encounter.toSupabaseJson(facilityId: String = this.facilityId): JsonObject = buildJsonObject {
    put("encounter_id", encounterId)
    putNullable("server_id", serverId)
    put("patient_id", patientId)
    put("encounter_datetime", encounterDatetime)
    put("department", department)
    put("visit_type", visitType)
    putNullable("provider_id", providerId)
    put("facility_id", facilityId)
    putNullable("location_id", locationId)
    putNullable("source_type", sourceType?.name)
    putNullable("source_id", sourceId)
    put("status", status.name)
    put("version", version)
    put("created_at", createdAt)
    put("updated_at", updatedAt)
    putNullable("deleted_at", deletedAt)
    putNullable("nursing_notes", nursingNotes)
    put("sync_state", syncState)
}

private fun JsonObject.toEncounter(): Encounter = Encounter(
    encounterId = string("encounter_id").orEmpty(),
    serverId = string("server_id"),
    patientId = string("patient_id").orEmpty(),
    providerId = string("provider_id"),
    facilityId = string("facility_id") ?: "default",
    locationId = string("location_id"),
    encounterDatetime = string("encounter_datetime").orEmpty(),
    department = string("department") ?: "OPD",
    visitType = string("visit_type") ?: "outpatient",
    sourceType = string("source_type")?.let { enumValueOrNull<EncounterSourceType>(it) },
    sourceId = string("source_id"),
    status = string("status")?.let { enumValueOrNull<EncounterStatus>(it) } ?: EncounterStatus.DRAFT,
    version = int("version") ?: 1,
    createdAt = string("created_at") ?: Clock.System.now().toString(),
    updatedAt = string("updated_at") ?: Clock.System.now().toString(),
    deletedAt = string("deleted_at"),
    nursingNotes = string("nursing_notes"),
    syncState = string("sync_state") ?: "LOCAL_ONLY"
)

private fun VitalSignsDto.toSupabaseJson(): JsonObject = buildJsonObject {
    put("vital_signs_id", vitalSignsId)
    put("encounter_id", encounterId)
    putNullable("weight_kg", weightKg)
    putNullable("height_cm", heightCm)
    putNullable("bmi", bmi)
    putNullable("temperature_c", temperatureC)
    putNullable("systolic_bp", systolicBp)
    putNullable("diastolic_bp", diastolicBp)
    putNullable("pulse_bpm", pulseBpm)
    putNullable("respiratory_rate", respiratoryRate)
    putNullable("spo2_percent", spo2Percent)
    putNullable("muac_cm", muacCm)
    put("recorded_at", recordedAt)
}

private fun EncounterHistory.toSupabaseJson(): JsonObject = buildJsonObject {
    put("encounter_id", encounterId)
    putNullable("server_id", serverId)
    put("chief_complaint", chiefComplaint)
    put("hpi", hpi)
    put("pmh", pmh)
    put("medication_history", medicationHistory)
    put("allergies", allergies)
    put("family_history", familyHistory)
    put("social_history", socialHistory)
    put("version", version)
    put("created_at", createdAt)
    put("updated_at", updatedAt)
    putNullable("deleted_at", deletedAt)
}

private fun JsonObject.toEncounterHistory(): EncounterHistory = EncounterHistory(
    encounterId = string("encounter_id").orEmpty(),
    serverId = string("server_id"),
    chiefComplaint = string("chief_complaint").orEmpty(),
    hpi = string("hpi").orEmpty(),
    pmh = string("pmh").orEmpty(),
    medicationHistory = string("medication_history").orEmpty(),
    allergies = string("allergies").orEmpty(),
    familyHistory = string("family_history").orEmpty(),
    socialHistory = string("social_history").orEmpty(),
    version = int("version") ?: 1,
    createdAt = string("created_at") ?: Clock.System.now().toString(),
    updatedAt = string("updated_at") ?: Clock.System.now().toString(),
    deletedAt = string("deleted_at")
)

private fun EncounterExam.toExamSupabaseJson(): JsonObject = buildJsonObject {
    put("encounter_id", encounterId)
    putNullable("server_id", serverId)
    put("system_exam_notes", systemExamNotes)
    put("version", version)
    put("created_at", createdAt)
    put("updated_at", updatedAt)
    putNullable("deleted_at", deletedAt)
}

private fun EncounterExam.toVitalSignsSupabaseJson(fallbackRecordedAt: String): JsonObject = buildJsonObject {
    put("vital_signs_id", vitalSignsId ?: "VITAL-$encounterId")
    put("encounter_id", encounterId)
    putNullable("weight_kg", weightKg)
    putNullable("height_cm", heightCm)
    putNullable("bmi", bmi)
    putNullable("temperature_c", temperatureC)
    putNullable("systolic_bp", systolicBp)
    putNullable("diastolic_bp", diastolicBp)
    putNullable("pulse_bpm", pulseBpm)
    putNullable("respiratory_rate", respiratoryRate)
    putNullable("spo2_percent", spo2Percent)
    putNullable("muac_cm", null as Double?)
    put("recorded_at", recordedAt ?: fallbackRecordedAt)
}

private fun JsonObject.toEncounterExam(vitals: JsonObject?): EncounterExam = EncounterExam(
    encounterId = string("encounter_id") ?: vitals?.string("encounter_id").orEmpty(),
    serverId = string("server_id"),
    systemExamNotes = string("system_exam_notes").orEmpty(),
    vitalSignsId = vitals?.string("vital_signs_id"),
    weightKg = vitals?.double("weight_kg"),
    heightCm = vitals?.double("height_cm"),
    bmi = vitals?.double("bmi"),
    temperatureC = vitals?.double("temperature_c"),
    systolicBp = vitals?.int("systolic_bp"),
    diastolicBp = vitals?.int("diastolic_bp"),
    pulseBpm = vitals?.int("pulse_bpm"),
    respiratoryRate = vitals?.int("respiratory_rate"),
    spo2Percent = vitals?.double("spo2_percent"),
    recordedAt = vitals?.string("recorded_at"),
    version = int("version") ?: 1,
    createdAt = string("created_at") ?: Clock.System.now().toString(),
    updatedAt = string("updated_at") ?: Clock.System.now().toString(),
    deletedAt = string("deleted_at")
)

private fun DiagnosisDto.toSupabaseJson(): JsonObject = buildJsonObject {
    put("diagnosis_id", diagnosisId)
    put("encounter_id", encounterId)
    put("diagnosis_text", diagnosisText)
    put("is_primary", isPrimary)
    putNullable("code_system", codeSystem)
    putNullable("diagnosis_code", diagnosisCode)
}

private fun EncounterDiagnosis.toSupabaseJson(): JsonObject = buildJsonObject {
    put("diagnosis_id", diagnosisId)
    putNullable("server_id", serverId)
    put("encounter_id", encounterId)
    put("diagnosis_text", diagnosisText)
    put("is_primary", isPrimary)
    putNullable("code_system", codeSystem)
    putNullable("diagnosis_code", diagnosisCode)
    put("version", version)
    put("created_at", createdAt)
    put("updated_at", updatedAt)
    putNullable("deleted_at", deletedAt)
}

private fun JsonObject.toEncounterDiagnosis(): EncounterDiagnosis = EncounterDiagnosis(
    diagnosisId = string("diagnosis_id").orEmpty(),
    serverId = string("server_id"),
    encounterId = string("encounter_id").orEmpty(),
    diagnosisText = string("diagnosis_text").orEmpty(),
    isPrimary = boolean("is_primary") ?: false,
    codeSystem = string("code_system"),
    diagnosisCode = string("diagnosis_code"),
    version = int("version") ?: 1,
    createdAt = string("created_at") ?: Clock.System.now().toString(),
    updatedAt = string("updated_at") ?: Clock.System.now().toString(),
    deletedAt = string("deleted_at")
)

private fun EncounterPlan.toSupabaseJson(): JsonObject = buildJsonObject {
    put("encounter_id", encounterId)
    putNullable("server_id", serverId)
    put("clinical_advice", clinicalAdvice)
    putNullable("follow_up_date", followUpDate)
    put("version", version)
    put("created_at", createdAt)
    put("updated_at", updatedAt)
    putNullable("deleted_at", deletedAt)
}

private fun JsonObject.toEncounterPlan(): EncounterPlan = EncounterPlan(
    encounterId = string("encounter_id").orEmpty(),
    serverId = string("server_id"),
    clinicalAdvice = string("clinical_advice").orEmpty(),
    followUpDate = string("follow_up_date"),
    version = int("version") ?: 1,
    createdAt = string("created_at") ?: Clock.System.now().toString(),
    updatedAt = string("updated_at") ?: Clock.System.now().toString(),
    deletedAt = string("deleted_at")
)

private fun MedicationOrderDto.toSupabaseJson(): JsonObject = buildJsonObject {
    put("medication_order_id", medicationOrderId)
    put("encounter_id", encounterId)
    put("medication_name", medicationName)
    putNullable("dose", dose)
    putNullable("route", route)
    putNullable("frequency", frequency)
    putNullable("duration", duration)
    putNullable("instructions", instructions)
}

private fun Prescription.toSupabaseJson(): JsonObject = buildJsonObject {
    put("medication_order_id", prescriptionId)
    putNullable("server_id", serverId)
    put("encounter_id", encounterId)
    put("medication_name", medicationName)
    putNullable("dose", dose)
    putNullable("route", route)
    putNullable("frequency", frequency)
    putNullable("duration", duration)
    putNullable("instructions", instructions)
    put("version", version)
    put("created_at", createdAt)
    put("updated_at", updatedAt)
    putNullable("deleted_at", deletedAt)
}

private fun JsonObject.toPrescription(): Prescription = Prescription(
    prescriptionId = string("medication_order_id").orEmpty(),
    serverId = string("server_id"),
    encounterId = string("encounter_id").orEmpty(),
    medicationName = string("medication_name").orEmpty(),
    dose = string("dose"),
    frequency = string("frequency"),
    duration = string("duration"),
    route = string("route"),
    instructions = string("instructions"),
    version = int("version") ?: 1,
    createdAt = string("created_at") ?: Clock.System.now().toString(),
    updatedAt = string("updated_at") ?: Clock.System.now().toString(),
    deletedAt = string("deleted_at")
)

private fun EncounterOutcomeDto.toSupabaseJson(): JsonObject = buildJsonObject {
    put("outcome_id", outcomeId)
    put("encounter_id", encounterId)
    put("disposition", disposition)
    putNullable("referral_to", referralTo)
    put("admitted", admitted)
    putNullable("discharge_notes", dischargeNotes)
}

private fun OpdEncounterBundle.toOutcomeSupabaseJson(): JsonObject = buildJsonObject {
    put("outcome_id", "OUT-${encounter.encounterId}")
    put("encounter_id", encounter.encounterId)
    put("disposition", disposition ?: "discharged")
    putNullable("referral_to", referralTo)
    put("admitted", disposition.equals("admitted", ignoreCase = true))
    putNullable("discharge_notes", dischargeNotes)
}

private fun ImagingOrder.toSupabaseJson(): JsonObject = buildJsonObject {
    put("order_id", orderId)
    putNullable("server_id", serverId)
    put("encounter_id", encounterId)
    put("order_type", "IMAGING")
    put("order_name", studyName)
    putNullable("instructions", instructions)
    put("status", status)
    put("sync_state", "LOCAL_ONLY")
    put("version", version)
    put("created_at", createdAt)
    put("updated_at", updatedAt)
    putNullable("deleted_at", deletedAt)
}

private fun JsonObject.toImagingOrder(): ImagingOrder = ImagingOrder(
    orderId = string("order_id").orEmpty(),
    serverId = string("server_id"),
    encounterId = string("encounter_id").orEmpty(),
    studyName = string("order_name").orEmpty(),
    instructions = string("instructions"),
    status = string("status") ?: "ORDERED",
    version = int("version") ?: 1,
    createdAt = string("created_at") ?: Clock.System.now().toString(),
    updatedAt = string("updated_at") ?: Clock.System.now().toString(),
    deletedAt = string("deleted_at")
)

private fun HtsRegisterDto.toSupabaseJson(): JsonObject = buildJsonObject {
    put("hts_id", htsId)
    put("encounter_id", encounterId)
    putNullable("serial_number", serialNumber)
    putNullable("hts_number", htsNumber)
    put("population_type", populationType)
    put("testing_point", testingPoint)
    putNullable("test_1_result", test1Result)
    putNullable("test_2_result", test2Result)
    put("final_result", finalResult)
    putNullable("couple_testing", coupleTesting)
    putNullable("recency_test_result", recencyTestResult)
    putNullable("referred_to", referredTo)
    put("linked_to_care", linkedToCare)
    putNullable("remarks", remarks)
}

private fun ServiceEventDto.toSupabaseJson(): JsonObject = buildJsonObject {
    put("service_event_id", serviceEventId)
    put("encounter_id", encounterId)
    put("program", program)
    put("indicator_category", indicatorCategory)
    putNullable("service_code", serviceCode)
    putNullable("value_text", valueText)
    put("quantity", quantity)
    put("event_datetime", eventDatetime)
    put("sync_state", syncState)
}

private fun PatientDocumentDto.toSupabaseJson(): JsonObject = buildJsonObject {
    put("document_id", documentId)
    put("patient_id", patientId)
    put("document_type", documentType)
    put("image_uri", imageUri)
    put("verification_status", verificationStatus)
    putNullable("extracted_full_name", extractedFullName)
    putNullable("extracted_identifier", extractedIdentifier)
    putNullable("extracted_birth_date", extractedBirthDate)
    putNullable("extracted_sex", extractedSex)
    putNullable("extracted_guardian_name", extractedGuardianName)
    putNullable("notes", notes)
    put("captured_at", capturedAt)
}

private fun LabOrderDto.toSupabaseJson(facilityId: String = "default"): JsonObject = buildJsonObject {
    put("id", id)
    put("patient_id", patientId)
    putNullable("encounter_id", encounterId)
    put("ordered_by", orderedBy)
    put("department", department)
    put("status", status)
    put("priority", priority)
    putNullable("diagnosis_hint", diagnosisHint)
    putNullable("clinical_notes", clinicalNotes)
    putNullable("sample_id", sampleId)
    putNullable("billable_group_id", billableGroupId)
    putNullable("verified_by", verifiedBy)
    putNullable("verified_at", verifiedAt)
    putNullable("reported_by", reportedBy)
    putNullable("reported_at", reportedAt)
    put("created_at", createdAt)
    put("updated_at", updatedAt)
    put("facility_id", facilityId)
}

private fun LabOrderItemDto.toSupabaseJson(): JsonObject = buildJsonObject {
    put("id", id)
    put("order_id", orderId)
    put("test_id", testId)
    put("test_code", testCode)
    put("test_name", testName)
    put("status", status)
    put("priority", priority)
    putNullable("instructions", instructions)
    put("billing_code", billingCode)
    put("price", price)
    put("ordered_at", orderedAt)
    put("updated_at", updatedAt)
}

private fun LabResultDto.toSupabaseJson(): JsonObject = buildJsonObject {
    put("id", id)
    put("order_id", orderId)
    put("order_item_id", orderItemId)
    put("patient_id", patientId)
    put("test_id", testId)
    put("test_code", testCode)
    put("test_name", testName)
    put("value", value)
    putNullable("value_numeric", valueNumeric)
    putNullable("unit", unit)
    putNullable("reference_range", referenceRange)
    putNullable("flag", flag)
    putNullable("comment", comment)
    put("entered_by", enteredBy)
    put("entered_at", enteredAt)
    putNullable("verified_by", verifiedBy)
    putNullable("verified_at", verifiedAt)
    putNullable("reported_by", reportedBy)
    putNullable("reported_at", reportedAt)
    put("created_at", createdAt)
    put("updated_at", updatedAt)
}

private fun JsonObject.toLabOrderDto(items: List<LabOrderItemDto>): LabOrderDto = LabOrderDto(
    id = string("id").orEmpty(),
    patientId = string("patient_id").orEmpty(),
    encounterId = string("encounter_id"),
    orderedBy = string("ordered_by").orEmpty(),
    department = string("department").orEmpty(),
    status = string("status") ?: LabOrderStatus.ORDERED.name,
    priority = string("priority") ?: LabPriority.ROUTINE.name,
    diagnosisHint = string("diagnosis_hint"),
    clinicalNotes = string("clinical_notes"),
    items = items,
    sampleId = string("sample_id"),
    billableGroupId = string("billable_group_id"),
    createdAt = string("created_at") ?: Clock.System.now().toString(),
    updatedAt = string("updated_at") ?: Clock.System.now().toString(),
    verifiedBy = string("verified_by"),
    verifiedAt = string("verified_at"),
    reportedBy = string("reported_by"),
    reportedAt = string("reported_at")
)

private fun JsonObject.toLabOrderItemDto(): LabOrderItemDto = LabOrderItemDto(
    id = string("id").orEmpty(),
    orderId = string("order_id").orEmpty(),
    testId = string("test_id").orEmpty(),
    testCode = string("test_code").orEmpty(),
    testName = string("test_name").orEmpty(),
    status = string("status") ?: LabOrderStatus.ORDERED.name,
    priority = string("priority") ?: LabPriority.ROUTINE.name,
    instructions = string("instructions"),
    billingCode = string("billing_code").orEmpty(),
    price = double("price") ?: 0.0,
    orderedAt = string("ordered_at") ?: Clock.System.now().toString(),
    updatedAt = string("updated_at") ?: Clock.System.now().toString()
)

private fun JsonObject.toLabResultDto(): LabResultDto = LabResultDto(
    id = string("id").orEmpty(),
    orderId = string("order_id").orEmpty(),
    orderItemId = string("order_item_id").orEmpty(),
    patientId = string("patient_id").orEmpty(),
    testId = string("test_id").orEmpty(),
    testCode = string("test_code").orEmpty(),
    testName = string("test_name").orEmpty(),
    value = string("value").orEmpty(),
    valueNumeric = double("value_numeric"),
    unit = string("unit"),
    referenceRange = string("reference_range"),
    flag = string("flag"),
    comment = string("comment"),
    enteredBy = string("entered_by").orEmpty(),
    enteredAt = string("entered_at") ?: Clock.System.now().toString(),
    verifiedBy = string("verified_by"),
    verifiedAt = string("verified_at"),
    reportedBy = string("reported_by"),
    reportedAt = string("reported_at"),
    createdAt = string("created_at") ?: Clock.System.now().toString(),
    updatedAt = string("updated_at") ?: Clock.System.now().toString()
)

private fun LabOrder.toDtoLike(): LabOrderDto = LabOrderDto(
    id = id,
    patientId = patientId,
    encounterId = encounterId,
    orderedBy = orderedBy,
    department = department,
    status = status.name,
    priority = priority.name,
    diagnosisHint = diagnosisHint,
    clinicalNotes = clinicalNotes,
    items = items.map { it.toDtoLike() },
    sampleId = sampleId,
    billableGroupId = billableGroupId,
    createdAt = createdAt,
    updatedAt = updatedAt,
    verifiedBy = verifiedBy,
    verifiedAt = verifiedAt,
    reportedBy = reportedBy,
    reportedAt = reportedAt
)

private fun LabOrderItem.toDtoLike(): LabOrderItemDto = LabOrderItemDto(
    id = id,
    orderId = orderId,
    testId = testId,
    testCode = testCode,
    testName = testName,
    status = status.name,
    priority = priority.name,
    instructions = instructions,
    billingCode = billingCode,
    price = price,
    orderedAt = orderedAt,
    updatedAt = updatedAt
)

private fun LabOrderDto.toDomainLike(): LabOrder = LabOrder(
    id = id,
    patientId = patientId,
    encounterId = encounterId,
    orderedBy = orderedBy,
    department = department,
    status = enumValueOrNull<LabOrderStatus>(status) ?: LabOrderStatus.ORDERED,
    priority = enumValueOrNull<LabPriority>(priority) ?: LabPriority.ROUTINE,
    diagnosisHint = diagnosisHint,
    clinicalNotes = clinicalNotes,
    items = items.map { it.toDomainLike() },
    sampleId = sampleId,
    billableGroupId = billableGroupId,
    createdAt = createdAt,
    updatedAt = updatedAt,
    verifiedBy = verifiedBy,
    verifiedAt = verifiedAt,
    reportedBy = reportedBy,
    reportedAt = reportedAt
)

private fun LabOrderItemDto.toDomainLike(): LabOrderItem = LabOrderItem(
    id = id,
    orderId = orderId,
    testId = testId,
    testCode = testCode,
    testName = testName,
    status = enumValueOrNull<LabOrderStatus>(status) ?: LabOrderStatus.ORDERED,
    priority = enumValueOrNull<LabPriority>(priority) ?: LabPriority.ROUTINE,
    instructions = instructions,
    billingCode = billingCode,
    price = price,
    orderedAt = orderedAt,
    updatedAt = updatedAt
)

private fun waitMinutesSince(checkedInAt: String?): Int {
    val started = checkedInAt?.let { runCatching { Instant.parse(it) }.getOrNull() } ?: return 0
    val elapsed = Clock.System.now().toEpochMilliseconds() - started.toEpochMilliseconds()
    return (elapsed / 60_000.0).roundToInt().coerceAtLeast(0)
}

private fun JsonObject.string(name: String): String? =
    this[name]?.takeUnless { it is JsonNull }?.jsonPrimitive?.content

private fun JsonObject.int(name: String): Int? =
    this[name]?.takeUnless { it is JsonNull }?.jsonPrimitive?.intOrNull

private fun JsonObject.boolean(name: String): Boolean? =
    this[name]?.takeUnless { it is JsonNull }?.jsonPrimitive?.booleanOrNull

private fun JsonObject.double(name: String): Double? =
    this[name]?.takeUnless { it is JsonNull }?.jsonPrimitive?.content?.toDoubleOrNull()

private fun JsonObject.sex(name: String): Sex =
    string(name)?.let { raw -> Sex.entries.find { it.code == raw || it.name == raw } } ?: Sex.UNKNOWN

private inline fun <reified T : Enum<T>> enumValueOrNull(value: String): T? =
    runCatching { enumValueOf<T>(value) }.getOrNull()

private suspend fun <T, R> Iterable<T>.mapSuspend(transform: suspend (T) -> R): List<R> {
    val result = ArrayList<R>()
    for (item in this) result += transform(item)
    return result
}

private suspend fun <T, R : Any> Iterable<T>.mapNotNullSuspend(transform: suspend (T) -> R?): List<R> {
    val result = ArrayList<R>()
    for (item in this) transform(item)?.let { result += it }
    return result
}

private fun kotlinx.serialization.json.JsonObjectBuilder.putNullable(name: String, value: String?) {
    if (value == null) put(name, JsonNull) else put(name, value)
}

private fun kotlinx.serialization.json.JsonObjectBuilder.putNullable(name: String, value: Number?) {
    if (value == null) put(name, JsonNull) else put(name, JsonPrimitive(value))
}

private fun JsonObject.toSchedule(): Schedule = Schedule(
    id = string("id").orEmpty(),
    actorType = string("actor_type").orEmpty(),
    actorId = string("actor_id").orEmpty(),
    name = string("name").orEmpty(),
    active = boolean("active") ?: true
)

private fun Schedule.toSupabaseJson(facilityId: String = "default"): JsonObject = buildJsonObject {
    put("id", id)
    put("actor_type", actorType)
    put("actor_id", actorId)
    put("name", name)
    put("active", active)
    put("facility_id", facilityId)
}

private fun JsonObject.toSlot(): Slot = Slot(
    id = string("id").orEmpty(),
    scheduleId = string("schedule_id").orEmpty(),
    startTime = string("start_time").orEmpty(),
    endTime = string("end_time").orEmpty(),
    status = string("status") ?: "free"
)

private fun Slot.toSupabaseJson(): JsonObject = buildJsonObject {
    put("id", id)
    put("schedule_id", scheduleId)
    put("start_time", startTime)
    put("end_time", endTime)
    put("status", status)
}

private fun JsonObject.toAppointment(): Appointment = Appointment(
    id = string("id").orEmpty(),
    patientId = string("patient_id").orEmpty(),
    scheduleId = string("schedule_id").orEmpty(),
    slotId = string("slot_id"),
    status = string("status") ?: "booked",
    appointmentType = string("appointment_type").orEmpty(),
    reason = string("reason"),
    startTime = string("start_time").orEmpty(),
    endTime = string("end_time").orEmpty(),
    createdAt = string("created_at").orEmpty(),
    updatedAt = string("updated_at").orEmpty()
)

private fun Appointment.toSupabaseJson(facilityId: String = "default"): JsonObject = buildJsonObject {
    put("id", id)
    put("patient_id", patientId)
    put("schedule_id", scheduleId)
    putNullable("slot_id", slotId)
    put("status", status)
    put("appointment_type", appointmentType)
    putNullable("reason", reason)
    put("start_time", startTime)
    put("end_time", endTime)
    put("facility_id", facilityId)
    put("created_at", createdAt)
    put("updated_at", updatedAt)
}
private fun JsonObject.toTenantHospital(): TenantHospital = TenantHospital(
    id = string("id").orEmpty(),
    name = string("name").orEmpty(),
    tenantCode = string("tenant_code").orEmpty(),
    contactEmail = string("contact_email").orEmpty(),
    billingPlan = string("billing_plan").orEmpty(),
    billingStatus = string("billing_status").orEmpty(),
    amountBilled = double("amount_billed") ?: 0.0,
    createdAt = string("created_at") ?: Clock.System.now().toString()
)

@kotlinx.serialization.Serializable
data class TenantHospital(
    val id: String,
    val name: String,
    val tenantCode: String,
    val contactEmail: String,
    val billingPlan: String,
    val billingStatus: String,
    val amountBilled: Double,
    val createdAt: String
)

@kotlinx.serialization.Serializable
data class TenantMetrics(
    val patientsCount: Int,
    val staffCount: Int,
    val activeQueueCount: Int
)
