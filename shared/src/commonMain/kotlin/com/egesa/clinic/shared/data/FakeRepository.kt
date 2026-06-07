package com.egesa.clinic.shared.data

import com.egesa.clinic.shared.*
import com.egesa.clinic.shared.sync.SyncHealthStatus
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock

object FakeRepository {

    private val db = HospitalState()
    private var clinicApi: ClinicApi? = null

    /**
     * Enable local mock fallback for development and offline testing.
     */
    var useMockFallback: Boolean = false

    /**
     * Source of truth for non-production builds is a seeded fixture so login works offline/demo-first.
     * When backend integration is ready, replace with GET /staff and keep this as fallback data.
     */
    suspend fun getStaff(): List<StaffMember> {
        delay(300)
        return STAFF_MEMBERS
    }

    fun installClinicApi(api: ClinicApi) {
        clinicApi = api
    }

    private fun requireApi(): ClinicApi = clinicApi ?: error("ClinicApi is not configured. Call FakeRepository.installClinicApi(...) during app startup.")

    // ── Staff ──────────────────────────────────────────────────────────────────

    suspend fun validatePin(staffId: String, pin: String): Boolean = apiOrFallback({
        requireApi().validatePin(staffId, pin).authenticated
    }) {
        pin.length >= 4
    }

    suspend fun getPatients(): List<Patient> = apiOrFallback({
        requireApi().getPatients().map { it.toDomain() }
    }) { db.allPatients() }

    suspend fun getPatient(id: String): Patient? = apiOrFallback({
        requireApi().getPatient(id)?.toDomain()
    }) { db.allPatients().find { it.id == id } }

    suspend fun getQueue(): List<QueueItem> = apiOrFallback({
        requireApi().getQueue().map { it.toDomain() }
    }) { db.receptionQueue() }

    suspend fun getKpis(): List<DashboardMetric> = apiOrFallback({
        requireApi().getKpis().map { it.toDomain() }
    }) { db.adminKpis() }

    suspend fun getBottlenecks(): List<BottleneckCell> = apiOrFallback({
        requireApi().getBottlenecks().map { it.toDomain() }
    }) { db.bottleneckHeatmap() }

    suspend fun getRegistrationTrend(): List<TrendPoint> = apiOrFallback({
        requireApi().getRegistrationTrend().map { it.toDomain() }
    }) { db.registrationTrend() }

    suspend fun getWardOverview(): WardOverview = apiOrFallback({
        requireApi().getWardOverview().toDomain()
    }) { db.wardOverview() }

    suspend fun getBedBoard(): List<BedCard> = apiOrFallback({
        requireApi().getBedBoard().map { it.toDomain() }
    }) { db.bedBoard() }

    suspend fun getNursingTasks(): List<NursingTask> = apiOrFallback({
        requireApi().getNursingTasks().map { it.toDomain() }
    }) { db.nursingTasks() }

    suspend fun getWardCensus(): List<WardCensusRow> = apiOrFallback({
        requireApi().getWardCensus().map { it.toDomain() }
    }) { db.wardCensus() }

    suspend fun getAtdState(): AdmissionTransferDischargeState = apiOrFallback({
        requireApi().getAtdState().toDomain()
    }) { db.atdState() }

    suspend fun getShiftHandoff(shift: Shift): List<String> = apiOrFallback({
        requireApi().getShiftHandoff(shift)
    }) { db.shiftHandoffSummary(shift) }

    suspend fun syncPatientData(remoteVersion: Long = 0): Result<SyncPatientDataResponse> = resultOrFallback({
        requireApi().syncPatientData(remoteVersion).toDomain()
    }) {
        val patients = db.allPatients()
        SyncPatientDataResponse(patients = patients, remoteVersion = remoteVersion, count = patients.size)
    }

    suspend fun uploadPatientChanges(changes: List<Patient>): Result<List<SyncResultItem>> = resultOrFallback({
        requireApi().uploadPatientChanges(changes.map { it.toDto() }).map { it.toDomain() }
    }) {
        changes.map { SyncResultItem(id = it.id, status = "synced", version = 1) }
    }

    suspend fun resolveConflict(
        entityId: String,
        localVersion: Int,
        remoteVersion: Int,
        strategy: String = "SERVER_WINS"
    ): Result<ConflictResolutionResult> = resultOrFallback({
        requireApi().resolveConflict(ConflictResolutionRequestDto(entityId, localVersion, remoteVersion, strategy)).toDomain()
    }) {
        ConflictResolutionResult(resolved = true, strategy = strategy, finalVersion = maxOf(localVersion, remoteVersion))
    }

    private suspend fun <T> apiOrFallback(apiCall: suspend () -> T, fallback: suspend () -> T): T {
        return if (useMockFallback) {
            fallback()
        } else {
            apiCall()
        }
    }

    private suspend fun <T> resultOrFallback(apiCall: suspend () -> T, fallback: suspend () -> T): Result<T> {
        return try {
            Result.success(if (useMockFallback) fallback() else apiCall())
        } catch (e: Throwable) {
            if (useMockFallback) Result.success(fallback()) else Result.failure(e)
        }
    }

    /**
     * Get sync health status from server
     */
    suspend fun getSyncHealth(): Result<SyncHealthStatus> {
        return try {
            delay(200)
            // Backend route is explicitly defined under payments in the current API spec.
            // TODO: GET /payments/sync-health
            Result.success(SyncHealthStatus(
                status = "healthy",
                pendingCount = 0,
                lastSyncTime = Clock.System.now().toEpochMilliseconds()
            ))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

}

class RepositoryHttpException(val statusCode: Int, val responseBody: String) : Exception(
    "HTTP $statusCode: ${responseBody.ifBlank { "No response body" }}"
)

data class SyncPatientDataResponse(
    val patients: List<Patient>,
    val remoteVersion: Long,
    val count: Int
)

data class SyncResultItem(
    val id: String,
    val status: String,
    val version: Int
)

data class ConflictResolutionResult(
    val resolved: Boolean,
    val strategy: String,
    val finalVersion: Int
)

val STAFF_MEMBERS = listOf(
    StaffMember("DOC-001", "Dr. Aisha Nambala", UserRole.DOCTOR, "General Medicine"),
    StaffMember("DOC-002", "Dr. Michael Odongo", UserRole.DOCTOR, "Pediatrics"),
    StaffMember("NRS-001", "Nurse Grace Atieno", UserRole.NURSE, "Maternity Ward"),
    StaffMember("NRS-002", "Nurse Peter Wekesa", UserRole.NURSE, "Emergency Department"),
    StaffMember("RCP-001", "Sarah Namutebi", UserRole.RECEPTIONIST, "Front Desk"),
    StaffMember("ADM-001", "Joseph Kato", UserRole.ADMIN, "Administration")
)
