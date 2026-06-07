package com.egesa.clinic.shared.data

import com.egesa.clinic.shared.*
import com.egesa.clinic.shared.domain.OpdEncounterBundle
import com.egesa.clinic.shared.domain.LabOrder
import com.egesa.clinic.shared.domain.LabOrderItem
import com.egesa.clinic.shared.domain.LabOrderStatus
import com.egesa.clinic.shared.domain.LabPriority
import com.egesa.clinic.shared.domain.LabResult
import com.egesa.clinic.shared.domain.LabResultFlag
import com.egesa.clinic.shared.domain.LabSample
import com.egesa.clinic.shared.sync.SyncHealthStatus
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.Serializable

interface ClinicApi {
    suspend fun login(staffId: String, pin: String, tenantCode: String? = null): LoginResponseDto
    suspend fun getStaff(): List<StaffMemberDto>
    suspend fun getPatients(): List<PatientDto>
    suspend fun getPatient(id: String): PatientDto?
    suspend fun getQueue(): List<QueueItemDto>
    suspend fun registerPatient(request: PatientRegistrationDto): PatientRegistrationResponseDto
    suspend fun checkInPatient(request: QueueCheckInRequestDto): QueueItemDto
    suspend fun checkOutPatient(patientId: String): QueueItemDto
    suspend fun getKpis(): List<DashboardMetricDto>
    suspend fun getBottlenecks(): List<BottleneckCellDto>
    suspend fun getRegistrationTrend(): List<TrendPointDto>
    suspend fun getWardOverview(): WardOverviewDto
    suspend fun getBedBoard(): List<BedCardDto>
    suspend fun getNursingTasks(): List<NursingTaskDto>
    suspend fun getWardCensus(): List<WardCensusRowDto>
    suspend fun getAtdState(): AdmissionTransferDischargeStateDto
    suspend fun getShiftHandoff(shift: Shift): List<String>
    suspend fun syncPatientData(remoteVersion: Long): SyncPatientDataDto
    suspend fun uploadPatientChanges(changes: List<PatientDto>): List<SyncResultItemDto>
    suspend fun uploadClinicalChanges(batch: ClinicalSyncBatchDto): List<SyncResultItemDto>
    suspend fun createLabOrder(request: LabOrderDto): LabOrderDto
    suspend fun getLabOrder(id: String): LabOrderDto?
    suspend fun getLabOrdersForPatient(patientId: String): List<LabOrderDto>
    suspend fun getLabWorklist(department: String, status: String? = null, fromDate: String? = null, toDate: String? = null): List<LabOrderDto>
    suspend fun saveLabResults(request: SaveLabResultsRequestDto): List<LabResultDto>
    suspend fun updateLabOrderStatus(orderId: String, request: UpdateLabOrderStatusRequestDto): LabOrderDto
    suspend fun resolveConflict(request: ConflictResolutionRequestDto): ConflictResolutionResultDto
    suspend fun getSyncHealth(): SyncHealthStatus
    suspend fun createEncounter(bundle: OpdEncounterBundle): OpdEncounterBundle
    suspend fun getEncounter(id: String): OpdEncounterBundle
    suspend fun getPatientEncounters(patientId: String): List<OpdEncounterBundle>
}

object ClinicApiProvider {
    var api: ClinicApi? = null
        private set

    fun install(api: ClinicApi) {
        this.api = api
    }

    fun clear() {
        api = null
    }
}

class KtorClinicApi(private val client: HttpClient, private val baseUrl: String) : ClinicApi {
    override suspend fun login(staffId: String, pin: String, tenantCode: String?): LoginResponseDto =
        client.post("$baseUrl/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequestDto(staffId = staffId, pin = pin, tenantCode = tenantCode))
        }.body()

    override suspend fun getStaff(): List<StaffMemberDto> = client.get("$baseUrl/auth/staff").body()
    override suspend fun getPatients(): List<PatientDto> = client.get("$baseUrl/patients").body()
    override suspend fun getPatient(id: String): PatientDto? = client.get("$baseUrl/patients/$id").body()
    override suspend fun getQueue(): List<QueueItemDto> = client.get("$baseUrl/queue").body()
    override suspend fun registerPatient(request: PatientRegistrationDto): PatientRegistrationResponseDto =
        client.post("$baseUrl/patients") { contentType(ContentType.Application.Json); setBody(request) }.body()

    override suspend fun checkInPatient(request: QueueCheckInRequestDto): QueueItemDto =
        client.post("$baseUrl/queue/check-in") { contentType(ContentType.Application.Json); setBody(request) }.body()

    override suspend fun checkOutPatient(patientId: String): QueueItemDto =
        client.post("$baseUrl/queue/$patientId/check-out") { contentType(ContentType.Application.Json) }.body()

    override suspend fun getKpis(): List<DashboardMetricDto> = client.get("$baseUrl/dashboard/kpis").body()
    override suspend fun getBottlenecks(): List<BottleneckCellDto> = client.get("$baseUrl/dashboard/bottlenecks").body()
    override suspend fun getRegistrationTrend(): List<TrendPointDto> = client.get("$baseUrl/dashboard/trend").body()
    override suspend fun getWardOverview(): WardOverviewDto = client.get("$baseUrl/wards/overview").body()
    override suspend fun getBedBoard(): List<BedCardDto> = client.get("$baseUrl/wards/beds").body()
    override suspend fun getNursingTasks(): List<NursingTaskDto> = client.get("$baseUrl/wards/tasks").body()
    override suspend fun getWardCensus(): List<WardCensusRowDto> = client.get("$baseUrl/wards/census").body()
    override suspend fun getAtdState(): AdmissionTransferDischargeStateDto = client.get("$baseUrl/wards/atd").body()
    override suspend fun getShiftHandoff(shift: Shift): List<String> = client.get("$baseUrl/wards/handoff") { parameter("shift", shift.name) }.body()
    override suspend fun syncPatientData(remoteVersion: Long): SyncPatientDataDto = client.get("$baseUrl/sync/patients") { parameter("version", remoteVersion) }.body()
    override suspend fun uploadPatientChanges(changes: List<PatientDto>): List<SyncResultItemDto> =
        client.post("$baseUrl/sync/patients/batch") { contentType(ContentType.Application.Json); setBody(changes) }.body()

    override suspend fun uploadClinicalChanges(batch: ClinicalSyncBatchDto): List<SyncResultItemDto> =
        client.post("$baseUrl/sync/clinical/batch") { contentType(ContentType.Application.Json); setBody(batch) }.body()

    override suspend fun createLabOrder(request: LabOrderDto): LabOrderDto =
        client.post("$baseUrl/api/lab/orders") { contentType(ContentType.Application.Json); setBody(request) }.body()

    override suspend fun getLabOrder(id: String): LabOrderDto? =
        client.get("$baseUrl/api/lab/orders/$id").body()

    override suspend fun getLabOrdersForPatient(patientId: String): List<LabOrderDto> =
        client.get("$baseUrl/api/patients/$patientId/lab-orders").body()

    override suspend fun getLabWorklist(department: String, status: String?, fromDate: String?, toDate: String?): List<LabOrderDto> =
        client.get("$baseUrl/api/lab/worklist") {
            parameter("department", department)
            status?.let { parameter("status", it) }
            fromDate?.let { parameter("fromDate", it) }
            toDate?.let { parameter("toDate", it) }
        }.body()

    override suspend fun saveLabResults(request: SaveLabResultsRequestDto): List<LabResultDto> =
        client.post("$baseUrl/api/lab/results") { contentType(ContentType.Application.Json); setBody(request) }.body()

    override suspend fun updateLabOrderStatus(orderId: String, request: UpdateLabOrderStatusRequestDto): LabOrderDto =
        client.post("$baseUrl/api/lab/orders/$orderId/status") { contentType(ContentType.Application.Json); setBody(request) }.body()

    override suspend fun resolveConflict(request: ConflictResolutionRequestDto): ConflictResolutionResultDto =
        client.post("$baseUrl/sync/resolve-conflict") { contentType(ContentType.Application.Json); setBody(request) }.body()

    override suspend fun getSyncHealth(): SyncHealthStatus =
        client.get("$baseUrl/payments/sync-health").body()

    override suspend fun createEncounter(bundle: OpdEncounterBundle): OpdEncounterBundle =
        client.post("$baseUrl/api/encounters") { contentType(ContentType.Application.Json); setBody(bundle) }.body()

    override suspend fun getEncounter(id: String): OpdEncounterBundle =
        client.get("$baseUrl/api/encounters/$id").body()

    override suspend fun getPatientEncounters(patientId: String): List<OpdEncounterBundle> =
        client.get("$baseUrl/api/patients/$patientId/encounters").body()
}

@Serializable
data class LoginRequestDto(
    val staffId: String,
    val pin: String,
    val tenantCode: String? = null
)
@Serializable
data class LoginResponseDto(
    val accessToken: String,
    val expiresAtEpochSeconds: Long,
    val role: String,
    val staffName: String,
    val facilityId: String = "default",
    val tenantCode: String? = null
)
@Serializable data class StaffMemberDto(val id: String, val fullName: String, val role: String, val department: String)
@Serializable data class PatientDto(
    val id: String, val fullName: String, val age: Int, val sex: String, val status: String,
    val assignedWard: String? = null, val roomBed: String? = null, val acuity: String = "Moderate",
    val isolation: String? = null, val visits: Int = 0, val activeDiagnosis: String = "",
    val currentMedications: List<String> = emptyList(), val timeline: List<TimelineEvent> = emptyList()
)
@Serializable data class PatientRegistrationDto(
    val id: String,
    val fullName: String,
    val age: Int,
    val sex: Sex,
    val status: String = "Checked in",
    val assignedWard: String? = null,
    val roomBed: String? = null,
    val acuity: String = "Moderate",
    val isolation: String? = null,
    val triageLevel: Int = 3
)
@Serializable data class PatientRegistrationResponseDto(val patient: PatientDto, val queueItem: QueueItemDto? = null)
@Serializable data class QueueCheckInRequestDto(
    val patientId: String,
    val name: String,
    val triageLevel: Int = 3,
    val checkedInAt: String? = null
)
@Serializable data class QueueItemDto(
    val patientId: String,
    val name: String,
    val triageLevel: Int,
    val waitMinutes: Int,
    val status: String = "WAITING",
    val checkedInAt: String? = null,
    val checkedOutAt: String? = null
)
@Serializable data class DashboardMetricDto(val title: String, val value: String, val subtitle: String? = null)
@Serializable data class BottleneckCellDto(val workflowStage: String, val severity: String, val pendingCount: Int)
@Serializable data class TrendPointDto(val label: String, val value: Int)
@Serializable data class WardOverviewDto(val occupancyPercent: Int, val bedsAvailable: Int, val nurseWorkload: String, val alerts: List<String>)
@Serializable data class BedCardDto(val ward: String, val roomBed: String, val patientName: String, val status: String, val acuity: String, val isolation: String?)
@Serializable data class NursingTaskDto(val type: String, val detail: String, val due: String, val priority: String)
@Serializable data class WardCensusRowDto(val ward: String, val occupiedBeds: Int, val totalBeds: Int, val highAcuityCount: Int, val isolationCount: Int)
@Serializable data class ChecklistItemDto(val label: String, val complete: Boolean)
@Serializable data class AdmissionTransferDischargeStateDto(val selectedPatientId: String, val selectedBed: String, val transferWard: String, val dischargeChecklist: List<ChecklistItemDto>)
@Serializable data class SyncPatientDataDto(val patients: List<PatientDto>, val remoteVersion: Long, val count: Int)
@Serializable data class SyncResultItemDto(val id: String, val status: String, val version: Int)
@Serializable data class ConflictResolutionRequestDto(val entityId: String, val localVersion: Int, val remoteVersion: Int, val strategy: String)
@Serializable data class ConflictResolutionResultDto(val resolved: Boolean, val strategy: String, val finalVersion: Int)

@Serializable data class ClinicalSyncBatchDto(
    val encounters: List<EncounterDto> = emptyList(),
    val vitalSigns: List<VitalSignsDto> = emptyList(),
    val diagnoses: List<DiagnosisDto> = emptyList(),
    val medicationOrders: List<MedicationOrderDto> = emptyList(),
    val encounterOutcomes: List<EncounterOutcomeDto> = emptyList(),
    val htsEntries: List<HtsRegisterDto> = emptyList(),
    val serviceEvents: List<ServiceEventDto> = emptyList(),
    val patientDocuments: List<PatientDocumentDto> = emptyList()
)

@Serializable
data class LabOrderDto(
    val id: String,
    val patientId: String,
    val encounterId: String? = null,
    val orderedBy: String,
    val department: String,
    val status: String,
    val priority: String,
    val diagnosisHint: String? = null,
    val clinicalNotes: String? = null,
    val items: List<LabOrderItemDto> = emptyList(),
    val sampleId: String? = null,
    val billableGroupId: String? = null,
    val createdAt: String,
    val updatedAt: String,
    val verifiedBy: String? = null,
    val verifiedAt: String? = null,
    val reportedBy: String? = null,
    val reportedAt: String? = null,
)

@Serializable
data class LabOrderItemDto(
    val id: String,
    val orderId: String,
    val testId: String,
    val testCode: String,
    val testName: String,
    val status: String,
    val priority: String,
    val instructions: String? = null,
    val billingCode: String,
    val price: Double,
    val orderedAt: String,
    val updatedAt: String,
)

@Serializable
data class LabSampleDto(
    val id: String,
    val orderId: String,
    val patientId: String,
    val specimenType: String,
    val accessionNumber: String? = null,
    val collectedBy: String? = null,
    val collectedAt: String? = null,
    val receivedBy: String? = null,
    val receivedAt: String? = null,
    val rejectedReason: String? = null,
    val status: String,
    val createdAt: String,
    val updatedAt: String,
)

@Serializable
data class LabResultDto(
    val id: String,
    val orderId: String,
    val orderItemId: String,
    val patientId: String,
    val testId: String,
    val testCode: String,
    val testName: String,
    val value: String,
    val valueNumeric: Double? = null,
    val unit: String? = null,
    val referenceRange: String? = null,
    val flag: String? = null,
    val comment: String? = null,
    val enteredBy: String,
    val enteredAt: String,
    val verifiedBy: String? = null,
    val verifiedAt: String? = null,
    val reportedBy: String? = null,
    val reportedAt: String? = null,
    val createdAt: String,
    val updatedAt: String,
)

@Serializable
data class SaveLabResultsRequestDto(
    val orderId: String,
    val actorId: String,
    val results: List<LabResultDto>,
)

@Serializable
data class UpdateLabOrderStatusRequestDto(
    val status: String,
    val actorId: String,
)

@Serializable data class EncounterDto(
    val encounterId: String,
    val patientId: String,
    val encounterDatetime: String,
    val department: String,
    val visitType: String,
    val providerId: String? = null,
    val facilityId: String,
    val locationId: String? = null,
    val sourceType: String? = null,
    val sourceId: String? = null,
    val status: String = "DRAFT",
    val version: Int = 1,
    val updatedAt: String? = null,
    val nursingNotes: String? = null,
    val syncState: String = "LOCAL_ONLY"
)

@Serializable data class VitalSignsDto(
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
    val recordedAt: String
)

@Serializable data class DiagnosisDto(
    val diagnosisId: String,
    val encounterId: String,
    val diagnosisText: String,
    val isPrimary: Boolean = false,
    val codeSystem: String? = null,
    val diagnosisCode: String? = null
)

@Serializable data class MedicationOrderDto(
    val medicationOrderId: String,
    val encounterId: String,
    val medicationName: String,
    val dose: String? = null,
    val route: String? = null,
    val frequency: String? = null,
    val duration: String? = null,
    val instructions: String? = null
)

@Serializable data class EncounterOutcomeDto(
    val outcomeId: String,
    val encounterId: String,
    val disposition: String,
    val referralTo: String? = null,
    val admitted: Boolean = false,
    val dischargeNotes: String? = null
)

@Serializable data class HtsRegisterDto(
    val htsId: String,
    val encounterId: String,
    val serialNumber: String? = null,
    val htsNumber: String? = null,
    val populationType: String,
    val testingPoint: String,
    val test1Result: String? = null,
    val test2Result: String? = null,
    val finalResult: String,
    val coupleTesting: String? = null,
    val recencyTestResult: String? = null,
    val referredTo: String? = null,
    val linkedToCare: Boolean = false,
    val remarks: String? = null
)

@Serializable data class ServiceEventDto(
    val serviceEventId: String,
    val encounterId: String,
    val program: String,
    val indicatorCategory: String,
    val serviceCode: String? = null,
    val valueText: String? = null,
    val quantity: Long = 1,
    val eventDatetime: String,
    val syncState: String = "LOCAL_ONLY"
)

@Serializable data class PatientDocumentDto(
    val documentId: String,
    val patientId: String,
    val documentType: String,
    val imageUri: String,
    val verificationStatus: String,
    val extractedFullName: String? = null,
    val extractedIdentifier: String? = null,
    val extractedBirthDate: String? = null,
    val extractedSex: String? = null,
    val extractedGuardianName: String? = null,
    val notes: String? = null,
    val capturedAt: String
)

fun PatientDto.toDomain() = Patient(id, fullName, age, sex, status, assignedWard, roomBed, acuity, isolation, visits, activeDiagnosis, currentMedications, timeline)
fun Patient.toDto() = PatientDto(id, fullName, age, sex, status, assignedWard, roomBed, acuity, isolation, visits, activeDiagnosis, currentMedications, timeline)
fun PatientRegistrationDto.toPatient() = Patient(id, fullName, age, sex, status, assignedWard, roomBed, acuity, isolation)
fun PatientRegistrationInput.toRegistrationDto(triageLevel: Int) = PatientRegistrationDto(
    id = id,
    fullName = fullName,
    age = age,
    sex = sex,
    status = status,
    assignedWard = assignedWard,
    roomBed = roomBed,
    acuity = acuity,
    isolation = isolation,
    triageLevel = triageLevel
)
fun StaffMemberDto.toDomain() = StaffMember(id, fullName, runCatching { UserRole.valueOf(role) }.getOrDefault(UserRole.RECEPTIONIST), department)
fun QueueItemDto.toDomain() = QueueItem(patientId, name, triageLevel, waitMinutes, status, checkedInAt, checkedOutAt)
fun QueueItem.toDto() = QueueItemDto(patientId, name, triageLevel, waitMinutes, status, checkedInAt, checkedOutAt)
fun DashboardMetricDto.toDomain() = DashboardMetric(title, value, subtitle)
fun BottleneckCellDto.toDomain() = BottleneckCell(workflowStage, severity, pendingCount)
fun TrendPointDto.toDomain() = TrendPoint(label, value)
fun WardOverviewDto.toDomain() = WardOverview(occupancyPercent, bedsAvailable, nurseWorkload, alerts)
fun BedCardDto.toDomain() = BedCard(ward, roomBed, patientName, status, acuity, isolation)
fun NursingTaskDto.toDomain() = NursingTask(type, detail, due, priority)
fun WardCensusRowDto.toDomain() = WardCensusRow(ward, occupiedBeds, totalBeds, highAcuityCount, isolationCount)
fun AdmissionTransferDischargeStateDto.toDomain() = AdmissionTransferDischargeState(selectedPatientId, selectedBed, transferWard, dischargeChecklist.map { it.label to it.complete })
fun SyncPatientDataDto.toDomain() = SyncPatientDataResponse(patients.map { it.toDomain() }, remoteVersion, count)
fun SyncResultItemDto.toDomain() = SyncResultItem(id, status, version)
fun ConflictResolutionResultDto.toDomain() = ConflictResolutionResult(resolved, strategy, finalVersion)

fun LabOrderDto.toDomain(): LabOrder = LabOrder(
    id = id,
    patientId = patientId,
    encounterId = encounterId,
    orderedBy = orderedBy,
    department = department,
    status = enumValueOrDefault(status, LabOrderStatus.ORDERED),
    priority = enumValueOrDefault(priority, LabPriority.ROUTINE),
    diagnosisHint = diagnosisHint,
    clinicalNotes = clinicalNotes,
    items = items.map { it.toDomain() },
    sampleId = sampleId,
    billableGroupId = billableGroupId,
    createdAt = createdAt,
    updatedAt = updatedAt,
    verifiedBy = verifiedBy,
    verifiedAt = verifiedAt,
    reportedBy = reportedBy,
    reportedAt = reportedAt,
)

fun LabOrder.toDto(): LabOrderDto = LabOrderDto(
    id = id,
    patientId = patientId,
    encounterId = encounterId,
    orderedBy = orderedBy,
    department = department,
    status = status.name,
    priority = priority.name,
    diagnosisHint = diagnosisHint,
    clinicalNotes = clinicalNotes,
    items = items.map { it.toDto() },
    sampleId = sampleId,
    billableGroupId = billableGroupId,
    createdAt = createdAt,
    updatedAt = updatedAt,
    verifiedBy = verifiedBy,
    verifiedAt = verifiedAt,
    reportedBy = reportedBy,
    reportedAt = reportedAt,
)

fun LabOrderItemDto.toDomain(): LabOrderItem = LabOrderItem(
    id = id,
    orderId = orderId,
    testId = testId,
    testCode = testCode,
    testName = testName,
    status = enumValueOrDefault(status, LabOrderStatus.ORDERED),
    priority = enumValueOrDefault(priority, LabPriority.ROUTINE),
    instructions = instructions,
    billingCode = billingCode,
    price = price,
    orderedAt = orderedAt,
    updatedAt = updatedAt,
)

fun LabOrderItem.toDto(): LabOrderItemDto = LabOrderItemDto(
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
    updatedAt = updatedAt,
)

fun LabResultDto.toDomain(): LabResult = LabResult(
    id = id,
    orderId = orderId,
    orderItemId = orderItemId,
    patientId = patientId,
    testId = testId,
    testCode = testCode,
    testName = testName,
    value = value,
    valueNumeric = valueNumeric,
    unit = unit,
    referenceRange = referenceRange,
    flag = flag?.let { enumValueOrDefault(it, LabResultFlag.NORMAL) },
    comment = comment,
    enteredBy = enteredBy,
    enteredAt = enteredAt,
    verifiedBy = verifiedBy,
    verifiedAt = verifiedAt,
    reportedBy = reportedBy,
    reportedAt = reportedAt,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun LabResult.toDto(): LabResultDto = LabResultDto(
    id = id,
    orderId = orderId,
    orderItemId = orderItemId,
    patientId = patientId,
    testId = testId,
    testCode = testCode,
    testName = testName,
    value = value,
    valueNumeric = valueNumeric,
    unit = unit,
    referenceRange = referenceRange,
    flag = flag?.name,
    comment = comment,
    enteredBy = enteredBy,
    enteredAt = enteredAt,
    verifiedBy = verifiedBy,
    verifiedAt = verifiedAt,
    reportedBy = reportedBy,
    reportedAt = reportedAt,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun LabSampleDto.toDomain(): LabSample = LabSample(
    id = id,
    orderId = orderId,
    patientId = patientId,
    specimenType = specimenType,
    accessionNumber = accessionNumber,
    collectedBy = collectedBy,
    collectedAt = collectedAt,
    receivedBy = receivedBy,
    receivedAt = receivedAt,
    rejectedReason = rejectedReason,
    status = enumValueOrDefault(status, LabOrderStatus.ORDERED),
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun LabSample.toDto(): LabSampleDto = LabSampleDto(
    id = id,
    orderId = orderId,
    patientId = patientId,
    specimenType = specimenType,
    accessionNumber = accessionNumber,
    collectedBy = collectedBy,
    collectedAt = collectedAt,
    receivedBy = receivedBy,
    receivedAt = receivedAt,
    rejectedReason = rejectedReason,
    status = status.name,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

private inline fun <reified T : Enum<T>> enumValueOrDefault(value: String, fallback: T): T =
    runCatching { enumValueOf<T>(value) }.getOrDefault(fallback)
