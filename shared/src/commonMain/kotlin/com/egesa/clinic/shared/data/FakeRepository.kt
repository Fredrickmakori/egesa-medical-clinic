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
    var useMockFallback: Boolean = true

    private var clinicApi: ClinicApi? = null

    // Set after successful login; used by HttpClient DefaultRequest to attach Authorization header.
    var accessToken: String? = null
        private set

    /**
     * Source of truth for non-production builds is a seeded fixture so login works offline/demo-first.
     * When backend integration is ready, replace with GET /staff and keep this as fallback data.
     */
    suspend fun getMockStaff(): List<StaffMember> = apiOrFallback({
        // In the future, this could be: it.getMockStaff()
        STAFF_MEMBERS
    }) {
        delay(300)
        STAFF_MEMBERS
    }

    fun installClinicApi(api: ClinicApi) {
        clinicApi = api
    }

    // ── Staff ──────────────────────────────────────────────────────────────────

    suspend fun login(staffId: String, pin: String): Result<LoginResponseDto> = runResultOrFallback(
        apiCall = {
            val response = it.login(staffId, pin)
            accessToken = response.accessToken
            response
        },
        fallback = {
            if (pin.length < 4) throw IllegalArgumentException("Invalid PIN")
            // Demo-only token. In production, useMockFallback must be false.
            val response = LoginResponseDto(
                accessToken = "demo-token",
                expiresAtEpochSeconds = (Clock.System.now().epochSeconds + 8 * 60 * 60),
                role = UserRole.ADMIN.name,
                staffName = "Demo User"
            )
            accessToken = response.accessToken
            response
        }
    )

    suspend fun validatePin(staffId: String, pin: String): Boolean {
        return login(staffId, pin).isSuccess
    }

    suspend fun getPatients(): List<Patient> = apiOrFallback({
        it.getPatients().map { it.toDomain() }
    }) { db.allPatients() }

    suspend fun getPatient(id: String): Patient? = apiOrFallback({
        it.getPatient(id)?.toDomain()
    }) { db.allPatients().find { it.id == id } }

    suspend fun getQueue(): List<QueueItem> = apiOrFallback({
        it.getQueue().map { it.toDomain() }
    }) { db.receptionQueue() }

    suspend fun getKpis(): List<DashboardMetric> = apiOrFallback({
        it.getKpis().map { it.toDomain() }
    }) { db.adminKpis() }

    suspend fun getBottlenecks(): List<BottleneckCell> = apiOrFallback({
        it.getBottlenecks().map { it.toDomain() }
    }) { db.bottleneckHeatmap() }

    suspend fun getRegistrationTrend(): List<TrendPoint> = apiOrFallback({
        it.getRegistrationTrend().map { it.toDomain() }
    }) { db.registrationTrend() }

    suspend fun getWardOverview(): WardOverview = apiOrFallback({
        it.getWardOverview().toDomain()
    }) { db.wardOverview() }

    suspend fun getBedBoard(): List<BedCard> = apiOrFallback({
        it.getBedBoard().map { it.toDomain() }
    }) { db.bedBoard() }

    suspend fun getNursingTasks(): List<NursingTask> = apiOrFallback({
        it.getNursingTasks().map { it.toDomain() }
    }) { db.nursingTasks() }

    suspend fun getWardCensus(): List<WardCensusRow> = apiOrFallback({
        it.getWardCensus().map { it.toDomain() }
    }) { db.wardCensus() }

    suspend fun getAtdState(): AdmissionTransferDischargeState = apiOrFallback({
        it.getAtdState().toDomain()
    }) { db.atdState() }

    suspend fun getShiftHandoff(shift: Shift): List<String> = apiOrFallback({
        it.getShiftHandoff(shift)
    }) { db.shiftHandoffSummary(shift) }

    suspend fun syncPatientData(remoteVersion: Long = 0): Result<SyncPatientDataResponse> = runResultOrFallback({
        it.syncPatientData(remoteVersion).toDomain()
    }) {
        val patients = db.allPatients()
        SyncPatientDataResponse(patients = patients, remoteVersion = remoteVersion, count = patients.size)
    }

    suspend fun uploadPatientChanges(changes: List<Patient>): Result<List<SyncResultItem>> = runResultOrFallback({
        it.uploadPatientChanges(changes.map { it.toDto() }).map { it.toDomain() }
    }) {
        changes.map { SyncResultItem(id = it.id, status = "synced", version = 1) }
    }

    suspend fun resolveConflict(
        entityId: String,
        localVersion: Int,
        remoteVersion: Int,
        strategy: String = "SERVER_WINS"
    ): Result<ConflictResolutionResult> = runResultOrFallback({
        it.resolveConflict(ConflictResolutionRequestDto(entityId, localVersion, remoteVersion, strategy)).toDomain()
    }) {
        ConflictResolutionResult(resolved = true, strategy = strategy, finalVersion = maxOf(localVersion, remoteVersion))
    }

    private suspend inline fun <T> runResultOrFallback(
        apiCall: suspend (ClinicApi) -> T,
        fallback: suspend () -> T
    ): Result<T> {
        val api = clinicApi
        return try {
            if (useMockFallback || api == null) {
                Result.success(fallback())
            } else {
                Result.success(apiCall(api))
            }
        } catch (e: Exception) {
            Result.failure(e.toRepositoryException())
        }
    }

    private suspend inline fun <T> apiOrFallback(
        apiCall: suspend (ClinicApi) -> T,
        fallback: suspend () -> T
    ): T {
        val api = clinicApi
        return if (useMockFallback || api == null) {
            fallback()
        } else {
            apiCall(api)
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

    @Suppress("UNUSED_PARAMETER")
    private suspend fun Throwable.toRepositoryException(): Throwable = this
}

class RepositoryHttpException(statusCode: Int, responseBody: String) : Exception(
    "HTTP $statusCode: ${responseBody.ifBlank { "No response body" }}"
)

val STAFF_MEMBERS = listOf(
    StaffMember("DOC-001", "Dr. Aisha Nambala", UserRole.DOCTOR, "General Medicine"),
    StaffMember("DOC-002", "Dr. Michael Odongo", UserRole.DOCTOR, "Pediatrics"),
    StaffMember("NRS-001", "Nurse Grace Atieno", UserRole.NURSE, "Maternity Ward"),
    StaffMember("NRS-002", "Nurse Peter Wekesa", UserRole.NURSE, "Emergency Department"),
    StaffMember("RCP-001", "Sarah Namutebi", UserRole.RECEPTIONIST, "Front Desk"),
    StaffMember("ADM-001", "Joseph Kato", UserRole.ADMIN, "Administration")
)
