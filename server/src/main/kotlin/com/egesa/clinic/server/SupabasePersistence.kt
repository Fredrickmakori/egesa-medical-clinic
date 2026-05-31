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
import com.egesa.clinic.shared.data.ServiceEventDto
import com.egesa.clinic.shared.data.SyncResultItemDto
import com.egesa.clinic.shared.data.VitalSignsDto
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

    suspend fun staffDirectory(): List<AuthUser> =
        client.select("staff_members", filters = mapOf("active" to "eq.true"), order = "full_name.asc")
            .mapNotNull { it.jsonObject.toAuthUserOrNull() }

    suspend fun verifyStaff(staffId: String, pin: String): AuthUser? {
        val row = client.select(
            "staff_members",
            filters = mapOf("id" to "eq.$staffId", "active" to "eq.true"),
            limit = 1
        ).firstOrNull()?.jsonObject ?: return null
        val storedPin = row.string("pin_hash") ?: return null
        return row.toAuthUserOrNull()?.takeIf { storedPin == pin }
    }

    suspend fun allPatients(): List<Patient> =
        client.select("patients", order = "full_name.asc").map { it.jsonObject.toPatient() }

    suspend fun patientById(id: String): Patient? =
        client.select("patients", filters = mapOf("id" to "eq.$id"), limit = 1)
            .firstOrNull()?.jsonObject?.toPatient()

    suspend fun upsertPatient(patient: Patient, triageLevel: Int? = null): Patient {
        val saved = client.upsert(
            table = "patients",
            rows = JsonArray(listOf(patient.toSupabasePatientJson(triageLevel))),
            onConflict = "id"
        ).firstOrNull()?.jsonObject
        return saved?.toPatient() ?: patient
    }

    suspend fun activeQueue(): List<QueueItem> =
        client.select(
            "queue_entries",
            filters = mapOf("status" to "neq.CHECKED_OUT"),
            order = "checked_in_at.asc"
        ).map { it.jsonObject.toQueueItem() }

    suspend fun checkInPatient(patientId: String, name: String, triageLevel: Int, checkedInAt: String? = null): QueueItem {
        val now = checkedInAt ?: Clock.System.now().toString()
        val item = QueueItem(patientId, name, triageLevel.coerceIn(1, 5), 0, "WAITING", now, null)
        client.upsert(
            "queue_entries",
            JsonArray(listOf(item.toSupabaseQueueJson())),
            "patient_id"
        )
        return item
    }

    suspend fun checkOutPatient(patientId: String): QueueItem? {
        val now = Clock.System.now().toString()
        val patched = client.patch(
            "queue_entries",
            buildJsonObject {
                put("status", "CHECKED_OUT")
                put("checked_out_at", now)
                put("updated_at", now)
            },
            filters = mapOf("patient_id" to "eq.$patientId", "status" to "neq.CHECKED_OUT")
        )
        return patched.firstOrNull()?.jsonObject?.toQueueItem()
    }

    suspend fun auditTrail(limit: Int = 200): List<AuditEvent> =
        client.select("audit_events", order = "timestamp.desc", limit = limit)
            .map { it.jsonObject.toAuditEvent() }

    suspend fun logAudit(event: AuditEvent) {
        client.insert("audit_events", event.toSupabaseAuditJson())
    }

    suspend fun uploadPatients(changes: List<PatientDto>): List<SyncResultItemDto> {
        if (changes.isEmpty()) return emptyList()
        return runBatch(changes, "Patient") { dto ->
            client.upsert("patients", JsonArray(listOf(dto.toSupabasePatientJson())), "id")
        }
    }

    suspend fun uploadClinical(batch: ClinicalSyncBatchDto): List<SyncResultItemDto> {
        val results = mutableListOf<SyncResultItemDto>()
        results += upsertClinical("encounters", "encounter_id", batch.encounters, EncounterDto::encounterId) { it.toSupabaseJson() }
        results += upsertClinical("vital_signs", "vital_signs_id", batch.vitalSigns, VitalSignsDto::vitalSignsId) { it.toSupabaseJson() }
        results += upsertClinical("diagnosis", "diagnosis_id", batch.diagnoses, DiagnosisDto::diagnosisId) { it.toSupabaseJson() }
        results += upsertClinical("medication_order", "medication_order_id", batch.medicationOrders, MedicationOrderDto::medicationOrderId) { it.toSupabaseJson() }
        results += upsertClinical("encounter_outcome", "outcome_id", batch.encounterOutcomes, EncounterOutcomeDto::outcomeId) { it.toSupabaseJson() }
        results += upsertClinical("hts_register", "hts_id", batch.htsEntries, HtsRegisterDto::htsId) { it.toSupabaseJson() }
        results += upsertClinical("service_events", "service_event_id", batch.serviceEvents, ServiceEventDto::serviceEventId) { it.toSupabaseJson() }
        results += upsertClinical("patient_documents", "document_id", batch.patientDocuments, PatientDocumentDto::documentId) { it.toSupabaseJson() }
        return results
    }

    suspend fun allSchedules(): List<Schedule> =
        client.select("schedules", order = "name.asc").map { it.jsonObject.toSchedule() }

    suspend fun upsertSchedule(schedule: Schedule): Schedule {
        val saved = client.upsert("schedules", JsonArray(listOf(schedule.toSupabaseJson())), "id").firstOrNull()?.jsonObject
        return saved?.toSchedule() ?: schedule
    }

    suspend fun slotsForSchedule(scheduleId: String): List<Slot> =
        client.select("slots", filters = mapOf("schedule_id" to "eq.$scheduleId"), order = "start_time.asc")
            .map { it.jsonObject.toSlot() }

    suspend fun upsertSlot(slot: Slot): Slot {
        val saved = client.upsert("slots", JsonArray(listOf(slot.toSupabaseJson())), "id").firstOrNull()?.jsonObject
        return saved?.toSlot() ?: slot
    }

    suspend fun allAppointments(): List<Appointment> =
        client.select("appointments", order = "start_time.asc").map { it.jsonObject.toAppointment() }

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

    suspend fun upsertAppointment(appointment: Appointment): Appointment {
        val saved = client.upsert("appointments", JsonArray(listOf(appointment.toSupabaseJson())), "id").firstOrNull()?.jsonObject
        return saved?.toAppointment() ?: appointment
    }

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
    return AuthUser(id, name, role, pin)
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

private fun Patient.toSupabasePatientJson(triageLevel: Int? = null): JsonObject = buildJsonObject {
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
    put("updated_at", Clock.System.now().toString())
}

private fun PatientDto.toSupabasePatientJson(): JsonObject = buildJsonObject {
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

private fun QueueItem.toSupabaseQueueJson(): JsonObject = buildJsonObject {
    put("patient_id", patientId)
    put("name", name)
    put("triage_level", triageLevel)
    put("status", status)
    putNullable("checked_in_at", checkedInAt)
    putNullable("checked_out_at", checkedOutAt)
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

private fun EncounterDto.toSupabaseJson(): JsonObject = buildJsonObject {
    put("encounter_id", encounterId)
    put("patient_id", patientId)
    put("encounter_datetime", encounterDatetime)
    put("department", department)
    put("visit_type", visitType)
    putNullable("provider_id", providerId)
    put("facility_id", facilityId)
    put("updated_at", Clock.System.now().toString())
}

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

private fun DiagnosisDto.toSupabaseJson(): JsonObject = buildJsonObject {
    put("diagnosis_id", diagnosisId)
    put("encounter_id", encounterId)
    put("diagnosis_text", diagnosisText)
    put("is_primary", isPrimary)
    putNullable("code_system", codeSystem)
    putNullable("diagnosis_code", diagnosisCode)
}

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

private fun EncounterOutcomeDto.toSupabaseJson(): JsonObject = buildJsonObject {
    put("outcome_id", outcomeId)
    put("encounter_id", encounterId)
    put("disposition", disposition)
    putNullable("referral_to", referralTo)
    put("admitted", admitted)
    putNullable("discharge_notes", dischargeNotes)
}

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

private fun JsonObject.sex(name: String): Sex =
    string(name)?.let { raw -> Sex.entries.find { it.code == raw || it.name == raw } } ?: Sex.UNKNOWN

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

private fun Schedule.toSupabaseJson(): JsonObject = buildJsonObject {
    put("id", id)
    put("actor_type", actorType)
    put("actor_id", actorId)
    put("name", name)
    put("active", active)
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

private fun Appointment.toSupabaseJson(): JsonObject = buildJsonObject {
    put("id", id)
    put("patient_id", patientId)
    put("schedule_id", scheduleId)
    putNullable("slot_id", slotId)
    put("status", status)
    put("appointment_type", appointmentType)
    putNullable("reason", reason)
    put("start_time", startTime)
    put("end_time", endTime)
    put("created_at", createdAt)
    put("updated_at", updatedAt)
}
