package com.egesa.clinic.shared.data

import com.egesa.clinic.shared.*
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

interface ClinicApi {
    suspend fun validatePin(staffId: String, pin: String): LoginResponseDto
    suspend fun getPatients(): List<PatientDto>
    suspend fun getPatient(id: String): PatientDto?
    suspend fun getQueue(): List<QueueItemDto>
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
    suspend fun resolveConflict(request: ConflictResolutionRequestDto): ConflictResolutionResultDto
    suspend fun getSyncHealth(): Map<String, String>
}

class KtorClinicApi(private val client: HttpClient, private val baseUrl: String) : ClinicApi {
    override suspend fun validatePin(staffId: String, pin: String): LoginResponseDto =
        client.post("$baseUrl/auth/login") { contentType(ContentType.Application.Json); setBody(LoginRequestDto(staffId, pin)) }.body()

    override suspend fun getPatients(): List<PatientDto> = client.get("$baseUrl/patients").body()
    override suspend fun getPatient(id: String): PatientDto? = client.get("$baseUrl/patients/$id").body()
    override suspend fun getQueue(): List<QueueItemDto> = client.get("$baseUrl/queue").body()
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

    override suspend fun resolveConflict(request: ConflictResolutionRequestDto): ConflictResolutionResultDto =
        client.post("$baseUrl/sync/resolve-conflict") { contentType(ContentType.Application.Json); setBody(request) }.body()

    override suspend fun getSyncHealth(): Map<String, String> = client.get("$baseUrl/sync/health").body()
}

@Serializable data class LoginRequestDto(@SerialName("staff_id") val staffId: String, val pin: String)
@Serializable data class LoginResponseDto(val authenticated: Boolean)
@Serializable data class PatientDto(
    val id: String, val fullName: String, val age: Int, val sex: String, val status: String,
    val assignedWard: String? = null, val roomBed: String? = null, val acuity: String = "Moderate",
    val isolation: String? = null, val visits: Int = 0, val activeDiagnosis: String = "",
    val currentMedications: List<String> = emptyList(), val timeline: List<TimelineEvent> = emptyList()
)
@Serializable data class QueueItemDto(val patientId: String, val name: String, val triageLevel: Int, val waitMinutes: Int)
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

internal fun PatientDto.toDomain() = Patient(id, fullName, age, sex, status, assignedWard, roomBed, acuity, isolation, visits, activeDiagnosis, currentMedications, timeline)
internal fun Patient.toDto() = PatientDto(id, fullName, age, sex, status, assignedWard, roomBed, acuity, isolation, visits, activeDiagnosis, currentMedications, timeline)
internal fun QueueItemDto.toDomain() = QueueItem(patientId, name, triageLevel, waitMinutes)
internal fun DashboardMetricDto.toDomain() = DashboardMetric(title, value, subtitle)
internal fun BottleneckCellDto.toDomain() = BottleneckCell(workflowStage, severity, pendingCount)
internal fun TrendPointDto.toDomain() = TrendPoint(label, value)
internal fun WardOverviewDto.toDomain() = WardOverview(occupancyPercent, bedsAvailable, nurseWorkload, alerts)
internal fun BedCardDto.toDomain() = BedCard(ward, roomBed, patientName, status, acuity, isolation)
internal fun NursingTaskDto.toDomain() = NursingTask(type, detail, due, priority)
internal fun WardCensusRowDto.toDomain() = WardCensusRow(ward, occupiedBeds, totalBeds, highAcuityCount, isolationCount)
internal fun AdmissionTransferDischargeStateDto.toDomain() = AdmissionTransferDischargeState(selectedPatientId, selectedBed, transferWard, dischargeChecklist.map { it.label to it.complete })
internal fun SyncPatientDataDto.toDomain() = SyncPatientDataResponse(patients.map { it.toDomain() }, remoteVersion, count)
internal fun SyncResultItemDto.toDomain() = SyncResultItem(id, status, version)
internal fun ConflictResolutionResultDto.toDomain() = ConflictResolutionResult(resolved, strategy, finalVersion)
