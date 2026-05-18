package com.egesa.clinic.shared.data

import com.egesa.clinic.shared.*
import com.egesa.clinic.shared.sync.SyncHealthStatus
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock

object FakeRepository {

    private val db = HospitalState()

    /**
     * Enable local mock fallback for development and offline testing.
     */
    var useMockFallback: Boolean = false

    private var clinicApi: ClinicApi? = null

    fun installClinicApi(api: ClinicApi) {
        clinicApi = api
    }

    private fun requireApi(): ClinicApi = clinicApi ?: error("ClinicApi is not configured. Call FakeRepository.installClinicApi(...) during app startup.")

    // ── Staff ──────────────────────────────────────────────────────────────────

    suspend fun getStaff(): List<StaffMember> = STAFF_MEMBERS

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

    suspend fun syncPatientData(remoteVersion: Long = 0): Result<SyncPatientDataResponse> = runResultOrFallback({
        requireApi().syncPatientData(remoteVersion).toDomain()
    }) {
        val patients = db.allPatients()
        SyncPatientDataResponse(patients = patients, remoteVersion = remoteVersion, count = patients.size)
    }

    suspend fun uploadPatientChanges(changes: List<Patient>): Result<List<SyncResultItem>> = runResultOrFallback({
        requireApi().uploadPatientChanges(changes.map { it.toDto() }).map { it.toDomain() }
    }) {
        changes.map { SyncResultItem(id = it.id, status = "synced", version = 1) }
    }

    suspend fun resolveConflict(
        entityId: String,
        localVersion: Int,
        remoteVersion: Int,
        strategy: String = "SERVER_WINS"
    ): Result<ConflictResolutionResult> = runResultOrFallback({
        requireApi().resolveConflict(ConflictResolutionRequestDto(entityId, localVersion, remoteVersion, strategy)).toDomain()
    }) {
        ConflictResolutionResult(resolved = true, strategy = strategy, finalVersion = maxOf(localVersion, remoteVersion))
    }

    suspend fun getSyncHealth(): Result<Map<String, String>> = runResultOrFallback({
        requireApi().getSyncHealth()
    }) { mapOf("status" to "unknown") }

    private suspend fun <T> apiOrFallback(apiCall: suspend () -> T, fallback: suspend () -> T): T {
        return if (useMockFallback) {
            fallback()
        } else {
            apiCall()
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

    private suspend fun Throwable.toRepositoryException(): Throwable = when (this) {
        is ClientRequestException -> RepositoryHttpException(response.status.value, response.bodyAsText())
        is ServerResponseException -> RepositoryHttpException(response.status.value, response.bodyAsText())
        else -> this
    }
}

class RepositoryHttpException(val statusCode: Int, val responseBody: String) : Exception(
    "HTTP $statusCode: ${responseBody.ifBlank { "No response body" }}"
)

val STAFF_MEMBERS = emptyList<StaffMember>()
